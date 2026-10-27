package com.spocky.projengmenu.embedded;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Applies the bundled Projectivy profile through Projectivy's own preference
 * importer/migration path. The Android-TV category list remains owned by the
 * existing v8/v9 delayed channel-layout restore.
 */
public final class ProfileRestoreBridge {
    private static final String STATE = "embedded_v9_profile_state";
    private static final String DONE = "native_profile_applied_v1";
    private static final String LIVE_DONE = "native_appearance_applied_v1";
    private static final AtomicBoolean APPLYING = new AtomicBoolean(false);

    private ProfileRestoreBridge() {}

    private static SharedPreferences state(Context c) {
        return c.getApplicationContext().getSharedPreferences(STATE, Context.MODE_PRIVATE);
    }

    /** Safe values needed before MainActivity has finished constructing Projectivy. */
    public static void bootstrap(Context c) {
        if (c == null) return;
        SharedPreferences p = EmbeddedRuntime.projectivyPrefs(c);
        SharedPreferences.Editor e = p.edit();
        e.putBoolean("general_onboarding_completed", true);
        e.putBoolean("general_default_launcher_asked", true);
        e.putString("appearance_wallpaper_provider", EmbeddedRuntime.AERIAL_ID);
        e.putString("appearance_wallpaper_provider_packagename", c.getPackageName());
        if (!p.contains("appearance_wallpaper_provider_params_" + EmbeddedRuntime.AERIAL_ID)) {
            e.putString("appearance_wallpaper_provider_params_" + EmbeddedRuntime.AERIAL_ID,
                    "{\"sources\":[\"A\",\"C1\",\"C2\"],\"prefer4k\":true,\"prefer_hdr\":false,\"fallback\":true}");
        }
        e.apply();
    }

    public static void onLauncherResume(Context context) {
        if (context == null) return;
        final Context app = context.getApplicationContext();
        bootstrap(app);

        if (!state(app).getBoolean(DONE, false) && APPLYING.compareAndSet(false, true)) {
            boolean applied = false;
            try {
                JSONObject root = bundledProfile(app);
                root.remove("action_categories_list");
                root.remove("launcher_embedded_profile_v2");
                root.remove("launcher_embedded_channel_layout_v1");
                root.remove("launcher_embedded_channel_layout_v2");
                root.remove("launcher_embedded_channel_layout_attempts_v2");

                Class<?> dj3 = Class.forName("dj3");
                Method clear = dj3.getDeclaredMethod("n", boolean.class);
                clear.setAccessible(true);
                clear.invoke(null, true);

                SharedPreferences prefs = EmbeddedRuntime.projectivyPrefs(app);
                Method merge = dj3.getDeclaredMethod("h", SharedPreferences.class, String.class);
                merge.setAccessible(true);
                merge.invoke(null, prefs, root.toString());

                Class<?> hj3 = Class.forName("hj3");
                Method migrate = hj3.getDeclaredMethod("a", boolean.class);
                migrate.setAccessible(true);
                migrate.invoke(null, false);

                SharedPreferences.Editor e = prefs.edit();
                e.putString("appearance_wallpaper_provider", EmbeddedRuntime.AERIAL_ID);
                e.putString("appearance_wallpaper_provider_packagename", app.getPackageName());
                if (!prefs.contains("appearance_wallpaper_provider_params_" + EmbeddedRuntime.AERIAL_ID)) {
                    e.putString("appearance_wallpaper_provider_params_" + EmbeddedRuntime.AERIAL_ID,
                            "{\"sources\":[\"A\",\"C1\",\"C2\"],\"prefer4k\":true,\"prefer_hdr\":false,\"fallback\":true}");
                }
                e.apply();

                Method reset = dj3.getDeclaredMethod("m");
                reset.setAccessible(true);
                reset.invoke(null);

                state(app).edit().putBoolean(DONE, true).apply();
                applied = true;
            } catch (Throwable t) {
                android.util.Log.e("ProjectivyProfile", "Bundled native profile restore failed", t);
            } finally {
                APPLYING.set(false);
            }

            if (applied && context instanceof Activity) {
                final Activity a = (Activity) context;
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override public void run() {
                        try {
                            if (!a.isFinishing() && !a.isDestroyed()) a.recreate();
                        } catch (Throwable ignored) {}
                    }
                }, 350L);
            }
        }

        if (!state(app).getBoolean(LIVE_DONE, false)) {
            Handler h = new Handler(Looper.getMainLooper());
            h.postDelayed(new Runnable() { @Override public void run() { applyAppearanceLive(app); } }, 100L);
            h.postDelayed(new Runnable() { @Override public void run() { applyAppearanceLive(app); } }, 700L);
            h.postDelayed(new Runnable() { @Override public void run() { applyAppearanceLive(app); } }, 1600L);
        }
    }

    private static JSONObject bundledProfile(Context c) throws Exception {
        try (InputStream in = c.getAssets().open("launcher_default_profile.json")) {
            return new JSONObject(EmbeddedRuntime.readAll(in, 1024 * 1024));
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyAppearanceLive(Context c) {
        try {
            JSONObject root = bundledProfile(c);
            Class<?> yh3 = Class.forName("yh3");
            Field registryField = yh3.getDeclaredField("n");
            registryField.setAccessible(true);
            Object registryObject = registryField.get(null);
            if (!(registryObject instanceof List)) return;

            Field keyField = yh3.getDeclaredField("a");
            Field defaultField = yh3.getDeclaredField("b");
            keyField.setAccessible(true);
            defaultField.setAccessible(true);
            Method setter = yh3.getDeclaredMethod("h", Object.class);
            setter.setAccessible(true);

            boolean cardApplied = false;
            boolean iconApplied = false;
            for (Object pref : (List<Object>) registryObject) {
                if (pref == null) continue;
                String key = String.valueOf(keyField.get(pref));
                if (!key.startsWith("appearance_") || !root.has(key)) continue;
                Object raw = root.opt(key);
                Object def = defaultField.get(pref);
                Object value = coerce(raw, def);
                if (value == null && raw != JSONObject.NULL) continue;
                setter.invoke(pref, value);
                if ("appearance_cards_bg_tint".equals(key)) cardApplied = true;
                if ("appearance_statusbar_maincontainer_icon_background_tint".equals(key)) iconApplied = true;
            }

            if (cardApplied && iconApplied) {
                state(c).edit().putBoolean(LIVE_DONE, true).apply();
            }
        } catch (Throwable t) {
            android.util.Log.e("ProjectivyProfile", "Live appearance replay failed", t);
        }
    }

    private static Object coerce(Object raw, Object def) {
        if (raw == null || raw == JSONObject.NULL) return null;
        if (def instanceof Integer && raw instanceof Number) return ((Number) raw).intValue();
        if (def instanceof Long && raw instanceof Number) return ((Number) raw).longValue();
        if (def instanceof Float && raw instanceof Number) return ((Number) raw).floatValue();
        if (def instanceof Double && raw instanceof Number) return ((Number) raw).doubleValue();
        if (def instanceof Boolean && raw instanceof Boolean) return raw;
        if (def instanceof String) return String.valueOf(raw);
        return def != null && def.getClass().isInstance(raw) ? raw : null;
    }
}
