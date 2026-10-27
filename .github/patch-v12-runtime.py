from pathlib import Path

# v12 abandons v11's preseed/live-reflection approach.  Projectivy's own settings
# importer is reused after the *real* Red Light TvProvider channels exist.  Its own
# cache-reset/restart path then rebuilds Home/settings against valid channel IDs.

engine = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightEngine.java')
text = engine.read_text()
old = '''        saveStates(context, states);\n        restoreBundledChannelLayoutOnce(context, states);\n        notifyProjectivyChannelsChanged(context);'''
new = '''        saveStates(context, states);\n        EmbeddedRuntime.maybeRestoreBundledProfileNatively(context);\n        notifyProjectivyChannelsChanged(context);'''
if text.count(old) != 1:
    raise SystemExit(f'v12 post-sync restore anchor mismatch: {text.count(old)}')
engine.write_text(text.replace(old, new, 1))
print('routed post-sync restore through Projectivy native importer')

embedded = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java')
text = embedded.read_text()

anchor = '''    static void repairEmbeddedAerialPackage(Context c) {'''
method = r'''    private static final String NATIVE_RESTORE_PREFS = "embedded_profile_restore";
    private static final String NATIVE_RESTORE_MARKER = "native_profile_20261027_002828_v1";

    static void maybeRestoreBundledProfileNatively(Context c) {
        if (c == null) return;
        try {
            SharedPreferences state = c.getSharedPreferences(NATIVE_RESTORE_PREFS, Context.MODE_PRIVATE);
            if (state.getBoolean(NATIVE_RESTORE_MARKER, false)) return;

            JSONObject root;
            try (InputStream in = c.getAssets().open("launcher_default_profile.json")) {
                root = new JSONObject(readAll(in, 1024 * 1024));
            }

            // Projectivy validates type-6 categories against TvProvider while loading
            // Home. Wait until every Red Light row the export asked to SHOW actually
            // exists. Hidden rows are allowed to be discovered later.
            String raw = root.optString("action_categories_list", "");
            if (raw.isEmpty()) return;
            org.json.JSONArray categories = new org.json.JSONArray(raw);
            boolean hasVisibleRedLight = false;
            for (int i = 0; i < categories.length(); i++) {
                JSONObject o = categories.optJSONObject(i);
                if (o == null || o.optInt("type", -1) != 6) continue;
                String title = o.optString("title", "");
                if (!title.startsWith("Red Light · ")) continue;
                boolean visible = !o.has("isVisible") || o.optBoolean("isVisible", true);
                if (!visible) continue;
                hasVisibleRedLight = true;
                if (!tvChannelExistsByTitle(c, title)) return;
            }
            if (!hasVisibleRedLight) return;

            // This is Projectivy 4.72's own JSON settings import routine. It preserves
            // the exact exported primitive types instead of us maintaining a parallel
            // importer. The bundled JSON is the decrypted body of the user's .plbackup.
            Class<?> importer = Class.forName("dj3");
            java.lang.reflect.Method importPrefs = importer.getDeclaredMethod(
                    "h", SharedPreferences.class, String.class);
            importPrefs.setAccessible(true);
            importPrefs.invoke(null, projectivyPrefs(c), root.toString());

            // Keep our embedded-provider invariants and tell the v10 resume throttle
            // that channel-layout materialisation has completed.
            projectivyPrefs(c).edit()
                    .putString("appearance_wallpaper_provider", AERIAL_ID)
                    .putString("appearance_wallpaper_provider_packagename", c.getPackageName())
                    .putBoolean("launcher_embedded_channel_layout_v2", true)
                    .apply();

            // Marker lives outside Projectivy's exported preference file, so importing
            // another profile can never accidentally mark itself as already restored.
            if (!state.edit().putBoolean(NATIVE_RESTORE_MARKER, true).commit()) return;

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    // Native settings import finishes by calling yb3.v(context): it
                    // invokes dj3.m() (reset Home/category caches) then restarts the
                    // Projectivy HOME task. Reuse that exact path.
                    Class<?> launcher = Class.forName("yb3");
                    java.lang.reflect.Method restart = launcher.getDeclaredMethod("v", Context.class);
                    restart.setAccessible(true);
                    restart.invoke(null, c.getApplicationContext());
                } catch (Throwable ignored) {
                    // If obfuscation ever changes, leave data imported; a normal manual
                    // relaunch will still read the restored preferences.
                }
            }, 350L);
        } catch (Throwable ignored) {}
    }

    private static boolean tvChannelExistsByTitle(Context c, String title) {
        android.database.Cursor cur = null;
        try {
            cur = c.getContentResolver().query(
                    android.media.tv.TvContract.Channels.CONTENT_URI,
                    new String[]{"_id", "display_name", "internal_provider_id"},
                    "display_name=?", new String[]{title}, null);
            if (cur == null) return false;
            int providerCol = cur.getColumnIndex("internal_provider_id");
            while (cur.moveToNext()) {
                String provider = providerCol >= 0 && !cur.isNull(providerCol)
                        ? cur.getString(providerCol) : "";
                if (provider.startsWith("redlight:")) return true;
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (cur != null) try { cur.close(); } catch (Throwable ignored) {}
        }
    }

'''
if text.count(anchor) != 1:
    raise SystemExit(f'v12 EmbeddedRuntime method anchor mismatch: {text.count(anchor)}')
embedded.write_text(text.replace(anchor, method + anchor, 1))
print('added delayed native Projectivy full-profile import/restart')
