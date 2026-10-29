#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean05/ProfileRestoreProvider.java')
text = p.read_text()

old = '''        app = (Application) ac;\n        if (state(app).getBoolean(DONE, false)) {\n            Log.i(TAG, "CLEAN05 profile already applied");\n            return true;\n        }\n'''
new = '''        app = (Application) ac;\n        // CLEAN13 RESCUE: remove the date-clock preference written by the failed\n        // CLEAN13 experiment before Projectivy renders Home. This preserves all\n        // other launcher data and lets Projectivy fall back to its native default.\n        try {\n            SharedPreferences prefs = projectivyPrefs(app);\n            if (prefs.contains("appearance_statusbar_clock_format_l1")) {\n                prefs.edit().remove("appearance_statusbar_clock_format_l1").commit();\n                Log.i(TAG, "CLEAN13 RESCUE removed clock format override");\n            } else {\n                Log.i(TAG, "CLEAN13 RESCUE clock format override already absent");\n            }\n        } catch (Throwable t) {\n            Log.e(TAG, "CLEAN13 RESCUE clock reset failed", t);\n        }\n        if (state(app).getBoolean(DONE, false)) {\n            Log.i(TAG, "CLEAN05 profile already applied");\n            return true;\n        }\n'''
if text.count(old) != 1:
    raise SystemExit(f'rescue anchor mismatch: {text.count(old)}')
text = text.replace(old, new, 1)
p.write_text(text)

check = p.read_text()
for token in [
    'CLEAN13 RESCUE removed clock format override',
    'remove("appearance_statusbar_clock_format_l1")',
    'CLEAN05 profile already applied',
]:
    if token not in check:
        raise SystemExit('rescue source verification failed: ' + token)
print('PASS: CLEAN13 rescue clock reset prepared')
