#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean13_1/WeatherStatusBarBridge.java')
s = p.read_text()

# CLEAN13.1G: pg.e0 must complete Projectivy's native page initialization first.
# The smali hook calls this method immediately before pg.e0's final return.
old = r'''    public static boolean populateWeatherPage(Object fragment, List list) {
        if (!isWeatherPage(fragment)) return false;
        ACTIVE_WEATHER_PAGE = new WeakReference<>(fragment);
        addWeatherActions(fragment, list);
        Log.i(TAG, "CLEAN13.1D Weather native page populated");
        return true;
    }
'''
new = r'''    public static void finishPopulateWeatherPage(Object fragment, List list) {
        if (!isWeatherPage(fragment) || list == null) return;
        ACTIVE_WEATHER_PAGE = new WeakReference<>(fragment);
        list.clear();
        addWeatherActions(fragment, list);
        Log.i(TAG, "CLEAN13.1G Weather page populated after native init");
    }
'''
assert s.count(old) == 1, f'populate method count={s.count(old)}'
s = s.replace(old, new, 1)

# Use Projectivy's own checked action presentation for Show weather. lu1.h(boolean)
# is the native builder API that selects the checked-row visual state.
old = r'''            list.add(makeAction(fragment, ID_SHOW_PARENT, "Show weather",
                    p.getBoolean(KEY_SHOW, true) ? "On" : "Off"));
'''
new = r'''            list.add(makeCheckedAction(fragment, ID_SHOW_PARENT, "Show weather",
                    p.getBoolean(KEY_SHOW, true)));
'''
assert s.count(old) == 1, f'show-row count={s.count(old)}'
s = s.replace(old, new, 1)

# Replace the previous custom Weather drawable route. Projectivy's own Material
# icon enum contains gmo_cloud at ta3.m4. J0 resolves it to the native h22 icon,
# and t0 mask 16 preserves the supplied drawable (mask 28 discarded it via bit 0x4).
start = s.index('    private static Object makeActionWithIcon(')
end = s.index('    private static Object makeAction(', start)
native_icon = r'''    private static Object makeActionWithIcon(Object fragment, Context context, long id,
                                             String title, String subtitle) throws Exception {
        Class<?> cb3 = Class.forName("cb3");
        Class<?> lu1 = Class.forName("lu1");
        Class<?> s02 = Class.forName("s02");
        Class<?> ta3 = Class.forName("ta3");
        Method j0 = cb3.getDeclaredMethod("J0", cb3, s02);
        j0.setAccessible(true);
        Method t0 = cb3.getDeclaredMethod("t0", cb3, long.class, CharSequence.class, Drawable.class, int.class);
        t0.setAccessible(true);
        Method z0 = cb3.getDeclaredMethod("z0", lu1);
        z0.setAccessible(true);
        Field cloudField = ta3.getDeclaredField("m4");
        cloudField.setAccessible(true);
        Object cloudToken = cloudField.get(null);
        Drawable nativeCloud = (Drawable) j0.invoke(null, fragment, cloudToken);
        Object action = t0.invoke(null, fragment, id, title, nativeCloud, 16);
        if (subtitle != null && subtitle.length() != 0) field(action, "d").set(action, subtitle);
        Log.i(TAG, "CLEAN13.1G Weather parent uses native Projectivy cloud icon");
        return z0.invoke(null, action);
    }

'''
s = s[:start] + native_icon + s[end:]

# Add a checked native action helper beside the existing action helper.
anchor = '    private static Object makeAction(Object fragment, long id, String title, String subtitle) throws Exception {\n'
assert s.count(anchor) == 1
checked = r'''    private static Object makeCheckedAction(Object fragment, long id, String title,
                                            boolean checked) throws Exception {
        Class<?> cb3 = Class.forName("cb3");
        Class<?> lu1 = Class.forName("lu1");
        Method t0 = cb3.getDeclaredMethod("t0", cb3, long.class, CharSequence.class, Drawable.class, int.class);
        t0.setAccessible(true);
        Method z0 = cb3.getDeclaredMethod("z0", lu1);
        z0.setAccessible(true);
        Object action = t0.invoke(null, fragment, id, title, null, 28);
        Method setChecked = lu1.getDeclaredMethod("h", boolean.class);
        setChecked.setAccessible(true);
        setChecked.invoke(action, checked);
        Log.i(TAG, "CLEAN13.1G native checked Show weather=" + checked);
        return z0.invoke(null, action);
    }

'''
s = s.replace(anchor, checked + anchor, 1)

for token in [
    'finishPopulateWeatherPage',
    'CLEAN13.1G Weather page populated after native init',
    'CLEAN13.1G Weather parent uses native Projectivy cloud icon',
    'CLEAN13.1G native checked Show weather=',
    'getDeclaredField("m4")',
    'setChecked.invoke(action, checked)',
]:
    assert token in s, token

# Hard guards against the two failed F mechanisms.
assert 'new WeatherMenuDrawable(context), 28' not in s
assert 'populateWeatherPage(Object fragment, List list)' not in s

p.write_text(s)
print('PASS: CLEAN13.1G bridge uses post-init Weather population + native icon/check state')
