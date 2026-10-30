#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean13_1/WeatherStatusBarBridge.java')
s = p.read_text()

# CLEAN13.1O is an isolated rebuild from the approved I source.
for token in [
    'CLEAN13.1I status order watcher attached',
    'CLEAN13.1I status geometry phase=',
    'CLEAN13.1G Weather parent uses native Projectivy cloud icon',
    'makeCheckedAction(fragment, ID_SHOW_PARENT, "Show weather"',
    'public static boolean handleWeatherParentAction(Object fragment, Object action)',
    'public static boolean handleWeatherPageAction(Object fragment, Object action)',
]:
    assert token in s, token

# The exact Projectivy DEX path has been audited:
# cb3.q0(yh3) -> hv1 builder -> cb3.z0(lu1) -> iv1 action.
# hv1 has no Context constructor; never instantiate it directly.
if 'import java.lang.reflect.Constructor;' not in s:
    s = s.replace('import java.lang.reflect.Field;\n',
                  'import java.lang.reflect.Constructor;\nimport java.lang.reflect.Field;\n', 1)

old_row = '''            list.add(makeCheckedAction(fragment, ID_SHOW_PARENT, "Show weather",
                    p.getBoolean(KEY_SHOW, true)));
'''
new_row = '''            boolean showWeatherChecked = p.getBoolean(KEY_SHOW, true);
            Object showWeatherAction;
            try {
                showWeatherAction = makeNativeCheckbox(fragment, ID_SHOW_PARENT,
                        "Show weather", showWeatherChecked);
                Log.i(TAG, "CLEAN13.1O Show weather native action="
                        + showWeatherAction.getClass().getName()
                        + " checked=" + showWeatherChecked);
            } catch (Throwable t) {
                Log.e(TAG, "CLEAN13.1O native checkbox failed; preserving G row", t);
                showWeatherAction = makeCheckedAction(fragment, ID_SHOW_PARENT,
                        "Show weather", showWeatherChecked);
                Log.i(TAG, "CLEAN13.1O Show weather fallback action="
                        + showWeatherAction.getClass().getName());
            }
            list.add(showWeatherAction);
'''
assert s.count(old_row) == 1, f'Show Weather row anchor={s.count(old_row)}'
s = s.replace(old_row, new_row, 1)

# Directly call the audited instance method on cb3. Do not search the pg class
# hierarchy for q0, and do not guess an hv1 constructor.
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

        // q0 selects the real hv1 checkbox builder from the Boolean value type.
        // Zero feature flags intentionally select its Boolean/checkbox branch.
        Object model = modelConstructor.newInstance(
                "clean13_1_weather_show", Boolean.valueOf(checked),
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

        Log.i(TAG, "CLEAN13.1O native checkbox built builder=hv1 action=iv1 checked=" + actual);
        return action;
    }

'''
s = s.replace(anchor, helper + anchor, 1)

# Replace the old fragment-identity-only routing. Weather action IDs are unique,
# so either the page callback or the parent callback can safely route them to
# the currently active Weather page.
old_parent = r'''    public static boolean handleWeatherParentAction(Object fragment, Object action) {
        try {
            if (fragment == null || action == null) return false;
            long id = ((Number) field(action, "a").get(action)).longValue();
            if (id != ID_WEATHER_PAGE) return false;
            openWeatherPage(fragment);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN13.1B open Weather page failed", t);
            return true;
        }
    }
'''
new_parent = r'''    public static boolean handleWeatherParentAction(Object fragment, Object action) {
        try {
            if (fragment == null || action == null) return false;
            long id = ((Number) field(action, "a").get(action)).longValue();
            if (id == ID_WEATHER_PAGE) {
                openWeatherPage(fragment);
                return true;
            }
            if (!isWeatherControlId(id)) return false;

            Object page = ACTIVE_WEATHER_PAGE.get();
            if (page == null) {
                Log.w(TAG, "CLEAN13.1O parent received Weather child action without active page id=" + id);
                return false;
            }
            Log.i(TAG, "CLEAN13.1O parent callback id=" + id
                    + " page=" + page.getClass().getName()
                    + " action=" + action.getClass().getName());
            return handleWeatherAction(page, action);
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN13.1O parent callback failed", t);
            return false;
        }
    }
'''
assert s.count(old_parent) == 1, f'parent handler anchor={s.count(old_parent)}'
s = s.replace(old_parent, new_parent, 1)

old_page = r'''    public static boolean handleWeatherPageAction(Object fragment, Object action) {
        if (!isWeatherPage(fragment)) return false;
        return handleWeatherAction(fragment, action);
    }
'''
new_page = r'''    private static boolean isWeatherControlId(long id) {
        return id == ID_SHOW_PARENT || id == ID_SHOW_ON || id == ID_SHOW_OFF
                || id == ID_LOCATION || id == ID_UNIT_C || id == ID_UNIT_F
                || id == ID_REFRESH || id == ID_ATTRIBUTION;
    }

    public static boolean handleWeatherPageAction(Object fragment, Object action) {
        try {
            if (fragment == null || action == null) return false;
            long id = ((Number) field(action, "a").get(action)).longValue();
            if (!isWeatherControlId(id)) return false;

            if (isWeatherPage(fragment)) {
                ACTIVE_WEATHER_PAGE = new WeakReference<>(fragment);
            }
            Object page = ACTIVE_WEATHER_PAGE.get();
            if (page == null) page = fragment;

            Log.i(TAG, "CLEAN13.1O page callback id=" + id
                    + " fragment=" + fragment.getClass().getName()
                    + " target=" + page.getClass().getName()
                    + " action=" + action.getClass().getName());
            return handleWeatherAction(page, action);
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN13.1O page callback failed", t);
            return false;
        }
    }
'''
assert s.count(old_page) == 1, f'page handler anchor={s.count(old_page)}'
s = s.replace(old_page, new_page, 1)

# The native iv1 action inherits mu1.c(), which returns the checked bit.
# Read that exact method; use the legacy toggle only for the non-native G fallback.
old_handler = r'''            if (id == ID_SHOW_PARENT || id == ID_SHOW_ON || id == ID_SHOW_OFF) {
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
new_handler = r'''            if (id == ID_SHOW_PARENT || id == ID_SHOW_ON || id == ID_SHOW_OFF) {
                boolean nativeCheckbox = "iv1".equals(action.getClass().getName());
                boolean show;
                if (nativeCheckbox) {
                    Class<?> mu1 = Class.forName("mu1");
                    Method checked = mu1.getDeclaredMethod("c");
                    checked.setAccessible(true);
                    show = ((Boolean) checked.invoke(action)).booleanValue();
                } else {
                    show = id == ID_SHOW_PARENT ? !p.getBoolean(KEY_SHOW, true) : id == ID_SHOW_ON;
                }

                boolean saved = p.edit().putBoolean(KEY_SHOW, show).commit();
                refreshMenu(fragment);
                renderLastHome();
                Activity a = activityOf(fragment);
                if (a != null) render(a);
                Log.i(TAG, "CLEAN13.1O show weather checked=" + show
                        + " native=" + nativeCheckbox + " saved=" + saved
                        + " action=" + action.getClass().getName());
                return true;
            }
'''
assert s.count(old_handler) == 1, f'Show Weather handler anchor={s.count(old_handler)}'
s = s.replace(old_handler, new_handler, 1)

# Keep the proven I/G visual path frozen and reject all failed constructor/dropdown paths.
for token in [
    'CLEAN13.1O native checkbox built builder=hv1 action=iv1 checked=',
    'CLEAN13.1O page callback id=',
    'CLEAN13.1O parent callback id=',
    'CLEAN13.1O show weather checked=',
    'CLEAN13.1I status order watcher attached',
    'CLEAN13.1G Weather parent uses native Projectivy cloud icon',
    'makeNativeCheckbox(fragment, ID_SHOW_PARENT',
]:
    assert token in s, token

for forbidden in [
    'getDeclaredConstructor(Context.class)',
    'Class.forName("hv1")',
    'findMethod(fragment.getClass(), "q0", preferenceClass)',
    'makeDropdown(fragment, ID_SHOW_PARENT, "Show weather"',
    'CLEAN13.1H native checkbox Show weather=',
    'CLEAN13.1J Show weather selector=',
    'CLEAN13.1K show weather choice=',
    'CLEAN13.1L route action id=',
]:
    assert forbidden not in s, forbidden

p.write_text(s)
print('PASS: CLEAN13.1O uses audited cb3.q0(yh3) -> hv1 -> z0 -> iv1 and routes both callbacks')
