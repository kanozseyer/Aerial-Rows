#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean13d/StatusBarCustomizationBridge.java')
s = p.read_text()

# The CLEAN13D2 preparation stage changes Show Date into an On/Off dropdown.
# Replace that exact post-build row with the same audited Projectivy Boolean
# checkbox builder used by CLEAN13.1O Show Weather.
for token in [
    'list.add(makeDropdown(fragment, ID_SHOW_DATE, "Show date",',
    'if (id == ID_SHOW_DATE_ON) e.putBoolean(KEY_SHOW_DATE, true);',
    'private static Object makeCheckbox(Object fragment, Context context, long id, String title, boolean checked)',
]:
    assert token in s, token

if 'import java.lang.reflect.Constructor;' not in s:
    s = s.replace('import java.lang.reflect.Field;\n',
                  'import java.lang.reflect.Constructor;\nimport java.lang.reflect.Field;\n', 1)

old_constants = '''    private static final long ID_SHOW_DATE_ON = 0x13D00201L;
    private static final long ID_SHOW_DATE_OFF = 0x13D00202L;
'''
assert s.count(old_constants) == 1
s = s.replace(old_constants, '', 1)

old_row = '''            list.add(makeDropdown(fragment, ID_SHOW_DATE, "Show date",
                    p.getBoolean(KEY_SHOW_DATE, true) ? "On" : "Off",
                    new long[]{ID_SHOW_DATE_ON, ID_SHOW_DATE_OFF},
                    new String[]{"On", "Off"}));
'''
new_row = '''            boolean showDateChecked = p.getBoolean(KEY_SHOW_DATE, true);
            Object showDateAction;
            try {
                showDateAction = makeNativeCheckbox(fragment, ID_SHOW_DATE,
                        "Show date", showDateChecked);
                Log.i(TAG, "CLEAN13.1P Show date native action="
                        + showDateAction.getClass().getName()
                        + " checked=" + showDateChecked);
            } catch (Throwable t) {
                Log.e(TAG, "CLEAN13.1P native checkbox failed; preserving checked-row fallback", t);
                showDateAction = makeCheckedAction(fragment, ID_SHOW_DATE,
                        "Show date", showDateChecked);
                Log.i(TAG, "CLEAN13.1P Show date fallback action="
                        + showDateAction.getClass().getName());
            }
            list.add(showDateAction);
'''
assert s.count(old_row) == 1, f'Show Date row anchor={s.count(old_row)}'
s = s.replace(old_row, new_row, 1)

old_handler = '''            if (id == ID_SHOW_DATE_ON) e.putBoolean(KEY_SHOW_DATE, true);
            else if (id == ID_SHOW_DATE_OFF) e.putBoolean(KEY_SHOW_DATE, false);
            else if (id == ID_CLOCK_SYSTEM) e.putString(KEY_CLOCK, CLOCK_SYSTEM);
'''
new_handler = '''            boolean showDateChecked = p.getBoolean(KEY_SHOW_DATE, true);
            if (id == ID_SHOW_DATE) {
                Method checked = findMethod(action.getClass(), "c");
                showDateChecked = ((Boolean) checked.invoke(action)).booleanValue();
                e.putBoolean(KEY_SHOW_DATE, showDateChecked);
            } else if (id == ID_CLOCK_SYSTEM) e.putString(KEY_CLOCK, CLOCK_SYSTEM);
'''
assert s.count(old_handler) == 1, f'Show Date handler anchor={s.count(old_handler)}'
s = s.replace(old_handler, new_handler, 1)

old_apply = '''            e.apply();
            applyClockPreference(context);
'''
new_apply = '''            e.apply();
            if (id == ID_SHOW_DATE) {
                Log.i(TAG, "CLEAN13.1P Show date checked=" + showDateChecked
                        + " saved=" + p.getBoolean(KEY_SHOW_DATE, true)
                        + " action=" + action.getClass().getName());
            }
            applyClockPreference(context);
'''
assert s.count(old_apply) == 1, f'apply anchor={s.count(old_apply)}'
s = s.replace(old_apply, new_apply, 1)

old_helper = '''    private static Object makeCheckbox(Object fragment, Context context, long id, String title, boolean checked) throws Exception {
        Class<?> cb3 = Class.forName("cb3");
        Class<?> lu1 = Class.forName("lu1");
        Class<?> hv1 = Class.forName("hv1");
        Constructor<?> ctor = hv1.getDeclaredConstructor(Context.class);
        ctor.setAccessible(true);
        Object builder = ctor.newInstance(context);
        findMethod(builder.getClass(), "h", boolean.class).invoke(builder, false);
        Method r0 = cb3.getDeclaredMethod("r0", long.class, CharSequence.class, Drawable.class, boolean.class, lu1);
        r0.setAccessible(true);
        r0.invoke(null, id, title, null, false, builder);
        findMethod(builder.getClass(), "b", int.class).invoke(builder, -1);
        field(builder, "l").setBoolean(builder, checked);
        Method z0 = cb3.getDeclaredMethod("z0", lu1);
        z0.setAccessible(true);
        return z0.invoke(null, builder);
    }
'''
new_helper = r'''    private static Object makeNativeCheckbox(Object fragment, long id,
                                             String title, boolean checked) throws Exception {
        Class<?> preferenceClass = Class.forName("yh3");
        Class<?> categoryClass = Class.forName("ta3");
        Constructor<?> modelConstructor = preferenceClass.getDeclaredConstructor(
                String.class, Object.class, int.class, int.class, categoryClass, int.class,
                Object.class, Object.class, Object.class);
        modelConstructor.setAccessible(true);
        Object model = modelConstructor.newInstance(
                "clean13d_show_date", Boolean.valueOf(checked),
                0, 0, null, 0, null, null, null);

        Class<?> cb3 = Class.forName("cb3");
        Class<?> lu1 = Class.forName("lu1");
        Method nativeBuilderMethod = cb3.getDeclaredMethod("q0", preferenceClass);
        nativeBuilderMethod.setAccessible(true);
        Object builder = nativeBuilderMethod.invoke(fragment, model);
        if (builder == null || !"hv1".equals(builder.getClass().getName())) {
            throw new IllegalStateException("cb3.q0 did not return hv1; got "
                    + (builder == null ? "null" : builder.getClass().getName()));
        }

        Method setRow = cb3.getDeclaredMethod("r0", long.class, CharSequence.class,
                Drawable.class, boolean.class, lu1);
        setRow.setAccessible(true);
        setRow.invoke(null, id, title, null, false, builder);

        Field checkedField = builder.getClass().getDeclaredField("l");
        checkedField.setAccessible(true);
        checkedField.setBoolean(builder, checked);

        Method toAction = cb3.getDeclaredMethod("z0", lu1);
        toAction.setAccessible(true);
        Object action = toAction.invoke(null, builder);
        if (action == null || !"iv1".equals(action.getClass().getName())) {
            throw new IllegalStateException("cb3.z0 did not return iv1; got "
                    + (action == null ? "null" : action.getClass().getName()));
        }

        Class<?> mu1 = Class.forName("mu1");
        Method readChecked = mu1.getDeclaredMethod("c");
        readChecked.setAccessible(true);
        boolean actual = ((Boolean) readChecked.invoke(action)).booleanValue();
        if (actual != checked) {
            throw new IllegalStateException("native checkbox state mismatch expected="
                    + checked + " actual=" + actual);
        }
        Log.i(TAG, "CLEAN13.1P native checkbox built builder=hv1 action=iv1 checked=" + actual);
        return action;
    }

    private static Object makeCheckedAction(Object fragment, long id, String title,
                                            boolean checked) throws Exception {
        Class<?> cb3 = Class.forName("cb3");
        Class<?> lu1 = Class.forName("lu1");
        Method t0 = cb3.getDeclaredMethod("t0", cb3, long.class,
                CharSequence.class, Drawable.class, int.class);
        t0.setAccessible(true);
        Method z0 = cb3.getDeclaredMethod("z0", lu1);
        z0.setAccessible(true);
        Object builder = t0.invoke(null, fragment, id, title, null, 28);
        Method setChecked = lu1.getDeclaredMethod("h", boolean.class);
        setChecked.setAccessible(true);
        setChecked.invoke(builder, checked);
        return z0.invoke(null, builder);
    }
'''
assert s.count(old_helper) == 1, f'old Show Date helper anchor={s.count(old_helper)}'
s = s.replace(old_helper, new_helper, 1)

for token in [
    'CLEAN13.1P native checkbox built builder=hv1 action=iv1 checked=',
    'CLEAN13.1P Show date native action=',
    'CLEAN13.1P Show date fallback action=',
    'CLEAN13.1P Show date checked=',
    'cb3.getDeclaredMethod("q0", preferenceClass)',
]:
    assert token in s, token

for forbidden in [
    'getDeclaredConstructor(Context.class)',
    'Class.forName("hv1")',
    'ID_SHOW_DATE_ON',
    'ID_SHOW_DATE_OFF',
    'private static Object makeCheckbox(',
]:
    assert forbidden not in s, forbidden

p.write_text(s)
print('PASS: CLEAN13.1P Show Date uses the same audited native checkbox factory as CLEAN13.1O Show Weather')
