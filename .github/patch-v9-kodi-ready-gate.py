from pathlib import Path

p = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java')
s = p.read_text()

# Imports used by the readiness gate.
if 'import android.app.Activity;' not in s:
    s = s.replace('import android.app.job.JobInfo;\n', 'import android.app.Activity;\nimport android.app.job.JobInfo;\n', 1)
for imp in [
    'import java.io.BufferedReader;',
    'import java.io.InputStreamReader;',
    'import java.io.OutputStream;',
    'import java.net.HttpURLConnection;',
    'import java.net.InetSocketAddress;',
    'import java.net.Socket;',
    'import java.net.URL;',
]:
    if imp not in s:
        s = s.replace('import java.io.ByteArrayOutputStream;\n', 'import java.io.ByteArrayOutputStream;\n' + imp + '\n', 1)

field_anchor = '    private static final AtomicBoolean REDLIGHT_RUNNING = new AtomicBoolean(false);\n'
field_new = field_anchor + '''    private static final AtomicBoolean KODI_BOOTSTRAP_RUNNING = new AtomicBoolean(false);\n    private static volatile long KODI_LAST_BOOTSTRAP_MS = 0L;\n'''
if s.count(field_anchor) != 1:
    raise SystemExit('REDLIGHT_RUNNING field anchor mismatch')
s = s.replace(field_anchor, field_new, 1)

# v6 deliberately started Red Light discovery inside Application bootstrap. That is
# unsafe on Shield after a cold boot because it starts Kodi only as a file-provider
# process before Kodi's native core is ready. First discovery is now launcher-gated.
bootstrap_old = 'if (c != null) { applyDefaultProfile(c); repairEmbeddedAerialPackage(c); scheduleRedLight(c); syncRedLightAsync(c); }'
bootstrap_new = 'if (c != null) { applyDefaultProfile(c); repairEmbeddedAerialPackage(c); scheduleRedLight(c); }'
if s.count(bootstrap_old) != 1:
    raise SystemExit(f'bootstrap sync anchor mismatch: {s.count(bootstrap_old)}')
s = s.replace(bootstrap_old, bootstrap_new, 1)

# Replace the cold-Kodi immediate/retry chain. Existing channels may refresh normally;
# a clean install must first establish that Kodi JSON-RPC is live before touching the
# XBMCFileContentProvider.
resume_old = '''        scheduleRedLight(app);\n        syncRedLightAsync(app);\n        h.postDelayed(() -> retryRedLightIfMissing(app), 3000L);\n        h.postDelayed(() -> retryRedLightIfMissing(app), 10000L);\n        h.postDelayed(() -> retryRedLightIfMissing(app), 30000L);\n        h.postDelayed(() -> retryRedLightIfMissing(app), 60000L);'''
resume_new = '''        scheduleRedLight(app);\n        if (RedLightEngine.hasPublishedChannels(app)) {\n            syncRedLightAsync(app);\n        } else {\n            scheduleKodiBootstrapWhenSettled(context, app, h);\n        }'''
if s.count(resume_old) != 1:
    raise SystemExit(f'cold retry block mismatch: {s.count(resume_old)}')
s = s.replace(resume_old, resume_new, 1)

method_anchor = '    private static void retryRedLightIfMissing(Context c) {'
if s.count(method_anchor) != 1:
    raise SystemExit('retry helper anchor missing')
methods = r'''    private static void scheduleKodiBootstrapWhenSettled(Context launcherContext, Context app, Handler h) {
        if (launcherContext == null || app == null || h == null) return;
        h.postDelayed(() -> {
            if (RedLightEngine.hasPublishedChannels(app)) return;
            if (launcherContext instanceof Activity) {
                Activity a = (Activity) launcherContext;
                if (a.isFinishing() || a.isDestroyed() || !a.hasWindowFocus()) {
                    android.util.Log.i("ProjectivyKodiReady", "Deferred Kodi bootstrap: launcher not settled/focused");
                    return;
                }
            }
            beginKodiBootstrap(app, h);
        }, 2500L);
    }

    private static void beginKodiBootstrap(Context app, Handler h) {
        if (app == null || h == null) return;
        if (RedLightEngine.hasPublishedChannels(app)) return;
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - KODI_LAST_BOOTSTRAP_MS < 15000L) return;
        if (!KODI_BOOTSTRAP_RUNNING.compareAndSet(false, true)) return;
        KODI_LAST_BOOTSTRAP_MS = now;

        EXECUTOR.execute(() -> {
            boolean ready = isKodiJsonRpcReady();
            boolean launched = false;
            try {
                if (!ready) {
                    Intent kodi = app.getPackageManager().getLeanbackLaunchIntentForPackage("org.xbmc.kodi");
                    if (kodi == null) kodi = app.getPackageManager().getLaunchIntentForPackage("org.xbmc.kodi");
                    if (kodi != null) {
                        kodi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        app.startActivity(kodi);
                        launched = true;
                        android.util.Log.i("ProjectivyKodiReady", "Launched Kodi normally; waiting for JSON-RPC readiness");
                    } else {
                        android.util.Log.e("ProjectivyKodiReady", "Kodi launcher intent unavailable");
                    }
                } else {
                    android.util.Log.i("ProjectivyKodiReady", "Kodi JSON-RPC already ready; no foreground launch needed");
                }

                if (!ready && launched) {
                    for (int attempt = 0; attempt < 50; attempt++) {
                        try { Thread.sleep(attempt < 12 ? 250L : 500L); }
                        catch (InterruptedException stop) { Thread.currentThread().interrupt(); break; }
                        if (isKodiJsonRpcReady()) {
                            ready = true;
                            android.util.Log.i("ProjectivyKodiReady", "Kodi JSON-RPC ready after attempt=" + (attempt + 1));
                            break;
                        }
                    }
                }

                final boolean becameReady = ready;
                if (launched) {
                    h.post(() -> returnHome(app, becameReady ? "ready" : "timeout"));
                }

                if (becameReady) {
                    // Release the gate only after Kodi's native JSON-RPC server is live.
                    // The provider scan is posted after Home is restored so Kodi remains
                    // alive in the background but its UI does not stay on screen.
                    KODI_BOOTSTRAP_RUNNING.set(false);
                    h.postDelayed(() -> {
                        android.util.Log.i("ProjectivyKodiReady", "Starting first Red Light scan after Kodi readiness");
                        syncRedLightAsync(app);
                        h.postDelayed(() -> retryRedLightIfMissing(app), 3000L);
                        h.postDelayed(() -> retryRedLightIfMissing(app), 10000L);
                        h.postDelayed(() -> retryRedLightIfMissing(app), 30000L);
                    }, 750L);
                    return;
                }
                android.util.Log.e("ProjectivyKodiReady", "Kodi JSON-RPC readiness timeout; provider scan suppressed");
            } catch (Throwable t) {
                android.util.Log.e("ProjectivyKodiReady", "Kodi readiness bootstrap failed", t);
            } finally {
                if (!ready) KODI_BOOTSTRAP_RUNNING.set(false);
            }
        });
    }

    private static void returnHome(Context app, String reason) {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            app.startActivity(home);
            android.util.Log.i("ProjectivyKodiReady", "Returned Home after Kodi bootstrap: " + reason);
        } catch (Throwable t) {
            android.util.Log.e("ProjectivyKodiReady", "Could not return Home after Kodi bootstrap", t);
        }
    }

    private static boolean isKodiJsonRpcReady() {
        String request = "{\"jsonrpc\":\"2.0\",\"id\":77,\"method\":\"JSONRPC.Ping\"}";
        return sendKodiTcp(request) || sendKodiHttp(request);
    }

    private static boolean sendKodiTcp(String request) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 9090), 180);
            socket.setSoTimeout(500);
            OutputStream out = socket.getOutputStream();
            out.write((request + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line = reader.readLine();
            return line != null && (line.contains("\"result\"") || line.contains("pong"));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean sendKodiHttp(String request) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("http://127.0.0.1:8080/jsonrpc").openConnection();
            connection.setConnectTimeout(180);
            connection.setReadTimeout(500);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(request.getBytes(StandardCharsets.UTF_8));
            }
            if (connection.getResponseCode() != 200) return false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("\"result\"") || line.contains("pong")) return true;
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (connection != null) connection.disconnect();
        }
        return false;
    }

'''
s = s.replace(method_anchor, methods + method_anchor, 1)

# Retries must never enter Kodi's provider while Main/native startup is in progress.
s = s.replace('''    private static void retryRedLightIfMissing(Context c) {\n        if (c == null) return;''', '''    private static void retryRedLightIfMissing(Context c) {\n        if (c == null || KODI_BOOTSTRAP_RUNNING.get()) return;''', 1)

# A persisted JobScheduler run may occur at boot. It may refresh existing channels,
# but it must never perform first discovery before the readiness bootstrap.
job_old = '''        @Override public boolean onStartJob(JobParameters p) {\n            EXECUTOR.execute(() -> { try { RedLightEngine.sync(getApplicationContext()); } catch (Throwable ignored) {} finally { jobFinished(p, false); } });\n            return true;\n        }'''
job_new = '''        @Override public boolean onStartJob(JobParameters p) {\n            Context app = getApplicationContext();\n            if (!RedLightEngine.hasPublishedChannels(app) || KODI_BOOTSTRAP_RUNNING.get()) {\n                jobFinished(p, false);\n                return false;\n            }\n            EXECUTOR.execute(() -> { try { RedLightEngine.sync(app); } catch (Throwable ignored) {} finally { jobFinished(p, false); } });\n            return true;\n        }'''
if s.count(job_old) != 1:
    raise SystemExit(f'SyncJob anchor mismatch: {s.count(job_old)}')
s = s.replace(job_old, job_new, 1)

for marker in [
    'ProjectivyKodiReady',
    'JSONRPC.Ping',
    'scheduleKodiBootstrapWhenSettled(context, app, h)',
    'provider scan suppressed',
    'KODI_BOOTSTRAP_RUNNING.get()',
]:
    if marker not in s:
        raise SystemExit('readiness marker missing: ' + marker)
if 'scheduleRedLight(c); syncRedLightAsync(c);' in s:
    raise SystemExit('unsafe BootstrapProvider Red Light sync still present')

p.write_text(s)
print('patched Kodi readiness-gated Red Light cold bootstrap')
