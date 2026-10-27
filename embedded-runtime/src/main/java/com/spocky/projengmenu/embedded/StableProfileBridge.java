package com.spocky.projengmenu.embedded;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * First-run profile restoration that deliberately leaves Android-TV channel
 * discovery to Projectivy/each provider.  State is kept outside Projectivy's
 * preferences so exporting/importing a .plbackup never exports our markers.
 */
public final class StableProfileBridge {
    private static final String STATE = "embedded_stable_profile_state";
    private static final String BASE_VERSION = "base_profile_version";
    private static final String LAYOUT_DONE = "layout_complete_v1";
    private static final String LAST_LAYOUT = "last_materialized_layout_v1";
    private static final String MANUAL_OVERRIDE = "manual_import_override";
    private static final int PROFILE_VERSION = 1;
    private static final AtomicBoolean APPLYING_BASE = new AtomicBoolean(false);
    private static final AtomicBoolean APPLYING_LAYOUT = new AtomicBoolean(false);
    private static volatile WeakReference<Activity> lastActivity = new WeakReference<>(null);

    private StableProfileBridge() {}

    private static SharedPreferences state(Context c) {
        return c.getApplicationContext().getSharedPreferences(STATE, Context.MODE_PRIVATE);
    }

    /** Only early values that must exist before Projectivy draws its first screen. */
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
        if (context instanceof Activity) lastActivity = new WeakReference<>((Activity) context);
        Context app = context.getApplicationContext();
        if (state(app).getBoolean(MANUAL_OVERRIDE, false)) return;

        applyBundledBaseOnce(app);

        // Give Projectivy/provider discovery normal control. These are read-only
        // checks followed by a Home-layout merge; no channel is created here.
        Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(() -> tryApplyBundledLayout(app), 900L);
        h.postDelayed(() -> tryApplyBundledLayout(app), 3000L);
        h.postDelayed(() -> tryApplyBundledLayout(app), 7000L);
    }

    public static void onManualImportSucceeded(Context c) {
        if (c == null) return;
        state(c).edit()
                .putBoolean(MANUAL_OVERRIDE, true)
                .putInt(BASE_VERSION, PROFILE_VERSION)
                .putBoolean(LAYOUT_DONE, true)
                .remove(LAST_LAYOUT)
                .apply();
    }

    private static JSONObject bundledProfile(Context c) throws Exception {
        try (InputStream in = c.getAssets().open("launcher_default_profile.json")) {
            return new JSONObject(EmbeddedRuntime.readAll(in, 1024 * 1024));
        }
    }

    private static void applyBundledBaseOnce(Context c) {
        SharedPreferences s = state(c);
        if (s.getBoolean(MANUAL_OVERRIDE, false)) return;
        if (s.getInt(BASE_VERSION, 0) >= PROFILE_VERSION) return;
        if (!APPLYING_BASE.compareAndSet(false, true)) return;
        try {
            JSONObject root = bundledProfile(c);
            root.remove("action_categories_list");
            root.remove("launcher_embedded_profile_v2");
            root.remove("launcher_embedded_channel_layout_v1");
            root.remove("launcher_embedded_channel_layout_v2");
            root.remove("launcher_embedded_channel_layout_attempts_v2");

            // Use Projectivy's own JSON preference importer instead of duplicating
            // its type conversion. This is a merge, not the destructive full import.
            Class<?> dj3 = Class.forName("dj3");
            Method merge = dj3.getDeclaredMethod("h", SharedPreferences.class, String.class);
            merge.setAccessible(true);
            merge.invoke(null, EmbeddedRuntime.projectivyPrefs(c), root.toString());

            // Run Projectivy's own preference migration/normalisation after the merge.
            try {
                Class<?> hj3 = Class.forName("hj3");
                Method migrate = hj3.getDeclaredMethod("a", boolean.class);
                migrate.setAccessible(true);
                migrate.invoke(null, false);
            } catch (Throwable ignored) {}

            s.edit().putInt(BASE_VERSION, PROFILE_VERSION).apply();
            refreshProjectivy(true);
        } catch (Throwable ignored) {
            // Leave BASE_VERSION unset so a later resume can retry.
        } finally {
            APPLYING_BASE.set(false);
        }
    }

    /** Called after Red Light sync too, so newly-created real channels can be merged immediately. */
    public static void tryApplyBundledLayout(Context c) {
        if (c == null) return;
        Context app = c.getApplicationContext();
        SharedPreferences s = state(app);
        if (s.getBoolean(MANUAL_OVERRIDE, false) || s.getBoolean(LAYOUT_DONE, false)) return;
        if (!APPLYING_LAYOUT.compareAndSet(false, true)) return;
        try {
            JSONObject profile = bundledProfile(app);
            String raw = profile.optString("action_categories_list", "");
            if (raw.isEmpty()) return;
            JSONArray wanted = new JSONArray(raw);
            Set<String> available = queryChannelUniqueIds(app);

            JSONArray materialized = new JSONArray();
            int visibleWanted = 0;
            int visibleReady = 0;

            for (int i = 0; i < wanted.length(); i++) {
                Object value = wanted.get(i);
                JSONObject o = value instanceof JSONObject ? (JSONObject) value : null;
                if (o == null || o.optInt("type", -1) != 6) {
                    materialized.put(value);
                    continue;
                }

                String uid = o.optString("channelUniqueId", "");
                boolean visible = !o.has("isVisible") || o.optBoolean("isVisible", true);
                if (visible) visibleWanted++;

                if (uid.isEmpty() || available.contains(uid)) {
                    materialized.put(o);
                    if (visible) visibleReady++;
                }
                // Missing channel rows are deliberately omitted for now. A later
                // provider/Red Light sync retries from the untouched bundled profile.
            }

            // Never replace Home with a channel-less profile while providers are still starting.
            if (visibleWanted > 0 && visibleReady == 0) return;

            String layout = materialized.toString();
            String previous = s.getString(LAST_LAYOUT, "");
            boolean complete = visibleWanted == visibleReady;

            if (!layout.equals(previous)) {
                EmbeddedRuntime.projectivyPrefs(app).edit()
                        .putString("action_categories_list", layout)
                        .apply();
                s.edit().putString(LAST_LAYOUT, layout).apply();
                refreshProjectivy(true);
            }

            if (complete) s.edit().putBoolean(LAYOUT_DONE, true).apply();
        } catch (Throwable ignored) {
            // Keep pending; a later resume/sync retries.
        } finally {
            APPLYING_LAYOUT.set(false);
        }
    }

    private static Set<String> queryChannelUniqueIds(Context c) {
        HashSet<String> out = new HashSet<>();
        Cursor cur = null;
        try {
            cur = c.getContentResolver().query(TvContract.Channels.CONTENT_URI,
                    new String[]{"package_name", "display_name"}, null, null, null);
            if (cur == null) return out;
            int pkgCol = cur.getColumnIndex("package_name");
            int nameCol = cur.getColumnIndex("display_name");
            while (cur.moveToNext()) {
                String pkg = pkgCol >= 0 ? cur.getString(pkgCol) : null;
                String name = nameCol >= 0 ? cur.getString(nameCol) : null;
                if (pkg != null && name != null) out.add(pkg + name);
            }
        } catch (Throwable ignored) {
        } finally {
            if (cur != null) cur.close();
        }
        return out;
    }

    private static void refreshProjectivy(boolean recreate) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Class<?> dj3 = Class.forName("dj3");
                Method reset = dj3.getDeclaredMethod("m");
                reset.setAccessible(true);
                reset.invoke(null);
            } catch (Throwable ignored) {}
            try {
                Class<?> w40 = Class.forName("w40");
                Method reload = w40.getDeclaredMethod("z", boolean.class);
                reload.setAccessible(true);
                reload.invoke(null, false);
            } catch (Throwable ignored) {}

            if (recreate) {
                Activity a = lastActivity.get();
                if (a != null && !a.isFinishing() && !a.isDestroyed()) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try { if (!a.isFinishing() && !a.isDestroyed()) a.recreate(); }
                        catch (Throwable ignored) {}
                    }, 250L);
                }
            }
        });
    }
}
