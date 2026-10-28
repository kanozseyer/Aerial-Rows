#!/usr/bin/env python3
from pathlib import Path

# CLEAN 08.4 keeps the proven CLEAN 08.3 native appearance import and changes
# only the final UI refresh. The physical Android TV HOME action was proven at
# runtime to make the restored card background visible; mirror that path here.

engine = Path('.github/clean07/RedLightDiscoveryEngine.java')
text = engine.read_text()

old_marker = 'private static final String CLEAN08_APPLIED = "canonical_002828_applied_v4_native_appearance";'
new_marker = 'private static final String CLEAN08_APPLIED = "canonical_002828_applied_v5_home_intent_refresh";'
if text.count(old_marker) != 1:
    raise SystemExit(f'CLEAN084 marker anchor mismatch: {text.count(old_marker)}')
text = text.replace(old_marker, new_marker, 1)

old_refresh = '''                    // A real Activity reload is required for card renderers that already\n                    // captured the previous live appearance object. Run once because the\n                    // v4 marker/signature is persisted above before this restart occurs.\n                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {\n                        try {\n                            android.content.Intent restart = homeIntent(c);\n                            restart.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK\n                                    | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);\n                            c.startActivity(restart);\n                            Log.i(CLEAN08_TAG, "CLEAN083 Home restarted after native appearance apply");\n                        } catch (Throwable restartError) {\n                            Log.i(CLEAN08_TAG, "CLEAN083 Home restart unavailable");\n                        }\n                    }, 350L);\n'''
new_refresh = '''                    // CLEAN 08.4: the physical Shield HOME button is the runtime-proven\n                    // refresh path for Projectivy's already-created card renderers. Dispatch\n                    // the real Android HOME intent after the native appearance import has had\n                    // time to settle, instead of explicitly clearing/restarting MainActivity.\n                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {\n                        try {\n                            android.content.Intent homeRefresh = new android.content.Intent(\n                                    android.content.Intent.ACTION_MAIN);\n                            homeRefresh.addCategory(android.content.Intent.CATEGORY_HOME);\n                            homeRefresh.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK\n                                    | android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED\n                                    | android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);\n                            c.startActivity(homeRefresh);\n                            Log.i(CLEAN08_TAG, "CLEAN084 Android HOME intent dispatched after native appearance apply");\n                        } catch (Throwable homeError) {\n                            Log.i(CLEAN08_TAG, "CLEAN084 Android HOME intent unavailable");\n                        }\n                    }, 1500L);\n'''
if text.count(old_refresh) != 1:
    raise SystemExit(f'CLEAN084 refresh anchor mismatch: {text.count(old_refresh)}')
text = text.replace(old_refresh, new_refresh, 1)
engine.write_text(text)

check = engine.read_text()
for token in [
    'canonical_002828_applied_v5_home_intent_refresh',
    'appearancePatchJson',
    'Class.forName("hj3")',
    'migrate.invoke(null, false)',
    'android.content.Intent.ACTION_MAIN',
    'android.content.Intent.CATEGORY_HOME',
    'android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED',
    'android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP',
    'CLEAN084 Android HOME intent dispatched after native appearance apply',
    '}, 1500L);',
    'navigator.trakt_lists_personal',
    'redlight:trakt:watchlist:movies',
    'redlight:trakt:watchlist:shows',
]:
    if token not in check:
        raise SystemExit('CLEAN084 verification failed: ' + token)

if 'FLAG_ACTIVITY_CLEAR_TASK' in check:
    raise SystemExit('CLEAN084 still contains explicit CLEAR_TASK refresh')
if 'CLEAN083 Home restarted after native appearance apply' in check:
    raise SystemExit('CLEAN084 still contains old explicit MainActivity restart')

print('PASS: CLEAN 08.4 prepared — native appearance import + true Android HOME refresh')
