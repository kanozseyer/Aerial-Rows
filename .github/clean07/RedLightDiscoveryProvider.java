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
import android.os.SystemClock;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/** CLEAN 12: cold-safe Red Light sync + Kodi retention lease + throttled warm refresh. */
public final class RedLightDiscoveryProvider extends ContentProvider {
    private static final String TAG = "ProjectivyRedLight";
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final Object KODI_LEASE_LOCK = new Object();
    private static final Uri KODI_FILE_PROVIDER = Uri.parse("content://org.xbmc.kodi.file");
    private static final long REFRESH_INTERVAL_MS = 5L * 60L * 1000L;
    private static volatile ContentProviderClient kodiProviderLease;

    private Application app;
    private Application.ActivityLifecycleCallbacks callbacks;
    private volatile boolean hasSuccessfulSync;
    private volatile long lastSuccessfulSyncElapsed;
    private int attempt;

    @Override public boolean onCreate() {
        Context c = getContext();
        if (c == null) return true;
        Context ac = c.getApplicationContext();
        if (!(ac instanceof Application)) return true;
        app = (Application) ac;

        callbacks = new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityResumed(Activity a) {
                if (a == null || !a.getClass().getName().equals("com.spocky.projengmenu.ui.home.MainActivity")) return;

                long now = SystemClock.elapsedRealtime();
                if (hasSuccessfulSync && lastSuccessfulSyncElapsed > 0L) {
                    long age = now - lastSuccessfulSyncElapsed;
                    if (age < REFRESH_INTERVAL_MS) {
                        Log.i(TAG, "CLEAN12 refresh throttled ageMs=" + age
                                + " remainingMs=" + (REFRESH_INTERVAL_MS - age));
                        return;
                    }
                }

                if (!RUNNING.compareAndSet(false, true)) return;
                final int n = ++attempt;
                final boolean refresh = hasSuccessfulSync;
                final WeakReference<Activity> activityRef = new WeakReference<>(a);
                Log.i(TAG, "CLEAN12 discovery armed attempt=" + n
                        + " mode=" + (refresh ? "refresh" : "initial")
                        + " on MainActivity resume");

                new Thread(() -> {
                    try {
                        try { Thread.sleep(1800L); } catch (InterruptedException ignored) {}

                        // Preserve CLEAN11/CLEAN11.1's safety rule: never acquire/query Kodi's
                        // exported provider while its native core is cold. Port 9090 remains
                        // the proven native-ready boundary on this Shield.
                        if (!isKodiNativeReady()) {
                            Log.i(TAG, "CLEAN12 Kodi native not ready - cached rows retained; no provider lease");
                            return;
                        }

                        boolean leased = ensureKodiProviderLease(app);
                        Log.i(TAG, "CLEAN12 Kodi provider retention lease=" + leased);
                        Log.i(TAG, "CLEAN12 Kodi native ready - starting Red Light "
                                + (refresh ? "refresh" : "initial sync"));

                        boolean ok = RedLightDiscoveryEngine.syncOnce(app);
                        if (ok) {
                            hasSuccessfulSync = true;
                            lastSuccessfulSyncElapsed = SystemClock.elapsedRealtime();
                            Log.i(TAG, "CLEAN12 discovery COMPLETE attempt=" + n
                                    + " mode=" + (refresh ? "refresh" : "initial"));

                            // CLEAN12 intentionally keeps the lifecycle callback registered.
                            // A later Home resume may perform another sync, but only after the
                            // five-minute freshness window has expired. This prevents CLEAN11.1's
                            // long-lived process from leaving Continue Watching/In Progress stale.
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                Activity activity = activityRef.get();
                                if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
                                try {
                                    Log.i(TAG, "CLEAN12 requesting Projectivy channel-model reload");
                                    activity.recreate();
                                } catch (Throwable t) {
                                    Log.i(TAG, "CLEAN12 Projectivy reload request failed");
                                }
                            }, 450L);
                        } else {
                            Log.i(TAG, "CLEAN12 discovery INCOMPLETE attempt=" + n
                                    + " - will retry on next eligible MainActivity resume");
                        }
                    } finally {
                        RUNNING.set(false);
                    }
                }, "clean12-redlight").start();
            }
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        };
        app.registerActivityLifecycleCallbacks(callbacks);
        Log.i(TAG, "CLEAN12 discovery provider armed with cold gate + retention lease + 5m refresh throttle");
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
     * Android process importance follows active provider dependencies; CLEAN11.1 runtime testing
     * proved this lowers Kodi from oom_score_adj 700 to 0 on Home and about 150 under app pressure.
     */
    private static boolean ensureKodiProviderLease(Context context) {
        if (kodiProviderLease != null) return true;
        synchronized (KODI_LEASE_LOCK) {
            if (kodiProviderLease != null) return true;
            try {
                ContentProviderClient client = context.getContentResolver()
                        .acquireContentProviderClient(KODI_FILE_PROVIDER);
                if (client == null) {
                    Log.i(TAG, "CLEAN12 Kodi provider lease unavailable");
                    return false;
                }
                kodiProviderLease = client;
                Log.i(TAG, "CLEAN12 Kodi provider lease ACQUIRED");
                return true;
            } catch (Throwable t) {
                Log.e(TAG, "CLEAN12 Kodi provider lease failed", t);
                return false;
            }
        }
    }

    @Override public String getType(Uri u) { return null; }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
