#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean13_1/WeatherStatusBarBridge.java')
s = p.read_text()

# CLEAN13.1J starts from the user-approved CLEAN13.1I state.
# Freeze the proven Weather category/icon/background and status-bar ordering.
for token in [
    'CLEAN13.1I status order watcher attached',
    'CLEAN13.1I status geometry phase=',
    'CLEAN13.1G Weather parent uses native Projectivy cloud icon',
    'getDeclaredField("m4")',
    'nativeCloud, 16',
    'CLEAN13.1B Weather sibling added to Status Bar',
]:
    assert token in s, token

# G changed the original native On/Off dropdown into makeCheckedAction(), but that
# did not render a visible selection on Shield. Restore the same proven Projectivy
# dropdown pattern already used by CLEAN13D2 Show date: parent row shows On/Off,
# selecting a child writes the existing preference and refreshes the page.
old = '''            list.add(makeCheckedAction(fragment, ID_SHOW_PARENT, "Show weather",\n                    p.getBoolean(KEY_SHOW, true)));\n'''
new = '''            list.add(makeDropdown(fragment, ID_SHOW_PARENT, "Show weather",\n                    p.getBoolean(KEY_SHOW, true) ? "On" : "Off",\n                    new long[]{ID_SHOW_ON, ID_SHOW_OFF},\n                    new String[]{"On", "Off"}));\n            Log.i(TAG, "CLEAN13.1J Show weather selector="\n                    + (p.getBoolean(KEY_SHOW, true) ? "On" : "Off"));\n'''
assert s.count(old) == 1, f'Show weather checked-row count={s.count(old)}'
s = s.replace(old, new, 1)

# CLEAN13.1D intentionally broadened the original handler so the former direct
# parent row can toggle as well. Keep that proven handler untouched. The child IDs
# used by the restored dropdown are still handled by this same block.
for token in [
    'ID_SHOW_ON',
    'ID_SHOW_OFF',
    'if (id == ID_SHOW_PARENT || id == ID_SHOW_ON || id == ID_SHOW_OFF)',
    'boolean show = id == ID_SHOW_PARENT ? !p.getBoolean(KEY_SHOW, true) : id == ID_SHOW_ON',
    'p.edit().putBoolean(KEY_SHOW, show).commit()',
    'refreshMenu(fragment)',
    'renderLastHome()',
    'CLEAN13.1D show weather=',
]:
    assert token in s, token

# H's rejected hv1 checkbox experiment must never reappear.
assert 'Class.forName("hv1")' not in s
assert 'CLEAN13.1H native checkbox Show weather=' not in s

# J must no longer call G's checked-action path for the Show weather row.
assert 'makeCheckedAction(fragment, ID_SHOW_PARENT' not in s
assert 'CLEAN13.1J Show weather selector=' in s

p.write_text(s)
print('PASS: CLEAN13.1J preserves approved I and restores native On/Off Show weather selector')
