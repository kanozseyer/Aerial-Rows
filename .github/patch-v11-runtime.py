from pathlib import Path

# v11 fixes first-boot determinism. Red Light channels are materialized from the
# bundled exported layout before Projectivy builds its Home/Edit-Channels model,
# and appearance values are replayed through Projectivy's own live preference
# objects on first launcher resume.

engine = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightEngine.java')
text = engine.read_text()

# Adopt synchronously preseeded channels instead of creating duplicates once the
# real Red Light route scan identifies the semantic channel.
create_anchor = '''            ContentValues v = new ContentValues();\n            v.put("type","TYPE_PREVIEW");'''
adopt = '''            long preseeded = findRedLightChannelByTitle(c, st.title);\n            if (preseeded > 0) {\n                st.channelId = preseeded;\n                ContentValues up = new ContentValues();\n                up.put("display_name", st.title);\n                up.put("internal_provider_id", st.key);\n                up.put("app_link_intent_uri", projectivyForwardIntent(c, st.uri).toUri(0));\n                c.getContentResolver().update(\n                        ContentUris.withAppendedId(TvContract.Channels.CONTENT_URI, st.channelId),\n                        up, null, null);\n                return st;\n            }\n\n            ContentValues v = new ContentValues();\n            v.put("type","TYPE_PREVIEW");'''
if text.count(create_anchor) != 1:
    raise SystemExit(f'ensureChannel creation anchor mismatch: {text.count(create_anchor)}')
text = text.replace(create_anchor, adopt, 1)

helper_anchor = '''    private static boolean isKodiInstalled(Context c) {'''
helpers = r'''    static void preseedBundledChannels(Context c) {
        if (c == null) return;
        try {
            JSONObject profile;
            try (InputStream in = c.getAssets().open("launcher_default_profile.json")) {
                profile = new JSONObject(EmbeddedRuntime.readAll(in, 1024 * 1024));
            }
            String raw = profile.optString("action_categories_list", "");
            if (raw.isEmpty()) return;
            JSONArray categories = new JSONArray(raw);
            String prefix = c.getPackageName() + "Red Light · ";
            int ready = 0;

            for (int i = 0; i < categories.length(); i++) {
                JSONObject o = categories.optJSONObject(i);
                if (o == null || o.optInt("type", -1) != 6) continue;
                String uid = o.optString("channelUniqueId", "");
                if (!uid.startsWith(prefix)) continue;
                String title = o.optString("title", "").trim();
                if (title.isEmpty()) continue;

                long id = findRedLightChannelByTitle(c, title);
                if (id <= 0) {
                    ContentValues v = new ContentValues();
                    v.put("type", "TYPE_PREVIEW");
                    v.put("display_name", title);
                    v.put("internal_provider_id", "redlight:preseed:" + shortHash(title));
                    Uri inserted = c.getContentResolver().insert(TvContract.Channels.CONTENT_URI, v);
                    if (inserted != null) {
                        id = ContentUris.parseId(inserted);
                        try { TvContract.requestChannelBrowsable(c, id); } catch (Throwable ignored) {}
                        applyRedLightChannelLogo(c, id);
                    }
                }
                if (id > 0) ready++;
            }

            if (ready > 0) {
                // All exported Red Light category identities now point at real
                // TvProvider channels, so Projectivy can safely build Home from the
                // exported list on its very first model construction.
                EmbeddedRuntime.projectivyPrefs(c).edit()
                        .putString("action_categories_list", categories.toString())
                        .putBoolean(CHANNEL_LAYOUT_MARKER, true)
                        .remove(CHANNEL_LAYOUT_ATTEMPTS)
                        .apply();
                try { c.getContentResolver().notifyChange(TvContract.Channels.CONTENT_URI, null); }
                catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private static long findRedLightChannelByTitle(Context c, String title) {
        if (title == null || title.isEmpty()) return 0L;
        try (Cursor cur = c.getContentResolver().query(
                TvContract.Channels.CONTENT_URI,
                new String[]{"_id", "display_name", "internal_provider_id"},
                "display_name=?", new String[]{title}, null)) {
            if (cur == null) return 0L;
            int idCol = cur.getColumnIndex("_id");
            int providerCol = cur.getColumnIndex("internal_provider_id");
            while (cur.moveToNext()) {
                String provider = providerCol >= 0 && !cur.isNull(providerCol)
                        ? cur.getString(providerCol) : "";
                if (provider.startsWith("redlight:") && idCol >= 0) return cur.getLong(idCol);
            }
        } catch (Throwable ignored) {}
        return 0L;
    }

'''
if text.count(helper_anchor) != 1:
    raise SystemExit(f'preseed helper anchor mismatch: {text.count(helper_anchor)}')
text = text.replace(helper_anchor, helpers + helper_anchor, 1)
engine.write_text(text)
print('added synchronous bundled Red Light channel preseed/adoption')

embedded = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java')
text = embedded.read_text()

const_anchor = '    private static final String PROFILE_MARKER = "launcher_embedded_profile_v2";\n'
const_repl = const_anchor + '    private static final String LIVE_APPEARANCE_MARKER = "launcher_embedded_live_appearance_v1";\n'
if text.count(const_anchor) != 1:
    raise SystemExit('live appearance constant anchor mismatch')
text = text.replace(const_anchor, const_repl, 1)

# BootstrapProvider is created before Projectivy builds the settings/Home channel
# model. Preseed the exported Red Light channels at this point.
bootstrap_old = 'if (c != null) { applyDefaultProfile(c); repairEmbeddedAerialPackage(c); scheduleRedLight(c); syncRedLightAsync(c); }'
bootstrap_new = 'if (c != null) { RedLightEngine.preseedBundledChannels(c); applyDefaultProfile(c); repairEmbeddedAerialPackage(c); scheduleRedLight(c); syncRedLightAsync(c); }'
if text.count(bootstrap_old) != 1:
    raise SystemExit(f'bootstrap preseed anchor mismatch: {text.count(bootstrap_old)}')
text = text.replace(bootstrap_old, bootstrap_new, 1)

# Replay appearance values through Projectivy's live yh3 preference objects once,
# after MainActivity and hj3 have initialized. This makes card tint/background and
# other exported appearance settings affect the live UI, not only SharedPreferences.
handler_anchor = '        Handler h = new Handler(Looper.getMainLooper());\n'
handler_repl = '''        Handler h = new Handler(Looper.getMainLooper());\n        if (!projectivyPrefs(app).getBoolean(LIVE_APPEARANCE_MARKER, false)) {\n            applyBundledAppearanceLive(app);\n            h.postDelayed(() -> {\n                if (applyBundledAppearanceLive(app)) {\n                    projectivyPrefs(app).edit().putBoolean(LIVE_APPEARANCE_MARKER, true).apply();\n                }\n            }, 900L);\n        }\n'''
if text.count(handler_anchor) != 1:
    raise SystemExit(f'live appearance handler anchor mismatch: {text.count(handler_anchor)}')
text = text.replace(handler_anchor, handler_repl, 1)

method_anchor = '''    static void repairEmbeddedAerialPackage(Context c) {'''
live_method = r'''    static boolean applyBundledAppearanceLive(Context c) {
        try {
            JSONObject root;
            try (InputStream in = c.getAssets().open("launcher_default_profile.json")) {
                root = new JSONObject(readAll(in, 1024 * 1024));
            }

            Class<?> itemClass = Class.forName("yh3");
            java.lang.reflect.Field registryField = itemClass.getField("n");
            java.lang.reflect.Field keyField = itemClass.getField("a");
            java.lang.reflect.Field defaultField = itemClass.getField("b");
            java.lang.reflect.Method setter = itemClass.getMethod("h", Object.class);
            Object registryObject = registryField.get(null);
            if (!(registryObject instanceof java.util.List)) return false;

            int applied = 0;
            for (Object item : (java.util.List<?>) registryObject) {
                if (item == null) continue;
                Object keyObject = keyField.get(item);
                if (keyObject == null) continue;
                String key = String.valueOf(keyObject);
                if (!key.startsWith("appearance_") || !root.has(key)) continue;
                Object raw = root.opt(key);
                if (raw == null || raw == JSONObject.NULL) continue;
                Object def = defaultField.get(item);
                Object value = coerceProfileValue(raw, def);
                setter.invoke(item, value);
                applied++;
            }
            return applied > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object coerceProfileValue(Object raw, Object def) {
        if (def instanceof Integer && raw instanceof Number) return ((Number) raw).intValue();
        if (def instanceof Long && raw instanceof Number) return ((Number) raw).longValue();
        if (def instanceof Float && raw instanceof Number) return ((Number) raw).floatValue();
        if (def instanceof Double && raw instanceof Number) return ((Number) raw).doubleValue();
        if (def instanceof Boolean) {
            if (raw instanceof Boolean) return raw;
            return Boolean.valueOf(String.valueOf(raw));
        }
        if (def instanceof String) return String.valueOf(raw);
        return raw;
    }

'''
if text.count(method_anchor) != 1:
    raise SystemExit(f'live appearance method anchor mismatch: {text.count(method_anchor)}')
text = text.replace(method_anchor, live_method + method_anchor, 1)
embedded.write_text(text)
print('added first-launch live appearance replay and bootstrap preseed')
