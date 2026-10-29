#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean13d/StatusBarCustomizationBridge.java')
s = p.read_text()

marker = '            Log.i(TAG, "CLEAN13D2 native clock/date actions added; unified default");\n'
assert s.count(marker) == 1, f'actions marker count={s.count(marker)}'
s = s.replace(marker,
'''            WeatherStatusBarBridge.addWeatherActions(fragment, list);\n            Log.i(TAG, "CLEAN13D2 native clock/date actions added; unified default");\n''', 1)

old = '            if (!handled) return false;\n'
assert s.count(old) == 1, f'handler marker count={s.count(old)}'
s = s.replace(old, '            if (!handled) return WeatherStatusBarBridge.handleWeatherAction(fragment, action);\n', 1)

old = '''        try {\n            applyClockPreference(activity);\n            final WeakReference<Activity> ref = new WeakReference<>(activity);\n'''
new = '''        try {\n            applyClockPreference(activity);\n            WeatherStatusBarBridge.applyToHome(activity);\n            final WeakReference<Activity> ref = new WeakReference<>(activity);\n'''
assert s.count(old) == 1, f'Home marker count={s.count(old)}'
s = s.replace(old, new, 1)

old = '''                        try {\n                            applyVisual(a);\n                        } catch (Throwable t) {\n                            Log.e(TAG, "CLEAN13D delayed visual apply failed", t);\n                        }\n'''
new = '''                        try {\n                            applyVisual(a);\n                            WeatherStatusBarBridge.applyToHome(a);\n                        } catch (Throwable t) {\n                            Log.e(TAG, "CLEAN13D delayed visual apply failed", t);\n                        }\n'''
assert s.count(old) == 1, f'delayed visual marker count={s.count(old)}'
s = s.replace(old, new, 1)

p.write_text(s)
print('PASS: CLEAN13.1 weather wired into frozen CLEAN13D2 status bar bridge')
