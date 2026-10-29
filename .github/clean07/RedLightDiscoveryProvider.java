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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/** CLEAN 11 foreground-only Red Light sync with Kodi-native readiness gate. */
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
                Log.i(TAG, "CLEAN11 discovery armed attempt=" + n + " on MainActivity resume");
                new Thread(() -> {
                    try {
                        try { Thread.sleep(1800L); } catch (InterruptedException ignored) {}

                        // Never poke Kodi's exported file provider while the native core is cold.
                        // Kodi 21.3's provider delegates to native _requestJSON(), which is not
                        // registered until Kodi's real native activity/core has started. Port
                        // 9090 is already proven by CLEAN10.3 TV browsing as the native-ready
                        // boundary on this Shield. If it is absent, leave Android TV's persisted
                        // Red Light rows/cards untouched and retry on a future Home resume.
                        if (!isKodiNativeReady()) {
                            Log.i(TAG, "CLEAN11 Kodi native not ready - cached rows retained; sync deferred");
                            return;
                        }

                        Log.i(TAG, "CLEAN11 Kodi native ready - starting Red Light sync");
                        boolean ok = RedLightDiscoveryEngine.syncOnce(app);
                        if (ok) {
                            complete = true;
                            Log.i(TAG, "CLEAN11 discovery COMPLETE attempt=" + n);
                            unregister();
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                Activity activity = activityRef.get();
                                if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
                                try {
                                    Log.i(TAG, "CLEAN11 requesting Projectivy channel-model reload");
                                    activity.recreate();
                                } catch (Throwable t) {
                                    Log.i(TAG, "CLEAN11 Projectivy reload request failed");
                                }
                            }, 450L);
                        } else {
                            Log.i(TAG, "CLEAN11 discovery INCOMPLETE attempt=" + n + " - will retry on next MainActivity resume");
                        }
                    } finally {
                        RUNNING.set(false);
                    }
                }, "clean11-redlight").start();
            }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        };
        app.registerActivityLifecycleCallbacks(callbacks);
        Log.i(TAG, "CLEAN11 dynamic discovery provider armed with Kodi-native readiness gate");
        return true;
    }

    private static boolean isKodiNativeReady() {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", 9090), 180);
            return true;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (socket != null) try { socket.close(); } catch (Throwable ignored) {}
        }
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
