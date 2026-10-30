#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean13_1/WeatherStatusBarBridge.java')
s = p.read_text()

# M starts from the user-approved CLEAN13.1I baseline only.
for token in [
    'CLEAN13.1I status order watcher attached',
    'CLEAN13.1I status geometry phase=',
    'CLEAN13.1G Weather parent uses native Projectivy cloud icon',
    'makeCheckedAction(fragment, ID_SHOW_PARENT, "Show weather"',
]:
    assert token in s, token

# Copy the exact native checkbox presentation already used by the proven Show date row.
if 'import java.lang.reflect.Constructor;' not in s:
    s = s.replace('import java.lang.reflect.Field;\n',
                  'import java.lang.reflect.Constructor;\nimport java.lang.reflect.Field;\n', 1)

old = '''            list.add(makeCheckedAction(fragment, ID_SHOW_PARENT, "Show weather",\n                    p.getBoolean(KEY_SHOW, true)));\n'''
new = '''            list.add(makeCheckbox(fragment, context, ID_SHOW_PARENT, "Show weather",\n                    p.getBoolean(KEY_SHOW, true)));\n            Log.i(TAG, "CLEAN13.1M Show weather checkbox built="\n                    + p.getBoolean(KEY_SHOW, true));\n'''
assert s.count(old) == 1, f'old Show weather row count={s.count(old)}'
s = s.replace(old, new, 1)

# Replace G's checked-action helper with the exact helper pattern used by
# StatusBarCustomizationBridge.makeCheckbox (the working Show date row).
start = s.index('    private static Object makeCheckedAction(')
end = s.index('    private static Object makeAction(', start)
checkbox = r'''    private static Object makeCheckbox(Object fragment, Context context, long id,
                                       String title, boolean checked) throws Exception {
        Class<?> cb3 = Class.forName("cb3");
        Class<?> lu1 = Class.forName("lu1");
        Class<?> hv1 = Class.forName("hv1");
        Constructor<?> ctor = hv1.getDeclaredConstructor(Context.class);
        ctor.setAccessible(true);
        Object builder = ctor.newInstance(context);
        findMethod(builder.getClass(), "h", boolean.class).invoke(builder, false);
        Method r0 = cb3.getDeclaredMethod("r0", long.class, CharSequence.class, Drawable.class,
                boolean.class, lu1);
        r0.setAccessible(true);
        r0.invoke(null, id, title, null, false, builder);
        findMethod(builder.getClass(), "b", int.class).invoke(builder, -1);
        field(builder, "l").setBoolean(builder, checked);
        Method z0 = cb3.getDeclaredMethod("z0", lu1);
        z0.setAccessible(true);
        return z0.invoke(null, builder);
    }

'''
s = s[:start] + checkbox + s[end:]

# The checkbox is a direct action, so read the action's native checked state exactly
# like StatusBarCustomizationBridge.handleClockDateAction does for Show date.
old = '''            if (id == ID_SHOW_PARENT || id == ID_SHOW_ON || id == ID_SHOW_OFF) {\n                boolean show = id == ID_SHOW_PARENT ? !p.getBoolean(KEY_SHOW, true) : id == ID_SHOW_ON;\n                p.edit().putBoolean(KEY_SHOW, show).commit();\n                refreshMenu(fragment);\n                renderLastHome();\n                Activity a = activityOf(fragment);\n                if (a != null) render(a);\n                Log.i(TAG, "CLEAN13.1D show weather=" + show);\n                return true;\n            }\n'''
new = '''            if (id == ID_SHOW_PARENT) {\n                Method checked = findMethod(action.getClass(), "c");\n                boolean show = ((Boolean) checked.invoke(action)).booleanValue();\n                p.edit().putBoolean(KEY_SHOW, show).commit();\n                refreshMenu(fragment);\n                renderLastHome();\n                Activity a = activityOf(fragment);\n                if (a != null) render(a);\n                Log.i(TAG, "CLEAN13.1M show weather checkbox=" + show);\n                return true;\n            }\n'''
assert s.count(old) == 1, f'legacy Show weather handler count={s.count(old)}'
s = s.replace(old, new, 1)

# Hard isolation guards: keep I/G category+order and do NOT inherit H's ordering experiment,
# J/K/L dropdown/routing experiments, or K global icon changes.
for token in [
    'makeCheckbox(fragment, context, ID_SHOW_PARENT, "Show weather"',
    'Class.forName("hv1")',
    'getDeclaredMethod("r0", long.class, CharSequence.class, Drawable.class',
    'findMethod(action.getClass(), "c")',
    'CLEAN13.1M show weather checkbox=',
    'CLEAN13.1I status order watcher attached',
    'CLEAN13.1G Weather parent uses native Projectivy cloud icon',
]:
    assert token in s, token

for forbidden in [
    'CLEAN13.1H Weather ordered between status icons and date/clock',
    'CLEAN13.1J Show weather selector=',
    'CLEAN13.1K show weather choice=',
    'CLEAN13.1L route action id=',
    'makeDropdown(fragment, ID_SHOW_PARENT, "Show weather"',
]:
    assert forbidden not in s, forbidden

p.write_text(s)
print('PASS: CLEAN13.1M isolates exact native Show-date checkbox path for Show weather')
