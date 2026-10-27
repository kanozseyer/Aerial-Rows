from pathlib import Path

p = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java')
s = p.read_text()

# v9.4 diagnostic/fix: never foreground-launch Kodi.  The launcher must remain on
# screen while we passively wait for Kodi's own boot/background lifecycle to make
# JSON-RPC available.  Red Light provider access stays suppressed until that point.
resume_old = '''        scheduleRedLight(app);\n        if (RedLightEngine.hasPublishedChannels(app)) {\n            syncRedLightAsync(app);\n        } else {\n            scheduleKodiBootstrapWhenSettled(context, app, h);\n        }'''
resume_new = '''        scheduleRedLight(app);\n        scheduleKodiBootstrapWhenSettled(context, app, h);'''
if s.count(resume_old) != 1:
    raise SystemExit(f'passive resume gate mismatch: {s.count(resume_old)}')
s = s.replace(resume_old, resume_new, 1)

start = s.index('    private static void scheduleKodiBootstrapWhenSettled(Context launcherContext, Context app, Handler h) {')
end = s.index('    private static void beginKodiBootstrap(Context app, Handler h) {', start)
schedule_new = r'''    private static void scheduleKodiBootstrapWhenSettled(Context launcherContext, Context app, Handler h) {
        scheduleKodiBootstrapWhenSettled(launcherContext, app, h, 0);
    }

    private static void scheduleKodiBootstrapWhenSettled(Context launcherContext, Context app, Handler h, int attempt) {
        if (launcherContext == null || app == null || h == null) return;
        long delay = attempt == 0 ? 2500L : 1000L;
        h.postDelayed(() -> {
            if (launcherContext instanceof Activity) {
                Activity a = (Activity) launcherContext;
                if (a.isFinishing() || a.isDestroyed()) {
                    android.util.Log.i("ProjectivyKodiReady", "Launcher settle probe abandoned: stale Activity attempt=" + attempt);
                    return;
                }
                if (!a.hasWindowFocus()) {
                    if (attempt < 45) {
                        if (attempt == 0 || attempt % 5 == 0) {
                            android.util.Log.i("ProjectivyKodiReady", "Launcher not focused yet; retrying settle probe attempt=" + attempt);
                        }
                        scheduleKodiBootstrapWhenSettled(launcherContext, app, h, attempt + 1);
                    } else {
                        android.util.Log.e("ProjectivyKodiReady", "Launcher focus settle timeout; Kodi/provider untouched");
                    }
                    return;
                }
            }
            android.util.Log.i("ProjectivyKodiReady", "Launcher stable/focused; starting passive Kodi readiness watch");
            beginKodiBootstrap(app, h);
        }, delay);
    }

'''
s = s[:start] + schedule_new + s[end:]

start = s.index('    private static void beginKodiBootstrap(Context app, Handler h) {')
end = s.index('    private static void returnHome(Context app, String reason) {', start)
begin_new = r'''    private static void beginKodiBootstrap(Context app, Handler h) {
        if (app == null || h == null) return;
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - KODI_LAST_BOOTSTRAP_MS < 15000L) {
            android.util.Log.i("ProjectivyKodiReady", "Passive readiness watch skipped by cooldown");
            return;
        }
        if (!KODI_BOOTSTRAP_RUNNING.compareAndSet(false, true)) {
            android.util.Log.i("ProjectivyKodiReady", "Passive readiness watch already running");
            return;
        }
        KODI_LAST_BOOTSTRAP_MS = now;

        EXECUTOR.execute(() -> {
            boolean ready = false;
            try {
                android.util.Log.i("ProjectivyKodiReady", "Passive Kodi readiness watch started; foreground launch disabled");
                for (int attempt = 0; attempt < 100; attempt++) {
                    if (isKodiJsonRpcReady()) {
                        ready = true;
                        android.util.Log.i("ProjectivyKodiReady", "Kodi JSON-RPC became ready passively at attempt=" + (attempt + 1));
                        break;
                    }
                    if (attempt == 0 || (attempt + 1) % 10 == 0) {
                        android.util.Log.i("ProjectivyKodiReady", "Kodi still not JSON-RPC ready; passive attempt=" + (attempt + 1));
                    }
                    try { Thread.sleep(attempt < 20 ? 500L : 1000L); }
                    catch (InterruptedException stop) { Thread.currentThread().interrupt(); break; }
                }

                if (ready) {
                    KODI_BOOTSTRAP_RUNNING.set(false);
                    h.postDelayed(() -> {
                        android.util.Log.i("ProjectivyKodiReady", "Starting Red Light scan after passive Kodi readiness");
                        syncRedLightAsync(app);
                        h.postDelayed(() -> retryRedLightIfMissing(app), 3000L);
                        h.postDelayed(() -> retryRedLightIfMissing(app), 10000L);
                        h.postDelayed(() -> retryRedLightIfMissing(app), 30000L);
                    }, 500L);
                    return;
                }
                android.util.Log.e("ProjectivyKodiReady", "Passive Kodi readiness timeout; foreground launch and provider scan suppressed");
            } catch (Throwable t) {
                android.util.Log.e("ProjectivyKodiReady", "Passive Kodi readiness watch failed", t);
            } finally {
                if (!ready) KODI_BOOTSTRAP_RUNNING.set(false);
            }
        });
    }

'''
s = s[:start] + begin_new + s[end:]

# returnHome existed only for the foreground-launch experiment. Remove it entirely so
# there is no hidden launch/switch-back path left in the runtime.
start = s.index('    private static void returnHome(Context app, String reason) {')
end = s.index('    private static boolean isKodiJsonRpcReady() {', start)
s = s[:start] + s[end:]

# A persisted job must also obey readiness; otherwise an existing Red Light channel
# can cause an unsafe provider query before the passive gate has started.
job_old = '''            if (!RedLightEngine.hasPublishedChannels(app) || KODI_BOOTSTRAP_RUNNING.get()) {\n                jobFinished(p, false);\n                return false;\n            }'''
job_new = '''            if (!RedLightEngine.hasPublishedChannels(app) || KODI_BOOTSTRAP_RUNNING.get() || !isKodiJsonRpcReady()) {\n                android.util.Log.i("ProjectivyKodiReady", "Scheduled Red Light refresh skipped: Kodi JSON-RPC not ready");\n                jobFinished(p, false);\n                return false;\n            }'''
if s.count(job_old) != 1:
    raise SystemExit(f'passive SyncJob gate mismatch: {s.count(job_old)}')
s = s.replace(job_old, job_new, 1)

# Hard safety assertions: this build must contain no Kodi foreground-launch path.
for forbidden in [
    'getLeanbackLaunchIntentForPackage("org.xbmc.kodi")',
    'getLaunchIntentForPackage("org.xbmc.kodi")',
    'app.startActivity(kodi)',
    'Launched Kodi normally',
    'Returned Home after Kodi bootstrap',
]:
    if forbidden in s:
        raise SystemExit('foreground Kodi launch path still present: ' + forbidden)

for required in [
    'Passive Kodi readiness watch started; foreground launch disabled',
    'Launcher not focused yet; retrying settle probe',
    'Kodi JSON-RPC became ready passively',
    'Passive Kodi readiness timeout; foreground launch and provider scan suppressed',
    'Scheduled Red Light refresh skipped: Kodi JSON-RPC not ready',
]:
    if required not in s:
        raise SystemExit('passive readiness marker missing: ' + required)

p.write_text(s)
print('patched v9 passive Kodi readiness: no foreground launch, retrying focus gate, provider-safe jobs')
