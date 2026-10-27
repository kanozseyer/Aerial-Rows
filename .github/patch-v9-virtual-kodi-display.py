from pathlib import Path

p = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java')
s = p.read_text()

field_anchor = '    private static volatile long KODI_LAST_BOOTSTRAP_MS = 0L;\n'
field_new = field_anchor + '''    private static volatile android.hardware.display.VirtualDisplay KODI_HIDDEN_DISPLAY;\n    private static volatile android.media.ImageReader KODI_HIDDEN_READER;\n'''
if s.count(field_anchor) != 1:
    raise SystemExit(f'virtual display field anchor mismatch: {s.count(field_anchor)}')
s = s.replace(field_anchor, field_new, 1)

start = s.index('    private static void beginKodiBootstrap(Context app, Handler h) {')
end = s.index('    private static boolean isKodiJsonRpcReady() {', start)

begin_new = r'''    private static void beginKodiBootstrap(Context app, Handler h) {
        if (app == null || h == null) return;
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - KODI_LAST_BOOTSTRAP_MS < 15000L) {
            android.util.Log.i("ProjectivyKodiReady", "Hidden Kodi bootstrap skipped by cooldown");
            return;
        }
        if (!KODI_BOOTSTRAP_RUNNING.compareAndSet(false, true)) {
            android.util.Log.i("ProjectivyKodiReady", "Hidden Kodi bootstrap already running");
            return;
        }
        KODI_LAST_BOOTSTRAP_MS = now;

        EXECUTOR.execute(() -> {
            boolean ready = false;
            boolean launchedHidden = false;
            try {
                if (isKodiJsonRpcReady()) {
                    ready = true;
                    android.util.Log.i("ProjectivyKodiReady", "Kodi JSON-RPC already ready; hidden launch not required");
                } else {
                    launchedHidden = startKodiOnHiddenDisplay(app, h);
                    if (!launchedHidden) {
                        android.util.Log.e("ProjectivyKodiReady", "Hidden Kodi launch unavailable; provider scan suppressed");
                    }
                }

                if (!ready && launchedHidden) {
                    for (int attempt = 0; attempt < 80; attempt++) {
                        try { Thread.sleep(attempt < 20 ? 250L : 500L); }
                        catch (InterruptedException stop) { Thread.currentThread().interrupt(); break; }
                        if (isKodiJsonRpcReady()) {
                            ready = true;
                            android.util.Log.i("ProjectivyKodiReady", "Kodi JSON-RPC ready from hidden display at attempt=" + (attempt + 1));
                            break;
                        }
                        if (attempt == 0 || (attempt + 1) % 10 == 0) {
                            android.util.Log.i("ProjectivyKodiReady", "Waiting for hidden Kodi native core; attempt=" + (attempt + 1));
                        }
                    }
                }

                if (ready) {
                    KODI_BOOTSTRAP_RUNNING.set(false);
                    h.postDelayed(() -> {
                        android.util.Log.i("ProjectivyKodiReady", "Starting Red Light scan after hidden Kodi readiness");
                        syncRedLightAsync(app);
                        h.postDelayed(() -> retryRedLightIfMissing(app), 3000L);
                        h.postDelayed(() -> retryRedLightIfMissing(app), 10000L);
                        h.postDelayed(() -> retryRedLightIfMissing(app), 30000L);
                    }, 500L);
                    return;
                }

                android.util.Log.e("ProjectivyKodiReady", "Hidden Kodi readiness timeout; provider scan suppressed");
                if (launchedHidden) releaseHiddenKodiDisplay();
            } catch (Throwable t) {
                android.util.Log.e("ProjectivyKodiReady", "Hidden Kodi bootstrap failed", t);
                if (launchedHidden) releaseHiddenKodiDisplay();
            } finally {
                if (!ready) KODI_BOOTSTRAP_RUNNING.set(false);
            }
        });
    }

    private static boolean startKodiOnHiddenDisplay(Context app, Handler h) {
        try {
            if (android.os.Build.VERSION.SDK_INT < 26) {
                android.util.Log.e("ProjectivyKodiReady", "Secondary-display bootstrap requires API 26+");
                return false;
            }
            boolean feature = app.getPackageManager().hasSystemFeature(
                    android.content.pm.PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS);
            android.util.Log.i("ProjectivyKodiReady", "Secondary-display activity feature=" + feature);
            if (!feature) return false;

            android.hardware.display.DisplayManager dm =
                    (android.hardware.display.DisplayManager) app.getSystemService(Context.DISPLAY_SERVICE);
            if (dm == null) {
                android.util.Log.e("ProjectivyKodiReady", "DisplayManager unavailable");
                return false;
            }

            android.media.ImageReader reader = android.media.ImageReader.newInstance(
                    640, 360, android.graphics.PixelFormat.RGBA_8888, 3);
            reader.setOnImageAvailableListener(r -> {
                android.media.Image image = null;
                try { image = r.acquireLatestImage(); }
                catch (Throwable ignored) {}
                finally { if (image != null) image.close(); }
            }, h);

            int flags = android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
                    | android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY;
            android.hardware.display.VirtualDisplay vd = dm.createVirtualDisplay(
                    "Projectivy-Kodi-Hidden", 640, 360, 160, reader.getSurface(), flags);
            if (vd == null || vd.getDisplay() == null) {
                android.util.Log.e("ProjectivyKodiReady", "Could not create public own-content virtual display");
                try { reader.close(); } catch (Throwable ignored) {}
                return false;
            }

            int displayId = vd.getDisplay().getDisplayId();
            Intent kodi = new Intent(Intent.ACTION_MAIN);
            kodi.setComponent(new android.content.ComponentName("org.xbmc.kodi", "org.xbmc.kodi.Main"));
            kodi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);

            boolean allowed = true;
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                android.app.ActivityManager am =
                        (android.app.ActivityManager) app.getSystemService(Context.ACTIVITY_SERVICE);
                allowed = am != null && am.isActivityStartAllowedOnDisplay(app, displayId, kodi);
            }
            android.util.Log.i("ProjectivyKodiReady", "Hidden display id=" + displayId
                    + " flags=PUBLIC|OWN_CONTENT_ONLY startAllowed=" + allowed);
            if (!allowed) {
                try { vd.release(); } catch (Throwable ignored) {}
                try { reader.close(); } catch (Throwable ignored) {}
                return false;
            }

            android.app.ActivityOptions options = android.app.ActivityOptions.makeBasic();
            options.setLaunchDisplayId(displayId);

            KODI_HIDDEN_READER = reader;
            KODI_HIDDEN_DISPLAY = vd;
            app.startActivity(kodi, options.toBundle());
            android.util.Log.i("ProjectivyKodiReady", "Requested Kodi Main on hidden virtual display id="
                    + displayId + "; TV display remains Projectivy");
            return true;
        } catch (Throwable t) {
            android.util.Log.e("ProjectivyKodiReady", "Could not launch Kodi on hidden virtual display", t);
            releaseHiddenKodiDisplay();
            return false;
        }
    }

    private static void releaseHiddenKodiDisplay() {
        android.hardware.display.VirtualDisplay vd = KODI_HIDDEN_DISPLAY;
        KODI_HIDDEN_DISPLAY = null;
        if (vd != null) {
            try { vd.release(); } catch (Throwable ignored) {}
        }
        android.media.ImageReader reader = KODI_HIDDEN_READER;
        KODI_HIDDEN_READER = null;
        if (reader != null) {
            try { reader.close(); } catch (Throwable ignored) {}
        }
        android.util.Log.i("ProjectivyKodiReady", "Released hidden Kodi virtual display");
    }

'''

s = s[:start] + begin_new + s[end:]

# This diagnostic intentionally uses only an explicitly targeted secondary display.
# Reject accidental normal/default-display launch patterns.
for forbidden in [
    'getLeanbackLaunchIntentForPackage("org.xbmc.kodi")',
    'getLaunchIntentForPackage("org.xbmc.kodi")',
    'Returned Home after Kodi bootstrap',
    'Launched Kodi normally',
]:
    if forbidden in s:
        raise SystemExit('normal Kodi launch path still present: ' + forbidden)

for required in [
    'VIRTUAL_DISPLAY_FLAG_PUBLIC',
    'VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY',
    'setLaunchDisplayId(displayId)',
    'org.xbmc.kodi.Main',
    'Requested Kodi Main on hidden virtual display',
    'Kodi JSON-RPC ready from hidden display',
    'Starting Red Light scan after hidden Kodi readiness',
]:
    if required not in s:
        raise SystemExit('virtual display marker missing: ' + required)

p.write_text(s)
print('patched v9.5 hidden virtual-display Kodi bootstrap diagnostic')
