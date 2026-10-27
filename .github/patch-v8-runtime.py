from pathlib import Path

# v8 fixes the actual Home-page restore path.  Projectivy keeps the category model
# in memory (w40.E), so writing action_categories_list alone is not enough once the
# launcher has started.  Also, v7 required every exported visible Red Light row to
# exist before restoring anything, allowing one slow/missing shelf to block the
# entire Home layout.

embedded = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java')
text = embedded.read_text()
anchor = '''                String key = keys.next();\n                Object v = root.opt(key);'''
replacement = '''                String key = keys.next();\n                // Channel/category layout is restored only after TvProvider rows exist.\n                // Loading it here lets Projectivy validate it too early and discard rows.\n                if ("action_categories_list".equals(key)) continue;\n                Object v = root.opt(key);'''
if text.count(anchor) != 1:
    raise SystemExit(f'EmbeddedRuntime deferred-category anchor mismatch: {text.count(anchor)}')
embedded.write_text(text.replace(anchor, replacement, 1))
print('deferred action_categories_list until channels exist')

engine = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightEngine.java')
text = engine.read_text()

# The v7 patch has already inserted restoreBundledChannelLayoutOnce immediately
# before notifyProjectivyChannelsChanged. Replace that whole region with the v8
# incremental restore + explicit Projectivy model reload.
start = text.find('    private static final String CHANNEL_LAYOUT_MARKER = ')
end = text.find('    private static void notifyProjectivyChannelsChanged(Context c) {', start)
if start < 0 or end < 0:
    raise SystemExit('v7 channel-layout restore region not found')

v8 = r'''    private static final String CHANNEL_LAYOUT_MARKER = "launcher_embedded_channel_layout_v2";
    private static final String CHANNEL_LAYOUT_ATTEMPTS = "launcher_embedded_channel_layout_attempts_v2";

    private static void pruneObsoletePersonalListChannels(Context c, Map<String, ChannelState> states) {
        ArrayList<String> remove = new ArrayList<>();
        for (ChannelState st : states.values()) {
            if (st.key != null && st.key.startsWith("redlight:trakt:list:")) {
                deleteChannel(c, st.channelId);
                remove.add(st.key);
            }
        }
        for (String key : remove) states.remove(key);
    }

    private static boolean isNoisyPersonalCategory(String uid) {
        String n = uid == null ? "" : uid.toLowerCase(Locale.ROOT);
        return n.contains("red light · couchy-")
                || n.contains("red light · ishows-")
                || n.contains("red light · following [");
    }

    private static void cleanObsoletePersonalListCategories(Context c) {
        try {
            android.content.SharedPreferences p = EmbeddedRuntime.projectivyPrefs(c);
            String raw = p.getString("action_categories_list", "[]");
            JSONArray in = new JSONArray(raw);
            JSONArray out = new JSONArray();
            boolean changed = false;
            for (int i = 0; i < in.length(); i++) {
                JSONObject o = in.optJSONObject(i);
                if (o != null && isNoisyPersonalCategory(o.optString("channelUniqueId", ""))) {
                    changed = true;
                    continue;
                }
                out.put(in.get(i));
            }
            if (changed) p.edit().putString("action_categories_list", out.toString()).apply();
        } catch (Throwable ignored) {}
    }

    private static void restoreBundledChannelLayoutOnce(Context c, Map<String, ChannelState> states) {
        try {
            final android.content.SharedPreferences p = EmbeddedRuntime.projectivyPrefs(c);
            if (p.getBoolean(CHANNEL_LAYOUT_MARKER, false)) return;
            if (states == null || states.isEmpty()) return;

            HashSet<String> available = new HashSet<>();
            for (ChannelState st : states.values()) {
                if (st == null || st.channelId <= 0 || st.title == null) continue;
                if (channelExists(c, st.channelId)) available.add(c.getPackageName() + st.title);
            }

            JSONObject profile;
            try (InputStream in = c.getAssets().open("launcher_default_profile.json")) {
                profile = new JSONObject(EmbeddedRuntime.readAll(in, 1024 * 1024));
            }
            String wantedRaw = profile.optString("action_categories_list", "");
            if (wantedRaw.isEmpty()) return;
            JSONArray wanted = new JSONArray(wantedRaw);
            JSONArray materialized = new JSONArray();

            int visibleWanted = 0;
            int visibleReady = 0;
            String redPrefix = c.getPackageName() + "Red Light · ";

            for (int i = 0; i < wanted.length(); i++) {
                JSONObject o = wanted.optJSONObject(i);
                if (o == null) {
                    materialized.put(wanted.get(i));
                    continue;
                }
                String uid = o.optString("channelUniqueId", "");
                if (isNoisyPersonalCategory(uid)) continue;

                if (o.optInt("type", -1) == 6 && uid.startsWith(redPrefix)) {
                    boolean visible = !o.has("isVisible") || o.optBoolean("isVisible", true);
                    if (visible) visibleWanted++;

                    // Do not feed a not-yet-created channel to Projectivy. It removes
                    // invalid channel categories during model construction. A later
                    // Red Light scan will retry from the untouched bundled profile.
                    if (!available.contains(uid)) continue;
                    if (visible) visibleReady++;
                }
                materialized.put(o);
            }

            // Wait until discovery has produced at least one of the Home rows the
            // export asked to show. This prevents the bootstrap scan from restoring a
            // non-channel-only layout before Red Light has actually initialized.
            if (visibleWanted > 0 && visibleReady == 0) return;

            int attempts = p.getInt(CHANNEL_LAYOUT_ATTEMPTS, 0) + 1;
            boolean complete = visibleWanted == 0 || visibleReady >= visibleWanted;
            boolean finalize = complete || attempts >= 3;
            p.edit().putInt(CHANNEL_LAYOUT_ATTEMPTS, attempts).apply();

            final String layout = materialized.toString();
            android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
            h.post(() -> applyBundledChannelLayout(c, layout, false));
            h.postDelayed(() -> applyBundledChannelLayout(c, layout, finalize), 900L);
        } catch (Throwable ignored) {}
    }

    private static void applyBundledChannelLayout(Context c, String layout, boolean finalize) {
        android.content.SharedPreferences p = EmbeddedRuntime.projectivyPrefs(c);
        try {
            // Write immediately before resetting Projectivy's model so its next load
            // cannot reuse the already-created default category list.
            p.edit().putString("action_categories_list", layout).apply();

            Class<?> settings = Class.forName("dj3");
            java.lang.reflect.Method reset = settings.getDeclaredMethod("m");
            reset.setAccessible(true);
            reset.invoke(null);

            // This is Projectivy 4.72's own channel/category refresh path (the same
            // path used after Android PACKAGE_ADDED/PACKAGE_REMOVED changes).
            Class<?> categories = Class.forName("w40");
            java.lang.reflect.Method reload = categories.getDeclaredMethod("z", boolean.class);
            reload.setAccessible(true);
            reload.invoke(null, false);

            if (finalize) {
                p.edit()
                        .putBoolean(CHANNEL_LAYOUT_MARKER, true)
                        .remove(CHANNEL_LAYOUT_ATTEMPTS)
                        .apply();
            }
        } catch (Throwable ignored) {
            if (finalize) {
                // Leave the marker unset so the next Red Light sync can retry.
                p.edit().remove(CHANNEL_LAYOUT_MARKER).apply();
            }
        }
    }

'''
text = text[:start] + v8 + text[end:]
engine.write_text(text)
print('replaced v7 restore with incremental live Projectivy Home-model restore')

# v9: add the installed Red Light artwork as the native Android-TV channel logo.
exec(Path('.github/patch-v9-runtime.py').read_text(), globals())
