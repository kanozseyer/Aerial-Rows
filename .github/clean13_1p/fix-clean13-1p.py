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


# CLEAN13.1P also fixes the manual Weather refresh path. The previous handler
# required the settings page's Context to be an Activity, which is not guaranteed
# for Projectivy's settings fragment. Start the request from its Context instead.
weather_path = Path('.github/clean13_1/WeatherStatusBarBridge.java')
w = weather_path.read_text()

for token in [
    'private static final String KEY_FETCHED = "fetched_at";',
    'list.add(makeAction(fragment, ID_REFRESH, "Refresh weather", lastUpdatedLabel(p)));',
    'if (id == ID_REFRESH) {',
    'private static void maybeRefresh(final Activity activity, final boolean force) {',
    'private static double[] geocode(Context context, String query) throws Exception {',
]:
    assert token in w, token

w = w.replace(
    '    private static final String KEY_FETCHED = "fetched_at";\n',
    '    private static final String KEY_FETCHED = "fetched_at";\n'
    '    private static final String KEY_REFRESHING = "refreshing";\n'
    '    private static final String KEY_REFRESH_ERROR = "refresh_error";\n',
    1)

old_refresh_row = '''            list.add(makeAction(fragment, ID_REFRESH, "Refresh weather", lastUpdatedLabel(p)));
'''
new_refresh_row = '''            String refreshLabel = p.getBoolean(KEY_REFRESHING, false)
                    ? "Updating..."
                    : (p.getBoolean(KEY_REFRESH_ERROR, false)
                    ? "Update failed - " + lastUpdatedLabel(p) : lastUpdatedLabel(p));
            list.add(makeAction(fragment, ID_REFRESH, "Refresh weather", refreshLabel));
'''
assert w.count(old_refresh_row) == 1, f'refresh row anchor={w.count(old_refresh_row)}'
w = w.replace(old_refresh_row, new_refresh_row, 1)

old_refresh_handler = '''            if (id == ID_REFRESH) {
                Activity a = activityOf(fragment);
                Log.i(TAG, "CLEAN13.1D manual refresh requested");
                if (a != null) {
                    maybeRefresh(a, true);
                    render(a);
                }
                refreshMenu(fragment);
                return true;
            }
'''
new_refresh_handler = '''            if (id == ID_REFRESH) {
                Activity a = activityOf(fragment);
                Log.i(TAG, "CLEAN13.1P manual refresh button clicked page="
                        + fragment.getClass().getName());
                startRefresh(context, fragment, a, true);
                if (a != null) render(a);
                Log.i(TAG, "CLEAN13.1P manual refresh dispatched context="
                        + context.getClass().getName()
                        + " activity=" + (a != null));
                return true;
            }
'''
assert w.count(old_refresh_handler) == 1, f'refresh handler anchor={w.count(old_refresh_handler)}'
w = w.replace(old_refresh_handler, new_refresh_handler, 1)

start = w.index('    private static void maybeRefresh(final Activity activity, final boolean force) {')
end = w.index('    private static double[] geocode(Context context, String query) throws Exception {', start)
old_method = w[start:end]
new_method = r'''    private static void maybeRefresh(final Activity activity, final boolean force) {
        if (activity == null) return;
        startRefresh(activity, null, activity, force);
    }

    private static void startRefresh(final Context context, final Object menuFragment,
                                     final Activity activity, final boolean force) {
        if (context == null) {
            Log.e(TAG, "CLEAN13.1P refresh has no Context force=" + force);
            return;
        }
        Context application = context.getApplicationContext();
        final Context app = application != null ? application : context;
        final SharedPreferences p = prefs(app);

        // Automatic Home refresh respects Show Weather. An explicit user refresh
        // always works, even when the badge itself is currently hidden.
        if (!force && !p.getBoolean(KEY_SHOW, true)) return;
        final String query = p.getString(KEY_QUERY, "").trim();
        if (query.length() == 0) {
            Log.w(TAG, "CLEAN13.1P refresh skipped: no location configured force=" + force);
            return;
        }

        long fetched = p.getLong(KEY_FETCHED, 0L);
        boolean haveCoords = p.contains(KEY_LAT) && p.contains(KEY_LON);
        if (!force && haveCoords && fetched > 0L
                && System.currentTimeMillis() - fetched < REFRESH_MS) return;

        if (!IN_FLIGHT.compareAndSet(false, true)) {
            Log.i(TAG, "CLEAN13.1P refresh already in flight force=" + force);
            if (menuFragment != null) refreshMenu(menuFragment);
            return;
        }

        p.edit().putBoolean(KEY_REFRESHING, true).putBoolean(KEY_REFRESH_ERROR, false).apply();
        if (menuFragment != null) refreshMenu(menuFragment);
        Log.i(TAG, "CLEAN13.1P refresh start force=" + force
                + " manual=" + (menuFragment != null) + " query=" + query);

        final WeakReference<Activity> activityRef = new WeakReference<>(activity);
        final WeakReference<Object> menuRef = new WeakReference<>(menuFragment);
        try {
            EXEC.execute(new Runnable() {
                @Override public void run() {
                    boolean success = false;
                    try {
                        SharedPreferences current = prefs(app);
                        if (!query.equals(current.getString(KEY_QUERY, "").trim())) {
                            throw new IOException("Weather location changed before refresh");
                        }

                        double lat;
                        double lon;
                        if (current.contains(KEY_LAT) && current.contains(KEY_LON)) {
                            lat = Double.parseDouble(current.getString(KEY_LAT, "0"));
                            lon = Double.parseDouble(current.getString(KEY_LON, "0"));
                        } else {
                            double[] coords = geocode(app, query);
                            if (coords == null) throw new IOException("No geocoding result for " + query);
                            lat = coords[0];
                            lon = coords[1];
                        }
                        fetchCurrent(app, query, lat, lon);
                        success = true;
                    } catch (Throwable t) {
                        Log.w(TAG, "CLEAN13.1P refresh failed force=" + force, t);
                    } finally {
                        IN_FLIGHT.set(false);
                        final boolean completed = success;
                        MAIN.post(new Runnable() {
                            @Override public void run() {
                                p.edit().putBoolean(KEY_REFRESHING, false)
                                        .putBoolean(KEY_REFRESH_ERROR, !completed).apply();

                                Object menu = menuRef.get();
                                if (menu != null) refreshMenu(menu);

                                Activity a = activityRef.get();
                                if (a != null && !a.isFinishing() && !a.isDestroyed()) render(a);
                                renderLastHome();
                                refreshActiveWeatherPage();

                                Log.i(TAG, "CLEAN13.1P refresh completed success=" + completed
                                        + " fetchedAt=" + p.getLong(KEY_FETCHED, 0L)
                                        + " manual=" + (menuRef.get() != null));
                            }
                        });
                    }
                }
            });
        } catch (Throwable t) {
            IN_FLIGHT.set(false);
            p.edit().putBoolean(KEY_REFRESHING, false).putBoolean(KEY_REFRESH_ERROR, true).apply();
            if (menuFragment != null) refreshMenu(menuFragment);
            Log.e(TAG, "CLEAN13.1P refresh executor rejected request", t);
        }
    }

'''
assert old_method.count('maybeRefresh') == 1
w = w[:start] + new_method + w[end:]

for token in [
    'CLEAN13.1P manual refresh button clicked',
    'CLEAN13.1P manual refresh dispatched context=',
    'CLEAN13.1P refresh start force=',
    'CLEAN13.1P refresh completed success=',
    'CLEAN13.1P refresh failed force=',
    'KEY_REFRESHING',
    'Update failed - ',
    'startRefresh(context, fragment, a, true)',
    'renderLastHome();',
    'refreshActiveWeatherPage();',
]:
    assert token in w, token

assert 'maybeRefresh(a, true);' not in w
weather_path.write_text(w)
print('PASS: CLEAN13.1P manual Weather refresh uses page Context, forces fetch, and refreshes its timestamp row')
