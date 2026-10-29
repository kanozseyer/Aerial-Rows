#!/usr/bin/env python3
from pathlib import Path

# CLEAN13.1B keeps the frozen CLEAN13D2 Clock page intact. Weather gets its
# own sibling page from the native Status Bar parent (qg) via classes2 smali.
# Here we only wire Home rendering/refresh into the proven status-bar lifecycle.
p = Path('.github/clean13d/StatusBarCustomizationBridge.java')
s = p.read_text()

old = '''        try {\n            applyClockPreference(activity);\n            final WeakReference<Activity> ref = new WeakReference<>(activity);\n'''
new = '''        try {\n            applyClockPreference(activity);\n            WeatherStatusBarBridge.applyToHome(activity);\n            final WeakReference<Activity> ref = new WeakReference<>(activity);\n'''
assert s.count(old) == 1, f'Home marker count={s.count(old)}'
s = s.replace(old, new, 1)

old = '''                        try {\n                            applyVisual(a);\n                        } catch (Throwable t) {\n                            Log.e(TAG, "CLEAN13D delayed visual apply failed", t);\n                        }\n'''
new = '''                        try {\n                            applyVisual(a);\n                            WeatherStatusBarBridge.applyToHome(a);\n                        } catch (Throwable t) {\n                            Log.e(TAG, "CLEAN13D delayed visual apply failed", t);\n                        }\n'''
assert s.count(old) == 1, f'delayed visual marker count={s.count(old)}'
s = s.replace(old, new, 1)

# Guard against accidentally re-embedding weather controls in Clock.
assert 'WeatherStatusBarBridge.addWeatherActions(fragment, list);' not in s
assert 'WeatherStatusBarBridge.handleWeatherAction(fragment, action)' not in s

p.write_text(s)
print('PASS: CLEAN13.1B weather removed from Clock page; Home rendering retained')
