#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean13_1/WeatherStatusBarBridge.java')
s = p.read_text()

# Start from J: approved I/G Weather visuals/order + visible native On/Off selector.
for token in [
    'CLEAN13.1J Show weather selector=',
    'makeDropdown(fragment, ID_SHOW_PARENT, "Show weather"',
    'CLEAN13.1I status order watcher attached',
    'CLEAN13.1G Weather parent uses native Projectivy cloud icon',
    'public static boolean handleWeatherPageAction(Object fragment, Object action)',
]:
    assert token in s, token

# J inherited CLEAN13.1D direct-parent toggle semantics. Once Show weather is a
# dropdown again, parent must only open the selector; only the child IDs may write.
old = '''            if (id == ID_SHOW_PARENT || id == ID_SHOW_ON || id == ID_SHOW_OFF) {\n                boolean show = id == ID_SHOW_PARENT ? !p.getBoolean(KEY_SHOW, true) : id == ID_SHOW_ON;\n                p.edit().putBoolean(KEY_SHOW, show).commit();\n'''
new = '''            if (id == ID_SHOW_ON || id == ID_SHOW_OFF) {\n                boolean show = id == ID_SHOW_ON;\n                p.edit().putBoolean(KEY_SHOW, show).commit();\n'''
assert s.count(old) == 1, f'legacy parent-toggle handler count={s.count(old)}'
s = s.replace(old, new, 1)
s = s.replace('Log.i(TAG, "CLEAN13.1D show weather=" + show);',
              'Log.i(TAG, "CLEAN13.1L show weather choice=" + show);', 1)

# The old page router required the callback fragment itself to be the exact pg
# instance stored in WEATHER_PAGES. GuidedActions nested dropdown child callbacks
# can arrive through another fragment instance, so their unique IDs were discarded.
# Route only Weather-owned IDs, and when the callback fragment is not the marked
# Weather page, send the action back through ACTIVE_WEATHER_PAGE.
old = '''    public static boolean handleWeatherPageAction(Object fragment, Object action) {\n        if (!isWeatherPage(fragment)) return false;\n        return handleWeatherAction(fragment, action);\n    }\n'''
new = '''    public static boolean handleWeatherPageAction(Object fragment, Object action) {\n        try {\n            if (action == null) return false;\n            long id = ((Number) field(action, "a").get(action)).longValue();\n            boolean owned = id == ID_SHOW_PARENT || id == ID_SHOW_ON || id == ID_SHOW_OFF\n                    || id == ID_LOCATION || id == ID_UNIT_PARENT || id == ID_UNIT_C || id == ID_UNIT_F\n                    || id == ID_REFRESH || id == ID_ATTRIBUTION;\n            if (!owned) return false;\n\n            boolean callbackMarked = isWeatherPage(fragment);\n            Object target = callbackMarked ? fragment : ACTIVE_WEATHER_PAGE.get();\n            if (target == null) target = fragment;\n\n            Log.i(TAG, "CLEAN13.1L route action id=" + id\n                    + " callbackMarked=" + callbackMarked\n                    + " usingActivePage=" + (target != fragment));\n            return handleWeatherAction(target, action);\n        } catch (Throwable t) {\n            Log.e(TAG, "CLEAN13.1L route action failed", t);\n            return false;\n        }\n    }\n'''
assert s.count(old) == 1, f'weather page router count={s.count(old)}'
s = s.replace(old, new, 1)

# Hard guards: frozen good UI/order stays; K global icon experiment is not present.
assert 'if (id == ID_SHOW_PARENT || id == ID_SHOW_ON || id == ID_SHOW_OFF)' not in s
assert 'id == ID_SHOW_PARENT ? !p.getBoolean(KEY_SHOW, true)' not in s
assert 'if (id == ID_SHOW_ON || id == ID_SHOW_OFF)' in s
assert 'CLEAN13.1L show weather choice=' in s
assert 'CLEAN13.1L route action id=' in s
assert 'CLEAN13.1I status order watcher attached' in s
assert 'CLEAN13.1G Weather parent uses native Projectivy cloud icon' in s
assert 'Class.forName("hv1")' not in s

p.write_text(s)
print('PASS: CLEAN13.1L keeps J/I UI and routes nested Weather child actions to the active Weather page')
