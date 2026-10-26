package com.spocky.projengmenu.embedded;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Entry points for the Aerial + Red Light features embedded in Projectivy. */
public final class EmbeddedRuntime {
    public static final String AERIAL_ID = "8c00feaf-c7be-43c2-80f9-d49853aaa99a";
    private static final String PROFILE_MARKER = "launcher_embedded_profile_v2";
    private static final String ACTION_PROVIDER_UPDATED = "tv.projectivy.plugin.action.WALLPAPER_PROVIDER_UPDATED";
    private static final String EXTRA_PROVIDER_ID = "tv.projectivy.plugin.extra.PROVIDER_ID";
    private static final String EXTRA_UPDATE_REASON = "tv.projectivy.plugin.extra.UPDATE_REASON";
    static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean REDLIGHT_RUNNING = new AtomicBoolean(false);

    private EmbeddedRuntime() {}

    public static void onLauncherResume(Context context) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        applyDefaultProfile(app);
        repairEmbeddedAerialPackage(app);
        Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(() -> notifyAerial(app), 350L);
        h.postDelayed(() -> notifyAerial(app), 1400L);
        scheduleRedLight(app);
        syncRedLightAsync(app);
    }

    static SharedPreferences projectivyPrefs(Context c) {
        return c.getSharedPreferences(c.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    static void applyDefaultProfile(Context c) {
        SharedPreferences prefs = projectivyPrefs(c);
        if (prefs.getBoolean(PROFILE_MARKER, false)) return;
        SharedPreferences.Editor e = prefs.edit();
        try (InputStream in = c.getAssets().open("launcher_default_profile.json")) {
            JSONObject root = new JSONObject(readAll(in, 1024 * 1024));
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object v = root.opt(key);
                if (v == null || v == JSONObject.NULL) continue;
                if (v instanceof Boolean) e.putBoolean(key, (Boolean) v);
                else if (v instanceof Integer) e.putInt(key, (Integer) v);
                else if (v instanceof Long) e.putLong(key, (Long) v);
                else if (v instanceof Double) e.putFloat(key, ((Double) v).floatValue());
                else e.putString(key, String.valueOf(v));
            }
        } catch (Throwable ignored) {
            e.putBoolean("general_onboarding_completed", true);
        }
        e.putString("appearance_wallpaper_provider", AERIAL_ID);
        e.putString("appearance_wallpaper_provider_packagename", c.getPackageName());
        if (!prefs.contains("appearance_wallpaper_provider_params_" + AERIAL_ID)) {
            e.putString("appearance_wallpaper_provider_params_" + AERIAL_ID,
                    "{\"sources\":[\"A\",\"C1\",\"C2\"],\"prefer4k\":true,\"prefer_hdr\":false,\"fallback\":true}");
        }
        e.putBoolean(PROFILE_MARKER, true).apply();
    }

    static void repairEmbeddedAerialPackage(Context c) {
        SharedPreferences p = projectivyPrefs(c);
        if (!AERIAL_ID.equals(p.getString("appearance_wallpaper_provider", ""))) return;
        if (!c.getPackageName().equals(p.getString("appearance_wallpaper_provider_packagename", ""))) {
            p.edit().putString("appearance_wallpaper_provider_packagename", c.getPackageName()).apply();
        }
    }

    public static void notifyAerial(Context c) {
        try {
            Intent i = new Intent(ACTION_PROVIDER_UPDATED).setPackage(c.getPackageName());
            i.putExtra(EXTRA_PROVIDER_ID, AERIAL_ID);
            i.putExtra(EXTRA_UPDATE_REASON, 2);
            c.sendBroadcast(i);
        } catch (Throwable ignored) {}
    }

    static void syncRedLightAsync(Context c) {
        if (!REDLIGHT_RUNNING.compareAndSet(false, true)) return;
        EXECUTOR.execute(() -> {
            try { RedLightEngine.sync(c.getApplicationContext()); }
            catch (Throwable ignored) {}
            finally { REDLIGHT_RUNNING.set(false); }
        });
    }

    static void scheduleRedLight(Context c) {
        try {
            JobScheduler s = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (s == null || s.getPendingJob(SyncJob.JOB_ID) != null) return;
            JobInfo j = new JobInfo.Builder(SyncJob.JOB_ID, new ComponentName(c, SyncJob.class))
                    .setPeriodic(30L * 60L * 1000L).setPersisted(true).build();
            s.schedule(j);
        } catch (Throwable ignored) {}
    }

    static String readAll(InputStream in, int max) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[16384];
        int total = 0;
        for (int n; (n = in.read(b)) >= 0;) {
            total += n;
            if (total > max) throw new IllegalStateException("input too large");
            out.write(b, 0, n);
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }

    public static final class BootstrapProvider extends ContentProvider {
        @Override public boolean onCreate() {
            Context c = getContext();
            if (c != null) { applyDefaultProfile(c); repairEmbeddedAerialPackage(c); scheduleRedLight(c); }
            return true;
        }
        @Override public String getType(Uri u) { return null; }
        @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
        @Override public Uri insert(Uri u, ContentValues v) { return null; }
        @Override public int delete(Uri u, String s, String[] a) { return 0; }
        @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
    }

    public static final class BootReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context c, Intent i) {
            scheduleRedLight(c);
            if (Intent.ACTION_BOOT_COMPLETED.equals(i == null ? null : i.getAction())) notifyAerial(c);
        }
    }

    public static final class SyncJob extends JobService {
        static final int JOB_ID = 0x4b0d3;
        @Override public boolean onStartJob(JobParameters p) {
            EXECUTOR.execute(() -> { try { RedLightEngine.sync(getApplicationContext()); } catch (Throwable ignored) {} finally { jobFinished(p, false); } });
            return true;
        }
        @Override public boolean onStopJob(JobParameters p) { return true; }
    }
}
