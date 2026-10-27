from pathlib import Path

# v13 replaces the blocking v12 restore with one shared tolerant profile engine:
# - non-channel preferences restore after MainActivity is alive (not in ContentProvider)
# - channel rows materialize progressively as their real TvProvider channels appear
# - the same engine accepts Projectivy .plbackup files for manual import
# - no channel is fabricated and SmartTube/other providers are never rewritten

embedded = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java')
text = embedded.read_text()

# Do not preload the full exported profile from BootstrapProvider/onResume. Projectivy
# initializes some of its own defaults/live models after providers are created, which
# was overwriting appearance values. Keep only safe bootstrap defaults there.
text = text.replace('        applyDefaultProfile(app);\n        repairEmbeddedAerialPackage(app);',
                    '        ensureBootstrapDefaults(app);\n        repairEmbeddedAerialPackage(app);', 1)
text = text.replace('if (c != null) { applyDefaultProfile(c); repairEmbeddedAerialPackage(c); scheduleRedLight(c); }',
                    'if (c != null) { ensureBootstrapDefaults(c); repairEmbeddedAerialPackage(c); scheduleRedLight(c); }', 1)

# Schedule the real bundled import after MainActivity has completed its first resume.
handler = '        Handler h = new Handler(Looper.getMainLooper());\n'
if text.count(handler) != 1:
    raise SystemExit(f'v13 handler anchor mismatch: {text.count(handler)}')
text = text.replace(handler, handler + '''        h.postDelayed(() -> maybeApplyBundledProfileAfterStart(context), 900L);\n        h.postDelayed(() -> reconcilePendingChannelLayout(app), 2600L);\n        h.postDelayed(() -> reconcilePendingChannelLayout(app), 6500L);\n''', 1)

anchor = '    static void repairEmbeddedAerialPackage(Context c) {\n'
if text.count(anchor) != 1:
    raise SystemExit(f'v13 method anchor mismatch: {text.count(anchor)}')

methods = r'''    private static final String RESTORE_STATE = "embedded_profile_restore_v13";
    private static final String AUTO_RESTORE_MARKER = "auto_profile_20261027_002828_v13";
    private static final String PENDING_LAYOUT = "pending_channel_layout";
    private static final String PENDING_LAYOUT_STARTED = "pending_channel_layout_started";
    private static final String LAST_MATERIALIZED_LAYOUT = "last_materialized_layout";
    private static final long PENDING_LAYOUT_WINDOW_MS = 60L * 1000L;

    static void ensureBootstrapDefaults(Context c) {
        try {
            SharedPreferences p = projectivyPrefs(c);
            SharedPreferences.Editor e = p.edit();
            if (!p.contains("general_onboarding_completed")) e.putBoolean("general_onboarding_completed", true);
            if (!p.contains("appearance_wallpaper_provider")) {
                e.putString("appearance_wallpaper_provider", AERIAL_ID);
                e.putString("appearance_wallpaper_provider_packagename", c.getPackageName());
            }
            if (!p.contains("appearance_wallpaper_provider_params_" + AERIAL_ID)) {
                e.putString("appearance_wallpaper_provider_params_" + AERIAL_ID,
                        "{\"sources\":[\"A\",\"C1\",\"C2\"],\"prefer4k\":true,\"prefer_hdr\":false,\"fallback\":true}");
            }
            e.apply();
        } catch (Throwable ignored) {}
    }

    static void maybeApplyBundledProfileAfterStart(Context context) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        try {
            SharedPreferences state = app.getSharedPreferences(RESTORE_STATE, Context.MODE_PRIVATE);
            if (state.getBoolean(AUTO_RESTORE_MARKER, false)) {
                reconcilePendingChannelLayout(app);
                return;
            }
            JSONObject root;
            try (InputStream in = app.getAssets().open("launcher_default_profile.json")) {
                root = new JSONObject(readAll(in, 1024 * 1024));
            }
            if (!applyProfileRoot(app, root, true)) return;
            if (!state.edit().putBoolean(AUTO_RESTORE_MARKER, true).commit()) return;
            new Handler(Looper.getMainLooper()).postDelayed(() -> restartProjectivy(context), 250L);
        } catch (Throwable t) {
            android.util.Log.e("ProjectivyProfile", "Bundled profile restore failed", t);
        }
    }

    /** Intercepts stock Projectivy settings imports with a tolerant compatible reader. */
    public static boolean handleManualImportActivity(android.app.Activity activity) {
        if (activity == null) return false;
        Intent intent = activity.getIntent();
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) return false;
        Uri uri = null;
        try { uri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM); } catch (Throwable ignored) {}
        if (uri == null) uri = intent.getData();
        if (uri == null) return false;

        try (InputStream in = activity.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("Unable to open backup");
            byte[] data = readBytes(in, 2 * 1024 * 1024);
            String json = decodeBackupJson(data);
            JSONObject root = new JSONObject(json);

            SharedPreferences state = activity.getSharedPreferences(RESTORE_STATE, Context.MODE_PRIVATE);
            // A manual import always wins over the bundled first-run profile.
            state.edit().putBoolean(AUTO_RESTORE_MARKER, true).apply();
            if (!applyProfileRoot(activity, root, true)) throw new IllegalStateException("Unable to apply backup");

            android.widget.Toast.makeText(activity, "Projectivy settings restored", android.widget.Toast.LENGTH_SHORT).show();
            new Handler(Looper.getMainLooper()).postDelayed(() -> restartProjectivy(activity), 250L);
            return true;
        } catch (Throwable t) {
            android.util.Log.e("ProjectivyImport", "Manual .plbackup import failed", t);
            android.widget.Toast.makeText(activity, "Unable to restore Projectivy backup", android.widget.Toast.LENGTH_LONG).show();
            return true;
        }
    }

    private static boolean applyProfileRoot(Context c, JSONObject root, boolean resetModels) {
        try {
            SharedPreferences prefs = projectivyPrefs(c);
            SharedPreferences.Editor e = prefs.edit();
            String wantedLayout = root.optString("action_categories_list", "");

            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if ("action_categories_list".equals(key) || key.startsWith("launcher_embedded_")) continue;
                Object v = root.opt(key);
                if (v == null || v == JSONObject.NULL) continue;
                if (v instanceof Boolean) e.putBoolean(key, (Boolean) v);
                else if (v instanceof Integer) e.putInt(key, (Integer) v);
                else if (v instanceof Long) e.putLong(key, (Long) v);
                else if (v instanceof Double) e.putFloat(key, ((Double) v).floatValue());
                else if (v instanceof org.json.JSONArray) {
                    java.util.HashSet<String> set = new java.util.HashSet<>();
                    org.json.JSONArray a = (org.json.JSONArray) v;
                    for (int i = 0; i < a.length(); i++) if (!a.isNull(i)) set.add(String.valueOf(a.get(i)));
                    e.putStringSet(key, set);
                } else e.putString(key, String.valueOf(v));
            }

            e.putBoolean(PROFILE_MARKER, true);
            if (!e.commit()) return false;
            repairEmbeddedAerialPackage(c);

            if (!wantedLayout.isEmpty()) {
                SharedPreferences state = c.getSharedPreferences(RESTORE_STATE, Context.MODE_PRIVATE);
                state.edit()
                        .putString(PENDING_LAYOUT, wantedLayout)
                        .putLong(PENDING_LAYOUT_STARTED, System.currentTimeMillis())
                        .remove(LAST_MATERIALIZED_LAYOUT)
                        .apply();
                reconcilePendingChannelLayout(c);
            }
            if (resetModels) resetProjectivyModels();
            return true;
        } catch (Throwable t) {
            android.util.Log.e("ProjectivyProfile", "Profile application failed", t);
            return false;
        }
    }

    static void reconcilePendingChannelLayout(Context c) {
        if (c == null) return;
        try {
            SharedPreferences state = c.getSharedPreferences(RESTORE_STATE, Context.MODE_PRIVATE);
            String desiredRaw = state.getString(PENDING_LAYOUT, "");
            if (desiredRaw == null || desiredRaw.isEmpty()) return;

            org.json.JSONArray desired = new org.json.JSONArray(desiredRaw);
            java.util.Set<String> available = availableChannelUniqueIds(c);
            org.json.JSONArray materialized = new org.json.JSONArray();
            int visibleWanted = 0;
            int visibleReady = 0;

            for (int i = 0; i < desired.length(); i++) {
                JSONObject o = desired.optJSONObject(i);
                if (o == null) { materialized.put(desired.get(i)); continue; }
                if (o.optInt("type", -1) != 6) { materialized.put(o); continue; }

                String uid = o.optString("channelUniqueId", "");
                boolean visible = !o.has("isVisible") || o.optBoolean("isVisible", true);
                if (visible) visibleWanted++;
                if (uid.isEmpty() || available.contains(uid)) {
                    materialized.put(o);
                    if (visible) visibleReady++;
                }
            }

            String next = materialized.toString();
            String last = state.getString(LAST_MATERIALIZED_LAYOUT, "");
            if (!next.equals(last)) {
                projectivyPrefs(c).edit().putString("action_categories_list", next).commit();
                state.edit().putString(LAST_MATERIALIZED_LAYOUT, next).apply();
                resetProjectivyModels();
                try { c.getContentResolver().notifyChange(android.media.tv.TvContract.Channels.CONTENT_URI, null); } catch (Throwable ignored) {}
            }

            long started = state.getLong(PENDING_LAYOUT_STARTED, System.currentTimeMillis());
            boolean complete = visibleWanted == 0 || visibleReady >= visibleWanted;
            boolean expired = System.currentTimeMillis() - started >= PENDING_LAYOUT_WINDOW_MS;
            if (complete || expired) {
                state.edit().remove(PENDING_LAYOUT).remove(PENDING_LAYOUT_STARTED).remove(LAST_MATERIALIZED_LAYOUT).apply();
            }
        } catch (Throwable t) {
            android.util.Log.e("ProjectivyProfile", "Pending channel reconciliation failed", t);
        }
    }

    private static java.util.Set<String> availableChannelUniqueIds(Context c) {
        java.util.HashSet<String> out = new java.util.HashSet<>();
        android.database.Cursor cur = null;
        try {
            cur = c.getContentResolver().query(
                    android.media.tv.TvContract.Channels.CONTENT_URI,
                    new String[]{"package_name", "display_name"}, null, null, null);
            if (cur == null) return out;
            int pkgCol = cur.getColumnIndex("package_name");
            int nameCol = cur.getColumnIndex("display_name");
            while (cur.moveToNext()) {
                String pkg = pkgCol >= 0 && !cur.isNull(pkgCol) ? cur.getString(pkgCol) : "";
                String name = nameCol >= 0 && !cur.isNull(nameCol) ? cur.getString(nameCol) : "";
                if (!pkg.isEmpty() && !name.isEmpty()) out.add(pkg + name);
            }
        } catch (Throwable ignored) {
        } finally {
            if (cur != null) try { cur.close(); } catch (Throwable ignored) {}
        }
        return out;
    }

    private static void resetProjectivyModels() {
        try {
            Class<?> settings = Class.forName("dj3");
            java.lang.reflect.Method reset = settings.getDeclaredMethod("m");
            reset.setAccessible(true);
            reset.invoke(null);
        } catch (Throwable ignored) {}
        try {
            Class<?> categories = Class.forName("w40");
            java.lang.reflect.Method reload = categories.getDeclaredMethod("z", boolean.class);
            reload.setAccessible(true);
            reload.invoke(null, false);
        } catch (Throwable ignored) {}
    }

    private static void restartProjectivy(Context c) {
        try {
            Class<?> launcher = Class.forName("yb3");
            java.lang.reflect.Method restart = launcher.getDeclaredMethod("v", Context.class);
            restart.setAccessible(true);
            restart.invoke(null, c);
        } catch (Throwable t) {
            android.util.Log.e("ProjectivyProfile", "Launcher restart failed", t);
        }
    }

    private static byte[] readBytes(InputStream in, int max) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int total = 0;
        for (int n; (n = in.read(buf)) >= 0;) {
            total += n;
            if (total > max) throw new IllegalStateException("backup too large");
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static String decodeBackupJson(byte[] data) throws Exception {
        int first = 0;
        while (first < data.length && Character.isWhitespace((char) (data[first] & 0xff))) first++;
        if (first < data.length && data[first] == '{') return new String(data, StandardCharsets.UTF_8);
        if (data.length < 48) throw new IllegalArgumentException("Invalid Projectivy backup");

        byte[] salt = java.util.Arrays.copyOfRange(data, 0, 16);
        byte[] iv = java.util.Arrays.copyOfRange(data, 16, 32);
        byte[] key = "walt_sent_me".getBytes(StandardCharsets.UTF_8);
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        for (int i = 0; i < 10000; i++) {
            digest.reset();
            digest.update(key);
            digest.update(salt);
            key = digest.digest();
        }
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE,
                new javax.crypto.spec.SecretKeySpec(key, "AES"),
                new javax.crypto.spec.IvParameterSpec(iv));
        byte[] plain = cipher.doFinal(data, 32, data.length - 32);
        return new String(plain, StandardCharsets.UTF_8);
    }

'''
text = text.replace(anchor, methods + anchor, 1)
embedded.write_text(text)
print('patched v13 progressive profile/manual backup restore engine')

engine = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightEngine.java')
text = engine.read_text()
old = '''        saveStates(context, states);\n        restoreBundledChannelLayoutOnce(context, states);\n        notifyProjectivyChannelsChanged(context);'''
new = '''        saveStates(context, states);\n        EmbeddedRuntime.reconcilePendingChannelLayout(context);\n        notifyProjectivyChannelsChanged(context);'''
if text.count(old) != 1:
    raise SystemExit(f'v13 RedLight post-sync anchor mismatch: {text.count(old)}')
engine.write_text(text.replace(old, new, 1))
print('routed Red Light sync into progressive pending-channel reconciliation')
