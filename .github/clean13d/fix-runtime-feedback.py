#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean13d/StatusBarCustomizationBridge.java')
s = p.read_text()

old = '    private static final long ID_SHOW_DATE = 0x13D00200L;\n'
new = old + '    private static final long ID_SHOW_DATE_ON = 0x13D00201L;\n    private static final long ID_SHOW_DATE_OFF = 0x13D00202L;\n'
assert old in s and 'ID_SHOW_DATE_ON' not in s
s = s.replace(old, new, 1)

old = '''            list.add(makeCheckbox(fragment, context, ID_SHOW_DATE, "Show date",\n                    p.getBoolean(KEY_SHOW_DATE, true)));\n'''
new = '''            list.add(makeDropdown(fragment, ID_SHOW_DATE, "Show date",\n                    p.getBoolean(KEY_SHOW_DATE, true) ? "On" : "Off",\n                    new long[]{ID_SHOW_DATE_ON, ID_SHOW_DATE_OFF},\n                    new String[]{"On", "Off"}));\n'''
assert old in s
s = s.replace(old, new, 1)

old = '''            if (id == ID_SHOW_DATE) {\n                Method checked = findMethod(action.getClass(), "c");\n                e.putBoolean(KEY_SHOW_DATE, ((Boolean) checked.invoke(action)).booleanValue());\n            } else if (id == ID_CLOCK_SYSTEM) e.putString(KEY_CLOCK, CLOCK_SYSTEM);\n'''
new = '''            if (id == ID_SHOW_DATE_ON) e.putBoolean(KEY_SHOW_DATE, true);\n            else if (id == ID_SHOW_DATE_OFF) e.putBoolean(KEY_SHOW_DATE, false);\n            else if (id == ID_CLOCK_SYSTEM) e.putString(KEY_CLOCK, CLOCK_SYSTEM);\n'''
assert old in s
s = s.replace(old, new, 1)

# The user's desired normal presentation is one continuous Projectivy-style pill.
# Keep the other modes selectable, but make unified the default when no explicit
# CLEAN13D background preference has been stored yet.
s = s.replace('getString(KEY_BACKGROUND, BG_COMBINED)', 'getString(KEY_BACKGROUND, BG_UNIFIED)')

old = '''    private static String backgroundLabel(SharedPreferences p) {\n        String v = p.getString(KEY_BACKGROUND, BG_UNIFIED);\n        if (BG_NONE.equals(v)) return "None";\n        if (BG_SEPARATE.equals(v)) return "Separate";\n        if (BG_UNIFIED.equals(v)) return "Combined with status icons";\n        return "Combined";\n    }\n'''
new = '''    private static String backgroundLabel(SharedPreferences p) {\n        String v = p.getString(KEY_BACKGROUND, BG_UNIFIED);\n        if (BG_NONE.equals(v)) return "None";\n        if (BG_SEPARATE.equals(v)) return "Separate";\n        if (BG_COMBINED.equals(v)) return "Combined";\n        return "Combined with status icons";\n    }\n'''
assert old in s
s = s.replace(old, new, 1)

# Add an unambiguous runtime marker so Shield logs can distinguish this corrected
# build from the first CLEAN13D test APK.
s = s.replace('Log.i(TAG, "CLEAN13D native clock/date actions added");',
              'Log.i(TAG, "CLEAN13D2 native clock/date actions added; unified default");', 1)

p.write_text(s)
print('PASS: CLEAN13D2 runtime feedback fix prepared — date/background menus reachable; unified pill default')
