package com.spocky.projengmenu.embedded;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicBoolean;

/** CLEAN 05: one-time canonical profile restore using Projectivy's own decrypt/import path. */
public final class ProfileRestoreProvider extends ContentProvider {
    private static final String TAG = "ProjectivyProfile";
    private static final String STATE = "clean05_profile_restore_state";
    private static final String DONE = "canonical_profile_applied_cda71dba_v1";
    private static final String ASSET = "launcher_default_profile.plbackup";
    private static final String EXPECTED_SHA256 = "cda71dbae3d2eca7bd298a198aca4068187bde34f25727df9e9ddd18e8aac2b7";
    private static final String AERIAL_ID = "8c00feaf-c7be-43c2-80f9-d49853aaa99a";
    private static final AtomicBoolean APPLYING = new AtomicBoolean(false);

    private Application app;
    private Application.ActivityLifecycleCallbacks callbacks;

    @Override public boolean onCreate() {
        Context c = getContext();
        if (c == null) return true;
        Context ac = c.getApplicationContext();
        if (!(ac instanceof Application)) return true;
        app = (Application) ac;
        if (state(app).getBoolean(DONE, false)) {
            Log.i(TAG, "CLEAN05 profile already applied");
            return true;
        }
        callbacks = new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity activity) {
                if (activity == null || !"MainActivity".equals(activity.getClass().getSimpleName())) return;
                if (state(app).getBoolean(DONE, false)) { unregister(); return; }
                new Handler(Looper.getMainLooper()).postDelayed(() -> applyOnce(activity), 450L);
            }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        };
        app.registerActivityLifecycleCallbacks(callbacks);
        Log.i(TAG, "CLEAN05 restore armed for first MainActivity resume");
        return true;
    }

    private void applyOnce(Activity activity) {
        if (!APPLYING.compareAndSet(false, true)) return;
        try {
            if (state(app).getBoolean(DONE, false)) { unregister(); return; }
            Log.i(TAG, "CLEAN05 restore START");

            byte[] encrypted = readAsset(app, ASSET, 1024 * 1024);
            String actual = sha256(encrypted);
            if (!EXPECTED_SHA256.equals(actual)) throw new IllegalStateException("profile SHA256 mismatch: " + actual);

            Class<?> s71 = Class.forName("s71");
            Method decrypt = s71.getDeclaredMethod("b", byte[].class);
            decrypt.setAccessible(true);
            byte[] plain = (byte[]) decrypt.invoke(null, (Object) encrypted);
            if (plain == null || plain.length == 0) throw new IllegalStateException("Projectivy decryptor returned no data");

            JSONObject root = new JSONObject(new String(plain, StandardCharsets.UTF_8));
            // Category ordering is intentionally deferred until real Android-TV channels exist.
            root.remove("action_categories_list");
            // Never import markers belonging to older embedded experiments.
            root.remove("launcher_embedded_profile_v2");
            root.remove("launcher_embedded_channel_layout_v1");
            root.remove("launcher_embedded_channel_layout_v2");
            root.remove("launcher_embedded_channel_layout_attempts_v2");

            Class<?> dj3 = Class.forName("dj3");
            Method clear = dj3.getDeclaredMethod("n", boolean.class);
            clear.setAccessible(true);
            clear.invoke(null, true);

            SharedPreferences prefs = projectivyPrefs(app);
            Method merge = dj3.getDeclaredMethod("h", SharedPreferences.class, String.class);
            merge.setAccessible(true);
            merge.invoke(null, prefs, root.toString());

            Class<?> hj3 = Class.forName("hj3");
            Method migrate = hj3.getDeclaredMethod("a", boolean.class);
            migrate.setAccessible(true);
            migrate.invoke(null, false);

            SharedPreferences.Editor e = prefs.edit();
            e.putBoolean("general_onboarding_completed", true);
            e.putBoolean("general_default_launcher_asked", true);
            e.putString("appearance_wallpaper_provider", AERIAL_ID);
            e.putString("appearance_wallpaper_provider_packagename", app.getPackageName());
            e.commit();

            Method reset = dj3.getDeclaredMethod("m");
            reset.setAccessible(true);
            reset.invoke(null);

            if (!state(app).edit().putBoolean(DONE, true).commit()) {
                throw new IllegalStateException("could not persist CLEAN05 completion marker");
            }
            Log.i(TAG, "CLEAN05 restore SUCCESS");
            unregister();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    if (!activity.isFinishing() && !activity.isDestroyed()) activity.recreate();
                } catch (Throwable ignored) {}
            }, 350L);
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN05 restore FAILED", t);
        } finally {
            APPLYING.set(false);
        }
    }

    private void unregister() {
        try {
            if (app != null && callbacks != null) app.unregisterActivityLifecycleCallbacks(callbacks);
        } catch (Throwable ignored) {}
        callbacks = null;
    }

    private static SharedPreferences state(Context c) {
        return c.getSharedPreferences(STATE, Context.MODE_PRIVATE);
    }

    private static SharedPreferences projectivyPrefs(Context c) {
        return c.getSharedPreferences(c.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    private static byte[] readAsset(Context c, String name, int max) throws Exception {
        try (InputStream in = c.getAssets().open(name); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[16384];
            int total = 0;
            for (int n; (n = in.read(b)) >= 0;) {
                total += n;
                if (total > max) throw new IllegalStateException("profile asset too large");
                out.write(b, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static String sha256(byte[] data) throws Exception {
        byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder s = new StringBuilder(d.length * 2);
        for (byte b : d) s.append(String.format("%02x", b & 0xff));
        return s.toString();
    }

    @Override public String getType(Uri uri) { return null; }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
