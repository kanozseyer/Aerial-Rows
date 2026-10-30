#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean13_1/WeatherStatusBarBridge.java')
s = p.read_text()

# K starts after J has restored the native Projectivy On/Off dropdown.
for token in [
    'CLEAN13.1J Show weather selector=',
    'makeDropdown(fragment, ID_SHOW_PARENT, "Show weather"',
    'CLEAN13.1I status order watcher attached',
    'CLEAN13.1G Weather parent uses native Projectivy cloud icon',
]:
    assert token in s, token

# CLEAN13.1D had previously made the parent Show weather action a direct toggle.
# Once J turned the row back into a dropdown, that legacy parent handling caused
# an unwanted state flip as soon as the selector was opened. Parent must navigate
# only; only the On and Off child actions are allowed to write KEY_SHOW.
old = '''            if (id == ID_SHOW_PARENT || id == ID_SHOW_ON || id == ID_SHOW_OFF) {\n                boolean show = id == ID_SHOW_PARENT ? !p.getBoolean(KEY_SHOW, true) : id == ID_SHOW_ON;\n                p.edit().putBoolean(KEY_SHOW, show).commit();\n'''
new = '''            if (id == ID_SHOW_ON || id == ID_SHOW_OFF) {\n                boolean show = id == ID_SHOW_ON;\n                p.edit().putBoolean(KEY_SHOW, show).commit();\n'''
assert s.count(old) == 1, f'legacy parent-toggle handler count={s.count(old)}'
s = s.replace(old, new, 1)

# Runtime marker for the corrected semantics.
s = s.replace('Log.i(TAG, "CLEAN13.1D show weather=" + show);',
              'Log.i(TAG, "CLEAN13.1K show weather choice=" + show);', 1)

assert 'if (id == ID_SHOW_PARENT || id == ID_SHOW_ON || id == ID_SHOW_OFF)' not in s
assert 'id == ID_SHOW_PARENT ? !p.getBoolean(KEY_SHOW, true)' not in s
assert 'if (id == ID_SHOW_ON || id == ID_SHOW_OFF)' in s
assert 'CLEAN13.1K show weather choice=' in s

# Frozen paths must remain untouched.
assert 'CLEAN13.1I status order watcher attached' in s
assert 'CLEAN13.1G Weather parent uses native Projectivy cloud icon' in s
assert 'Class.forName("hv1")' not in s

p.write_text(s)
print('PASS: CLEAN13.1K Show weather parent only opens selector; child choice changes state')
