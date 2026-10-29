package com.spocky.projengmenu.embedded;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentProviderClient;
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

/** CLEAN 11.1 foreground-only Red Light sync + Kodi provider retention lease. */
public final class RedLightDiscoveryProvider extends ContentProvider {
    private static final String TAG = "ProjectivyRedLight";
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final Object KODI_LEASE_LOCK = new Object();
    private static final Uri KODI_FILE_PROVIDER = Uri.parse("content://org.xbmc.kodi.file");
    private static volatile ContentProviderClient kodiProviderLease;

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
                Log.i(TAG, "CLEAN11.1 discovery armed attempt=" + n + " on MainActivity resume");
                new Thread(() -> {
                    try {
                        try { Thread.sleep(1800L); } catch (InterruptedException ignored) {}

                        // Preserve CLEAN11's safety rule: never acquire/query Kodi's provider
                        // while Kodi's native core is cold. The stable provider client is only
                        // acquired after port 9090 proves native Kodi is already alive.
                        if (!isKodiNativeReady()) {
                            Log.i(TAG, "CLEAN11.1 Kodi native not ready - cached rows retained; no provider lease");
                            return;
                        }

                        boolean leased = ensureKodiProviderLease(app);
                        Log.i(TAG, "CLEAN11.1 Kodi provider retention lease=" + leased);
                        Log.i(TAG, "CLEAN11.1 Kodi native ready - starting Red Light sync");

                        boolean ok = RedLightDiscoveryEngine.syncOnce(app);
                        if (ok) {
                            complete = true;
                            Log.i(TAG, "CLEAN11.1 discovery COMPLETE attempt=" + n);
                            unregister();
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                Activity activity = activityRef.get();
                                if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
                                try {
                                    Log.i(TAG, "CLEAN11.1 requesting Projectivy channel-model reload");
                                    activity.recreate();
                                } catch (Throwable t) {
                                    Log.i(TAG, "CLEAN11.1 Projectivy reload request failed");
                                }
                            }, 450L);
                        } else {
                            Log.i(TAG, "CLEAN11.1 discovery INCOMPLETE attempt=" + n + " - will retry on next MainActivity resume");
                        }
                    } finally {
                        RUNNING.set(false);
                    }
                }, "clean11-1-redlight").start();
            }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        };
        app.registerActivityLifecycleCallbacks(callbacks);
        Log.i(TAG, "CLEAN11.1 discovery provider armed with Kodi-native gate + retention lease");
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

    /**
     * Hold a stable reference to Kodi's exported file provider only after native Kodi is ready.
     * Android process importance follows active provider dependencies, so while Projectivy is the
     * foreground Home process this should make Kodi substantially harder for LMK to evict.
     */
    private static boolean ensureKodiProviderLease(Context context) {
        if (kodiProviderLease != null) return true;
        synchronized (KODI_LEASE_LOCK) {
            if (kodiProviderLease != null) return true;
            try {
                ContentProviderClient client = context.getContentResolver()
                        .acquireContentProviderClient(KODI_FILE_PROVIDER);
                if (client == null) {
                    Log.i(TAG, "CLEAN11.1 Kodi provider lease unavailable");
                    return false;
                }
                kodiProviderLease = client;
                Log.i(TAG, "CLEAN11.1 Kodi provider lease ACQUIRED");
                return true;
            } catch (Throwable t) {
                Log.e(TAG, "CLEAN11.1 Kodi provider lease failed", t);
                return false;
            }
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
