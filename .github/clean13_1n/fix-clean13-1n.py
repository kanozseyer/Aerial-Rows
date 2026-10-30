#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean13_1/WeatherStatusBarBridge.java')
s = p.read_text()

# N starts from the approved CLEAN13.1I baseline only.
for token in [
    'CLEAN13.1I status order watcher attached',
    'CLEAN13.1I status geometry phase=',
    'CLEAN13.1G Weather parent uses native Projectivy cloud icon',
    'makeCheckedAction(fragment, ID_SHOW_PARENT, "Show weather"',
]:
    assert token in s, token

# Use Projectivy's own Boolean-preference builder (cb3.q0(yh3)).
# If that native route unexpectedly fails, preserve the known-visible G row
# rather than aborting population of the entire Weather settings page.
old = '''            list.add(makeCheckedAction(fragment, ID_SHOW_PARENT, "Show weather",
                    p.getBoolean(KEY_SHOW, true)));
'''
new = '''            boolean showWeatherChecked = p.getBoolean(KEY_SHOW, true);
            Object showWeatherAction;
            try {
                showWeatherAction = makeNativeCheckbox(fragment, ID_SHOW_PARENT,
                        "Show weather", showWeatherChecked);
            } catch (Throwable t) {
                Log.e(TAG, "CLEAN13.1N native checkbox failed; using G checked-row fallback", t);
                showWeatherAction = makeCheckedAction(fragment, ID_SHOW_PARENT,
                        "Show weather", showWeatherChecked);
            }
            list.add(showWeatherAction);
            Log.i(TAG, "CLEAN13.1N Show weather action=" + showWeatherAction.getClass().getName());
'''
assert s.count(old) == 1, f'Show weather row count={s.count(old)}'
s = s.replace(old, new, 1)

# yh3 is Projectivy's native preference model. cb3.q0 performs the real DEX
# construction of hv1 using lu1(Context); no hv1 constructor is reflected here.
if 'import java.lang.reflect.Constructor;' not in s:
    s = s.replace('import java.lang.reflect.Field;\n',
                  'import java.lang.reflect.Constructor;\nimport java.lang.reflect.Field;\n', 1)

anchor = '    private static Object makeCheckedAction(Object fragment, long id, String title,\n'
assert s.count(anchor) == 1
helper = r'''    private static Object makeNativeCheckbox(Object fragment, long id,
                                             String title, boolean checked) throws Exception {
        Class<?> preferenceClass = Class.forName("yh3");
        Class<?> categoryClass = Class.forName("ta3");
        Constructor<?> modelConstructor = preferenceClass.getDeclaredConstructor(
                String.class, Object.class, int.class, int.class, categoryClass, int.class,
                Object.class, Object.class, Object.class);
        modelConstructor.setAccessible(true);
        Object model = modelConstructor.newInstance(
                "com.spocky.weather.show", Boolean.valueOf(checked),
                0, 0, null, 0, null, null, null);

        // This is the native Projectivy path used by cb3 to build Boolean settings.
        // Its DEX implementation allocates hv1 and invokes lu1(Context) correctly.
        Method nativeBuilderMethod = findMethod(fragment.getClass(), "q0", preferenceClass);
        Object builder = nativeBuilderMethod.invoke(fragment, model);
        if (builder == null) {
            throw new IllegalStateException("Projectivy cb3.q0 returned a null checkbox builder");
        }
        if (!"hv1".equals(builder.getClass().getName())) {
            throw new IllegalStateException("Expected native hv1 builder, got "
                    + builder.getClass().getName());
        }

        Class<?> cb3 = Class.forName("cb3");
        Class<?> lu1 = Class.forName("lu1");
        Method setRow = cb3.getDeclaredMethod("r0", long.class, CharSequence.class,
                Drawable.class, boolean.class, lu1);
        setRow.setAccessible(true);
        setRow.invoke(null, id, title, null, false, builder);
        field(builder, "l").setBoolean(builder, checked);

        Method toAction = cb3.getDeclaredMethod("z0", lu1);
        toAction.setAccessible(true);
        Object action = toAction.invoke(null, builder);
        Log.i(TAG, "CLEAN13.1N native checkbox built builder="
                + builder.getClass().getName() + " action=" + action.getClass().getName()
                + " checked=" + checked);
        return action;
    }

'''
s = s.replace(anchor, helper + anchor, 1)

# Read the native checkbox's checked state directly, just like the native
# Projectivy Boolean preference action. Do not toggle based on a stale pref.
old = '''            if (id == ID_SHOW_PARENT || id == ID_SHOW_ON || id == ID_SHOW_OFF) {
                boolean show = id == ID_SHOW_PARENT ? !p.getBoolean(KEY_SHOW, true) : id == ID_SHOW_ON;
                p.edit().putBoolean(KEY_SHOW, show).commit();
                refreshMenu(fragment);
                renderLastHome();
                Activity a = activityOf(fragment);
                if (a != null) render(a);
                Log.i(TAG, "CLEAN13.1D show weather=" + show);
                return true;
            }
'''
new = '''            if (id == ID_SHOW_PARENT) {
                boolean nativeCheckbox = "iv1".equals(action.getClass().getName());
                boolean show;
                if (nativeCheckbox) {
                    Method checked = findMethod(action.getClass(), "c");
                    show = ((Boolean) checked.invoke(action)).booleanValue();
                } else {
                    // G's safe visible fallback is a normal action row, not an hv1 checkbox.
                    show = !p.getBoolean(KEY_SHOW, true);
                }
                p.edit().putBoolean(KEY_SHOW, show).commit();
                refreshMenu(fragment);
                renderLastHome();
                Activity a = activityOf(fragment);
                if (a != null) render(a);
                Log.i(TAG, "CLEAN13.1N show weather checkbox=" + show
                        + " native=" + nativeCheckbox);
                return true;
            }
'''
assert s.count(old) == 1, f'legacy Show weather handler count={s.count(old)}'
s = s.replace(old, new, 1)

# Hard isolation guards: preserve G/I and exclude all failed H/J/K/L mechanisms.
for token in [
    'makeNativeCheckbox(fragment, ID_SHOW_PARENT',
    'preferenceClass.getDeclaredConstructor(',
    'findMethod(fragment.getClass(), "q0", preferenceClass)',
    'CLEAN13.1N native checkbox built builder=',
    'CLEAN13.1N show weather checkbox=',
    'CLEAN13.1I status order watcher attached',
    'CLEAN13.1G Weather parent uses native Projectivy cloud icon',
]:
    assert token in s, token

for forbidden in [
    'getDeclaredConstructor(Context.class)',
    'Class.forName("hv1")',
    'CLEAN13.1H Weather ordered between status icons and date/clock',
    'CLEAN13.1J Show weather selector=',
    'CLEAN13.1K show weather choice=',
    'CLEAN13.1L route action id=',
    'makeDropdown(fragment, ID_SHOW_PARENT, "Show weather"',
]:
    assert forbidden not in s, forbidden

p.write_text(s)
print('PASS: CLEAN13.1N uses Projectivy cb3.q0(yh3) native Boolean builder; G fallback preserves page')
