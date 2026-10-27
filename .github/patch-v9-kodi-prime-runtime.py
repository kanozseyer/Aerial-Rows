from pathlib import Path

p = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java')
s = p.read_text()

field_anchor = '    private static final AtomicBoolean REDLIGHT_RUNNING = new AtomicBoolean(false);\n'
field_new = field_anchor + '    private static final AtomicBoolean KODI_PRIME_ATTEMPTED = new AtomicBoolean(false);\n'
if s.count(field_anchor) != 1:
    raise SystemExit('REDLIGHT_RUNNING field anchor mismatch')
s = s.replace(field_anchor, field_new, 1)

resume_anchor = '''        scheduleRedLight(app);\n        syncRedLightAsync(app);\n        h.postDelayed(() -> retryRedLightIfMissing(app), 3000L);'''
resume_new = '''        scheduleRedLight(app);\n        primeKodiCoreIfNeeded(context, app, h);\n        syncRedLightAsync(app);\n        h.postDelayed(() -> retryRedLightIfMissing(app), 3000L);'''
if s.count(resume_anchor) != 1:
    raise SystemExit('launcher resume prime anchor mismatch')
s = s.replace(resume_anchor, resume_new, 1)

method_anchor = '    private static void retryRedLightIfMissing(Context c) {'
prime_method = '''    private static void primeKodiCoreIfNeeded(Context launcherContext, Context app, Handler h) {\n        if (launcherContext == null || app == null || h == null) return;\n        if (RedLightEngine.hasPublishedChannels(app)) return;\n        if (!KODI_PRIME_ATTEMPTED.compareAndSet(false, true)) return;\n\n        boolean started = false;\n        try {\n            Intent kodi = new Intent(Intent.ACTION_MAIN);\n            kodi.setComponent(new ComponentName("org.xbmc.kodi", "org.xbmc.kodi.Main"));\n            kodi.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK\n                    | Intent.FLAG_ACTIVITY_NO_ANIMATION\n                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);\n            launcherContext.startActivity(kodi);\n            android.util.Log.i("ProjectivyKodiPrime", "Started Kodi Main to initialise native core");\n            started = true;\n        } catch (Throwable directFailure) {\n            android.util.Log.w("ProjectivyKodiPrime", "Direct Kodi Main launch failed; trying launcher intent", directFailure);\n            try {\n                Intent fallback = app.getPackageManager().getLaunchIntentForPackage("org.xbmc.kodi");\n                if (fallback != null) {\n                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK\n                            | Intent.FLAG_ACTIVITY_NO_ANIMATION\n                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);\n                    launcherContext.startActivity(fallback);\n                    android.util.Log.i("ProjectivyKodiPrime", "Started Kodi launcher intent to initialise native core");\n                    started = true;\n                }\n            } catch (Throwable fallbackFailure) {\n                android.util.Log.e("ProjectivyKodiPrime", "Kodi prime launch failed", fallbackFailure);\n            }\n        }\n\n        if (!started) return;\n        h.postDelayed(() -> {\n            try {\n                Intent home = new Intent(Intent.ACTION_MAIN);\n                home.addCategory(Intent.CATEGORY_HOME);\n                home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK\n                        | Intent.FLAG_ACTIVITY_CLEAR_TOP\n                        | Intent.FLAG_ACTIVITY_NO_ANIMATION);\n                app.startActivity(home);\n                android.util.Log.i("ProjectivyKodiPrime", "Returned to launcher after Kodi native-core prime");\n            } catch (Throwable t) {\n                android.util.Log.e("ProjectivyKodiPrime", "Could not return to launcher after Kodi prime", t);\n            }\n        }, 1800L);\n    }\n\n'''
if s.count(method_anchor) != 1:
    raise SystemExit('retry helper anchor mismatch')
s = s.replace(method_anchor, prime_method + method_anchor, 1)

for marker in [
    'KODI_PRIME_ATTEMPTED',
    'org.xbmc.kodi.Main',
    'ProjectivyKodiPrime',
    'primeKodiCoreIfNeeded(context, app, h)',
    'Returned to launcher after Kodi native-core prime',
]:
    if marker not in s:
        raise SystemExit('prime marker missing: ' + marker)

p.write_text(s)
print('patched one-shot Kodi native-core prime for first Red Light bootstrap')
