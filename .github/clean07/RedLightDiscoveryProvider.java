package com.spocky.projengmenu.embedded;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

/** CLEAN 07 foreground-only dynamic Red Light sync trigger. */
public final class RedLightDiscoveryProvider extends ContentProvider {
    private static final String TAG = "ProjectivyRedLight";
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private Application app;
    private Application.ActivityLifecycleCallbacks callbacks;
    private volatile boolean complete;
    private int attempt;

    @Override public boolean onCreate() {
        Context c = getContext();
        if (c == null) return true;
        Context ac = c.getApplicationContext();
        if (!(ac instanceof Application)) return true;
        app = (Application) ac;

        callbacks = new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity a) {
                if (complete || a == null || !a.getClass().getName().equals("com.spocky.projengmenu.ui.home.MainActivity")) return;
                if (!RUNNING.compareAndSet(false, true)) return;
                final int n = ++attempt;
                final WeakReference<Activity> activityRef = new WeakReference<>(a);
                Log.i(TAG, "CLEAN07 discovery armed attempt=" + n + " on MainActivity resume");
                new Thread(() -> {
                    try {
                        try { Thread.sleep(1800L); } catch (InterruptedException ignored) {}
                        boolean ok = RedLightDiscoveryEngine.syncOnce(app);
                        if (ok) {
                            complete = true;
                            Log.i(TAG, "CLEAN07 discovery COMPLETE attempt=" + n);
                            unregister();
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                Activity activity = activityRef.get();
                                if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
                                try {
                                    Log.i(TAG, "CLEAN07 requesting Projectivy channel-model reload");
                                    activity.recreate();
                                } catch (Throwable t) {
                                    Log.i(TAG, "CLEAN07 Projectivy reload request failed");
                                }
                            }, 450L);
                        } else {
                            Log.i(TAG, "CLEAN07 discovery INCOMPLETE attempt=" + n + " — will retry on next MainActivity resume");
                        }
                    } finally {
                        RUNNING.set(false);
                    }
                }, "clean07-redlight").start();
            }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        };
        app.registerActivityLifecycleCallbacks(callbacks);
        Log.i(TAG, "CLEAN07 dynamic discovery provider armed");
        return true;
    }

    private void unregister() {
        try {
            if (app != null && callbacks != null) app.unregisterActivityLifecycleCallbacks(callbacks);
        } catch (Throwable ignored) {}
        callbacks = null;
    }

    @Override public String getType(Uri u) { return null; }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
