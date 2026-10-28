package com.spocky.projengmenu.embedded;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CLEAN 06 foreground-only Red Light discovery trigger.
 *
 * A failed attempt is deliberately NOT terminal. Android can resume Projectivy's
 * HOME activity behind Kodi while Kodi is still warming. If that early attempt
 * cannot read Red Light yet, the next real MainActivity resume retries.
 */
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
                Log.i(TAG, "CLEAN06 discovery armed attempt=" + n + " on MainActivity resume");
                new Thread(() -> {
                    boolean ok = false;
                    try {
                        try { Thread.sleep(1800L); } catch (InterruptedException ignored) {}
                        ok = RedLightDiscoveryEngine.syncOnce(app);
                        if (ok) {
                            complete = true;
                            Log.i(TAG, "CLEAN06 discovery COMPLETE attempt=" + n);
                            unregister();
                        } else {
                            Log.i(TAG, "CLEAN06 discovery NO_ROW attempt=" + n + " — will retry on next MainActivity resume");
                        }
                    } finally {
                        RUNNING.set(false);
                    }
                }, "clean06-redlight").start();
            }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        };
        app.registerActivityLifecycleCallbacks(callbacks);
        Log.i(TAG, "CLEAN06 retry-capable discovery provider armed");
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
