#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean13_1/WeatherStatusBarBridge.java')
s = p.read_text()

# H keeps G's now-proven native parent Weather icon/background completely untouched.
assert 'getDeclaredField("m4")' in s
assert 't0.invoke(null, fragment, id, title, nativeCloud, 16)' in s

# Reuse the exact native checkbox construction already proven by frozen CLEAN13's
# Show date row. G's lu1.h(boolean) changed action presentation but did not render
# a visible checked indicator on-device.
if 'import java.lang.reflect.Constructor;' not in s:
    s = s.replace('import java.lang.reflect.Field;\n',
                  'import java.lang.reflect.Constructor;\nimport java.lang.reflect.Field;\n', 1)

old = '''            list.add(makeCheckedAction(fragment, ID_SHOW_PARENT, "Show weather",\n                    p.getBoolean(KEY_SHOW, true)));\n'''
new = '''            list.add(makeCheckbox(fragment, context, ID_SHOW_PARENT, "Show weather",\n                    p.getBoolean(KEY_SHOW, true)));\n'''
assert s.count(old) == 1, f'show checked row count={s.count(old)}'
s = s.replace(old, new, 1)

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
        Method r0 = cb3.getDeclaredMethod("r0", long.class, CharSequence.class,
                Drawable.class, boolean.class, lu1);
        r0.setAccessible(true);
        r0.invoke(null, id, title, null, false, builder);
        findMethod(builder.getClass(), "b", int.class).invoke(builder, -1);
        field(builder, "l").setBoolean(builder, checked);
        Method z0 = cb3.getDeclaredMethod("z0", lu1);
        z0.setAccessible(true);
        Log.i(TAG, "CLEAN13.1H native checkbox Show weather=" + checked);
        return z0.invoke(null, builder);
    }

'''
s = s[:start] + checkbox + s[end:]

# Weather was only positioned when first attached. Frozen CLEAN13 can subsequently
# move the clock/date view, leaving Weather on the wrong side of the status icons.
# Keep Weather physically between the status-icon container and date/clock view,
# both immediately and once more after the native status-bar pass settles.
anchor = '    private static void render(Activity activity) {\n'
assert s.count(anchor) == 1
helper = r'''    private static void placeWeatherBetweenStatusAndClock(ViewGroup parent, View icons,
                                                          View clock, View badge) {
        if (parent == null || icons == null || clock == null || badge == null) return;
        if (icons.getParent() != parent || clock.getParent() != parent || badge.getParent() != parent) return;
        int iconsIndex = parent.indexOfChild(icons);
        int clockIndex = parent.indexOfChild(clock);
        int badgeIndex = parent.indexOfChild(badge);
        if (iconsIndex < 0 || clockIndex < 0 || badgeIndex < 0) return;
        boolean alreadyBetween = (iconsIndex < badgeIndex && badgeIndex < clockIndex)
                || (clockIndex < badgeIndex && badgeIndex < iconsIndex);
        if (alreadyBetween) return;

        ViewGroup.LayoutParams lp = badge.getLayoutParams();
        parent.removeView(badge);
        iconsIndex = parent.indexOfChild(icons);
        clockIndex = parent.indexOfChild(clock);
        int target = Math.min(iconsIndex, clockIndex) + 1;
        parent.addView(badge, Math.min(target, parent.getChildCount()), lp);
        parent.requestLayout();
        parent.invalidate();
        Log.i(TAG, "CLEAN13.1H Weather ordered between status icons and date/clock");
    }

'''
s = s.replace(anchor, helper + anchor, 1)

# Insert the ordering pass just before render() reads Weather prefs. Using positions
# rather than an exact multi-line anchor makes this resilient to the earlier D/G
# source transformations while still guaranteeing we are inside render().
render_start = s.index('    private static void render(Activity activity) {')
prefs_pos = s.index('            SharedPreferences p = prefs(activity);', render_start)
order_block = r'''            placeWeatherBetweenStatusAndClock(parent, icons, clock, badge);
            final ViewGroup orderParent = parent;
            final View orderIcons = icons;
            final View orderClock = clock;
            final View orderBadge = badge;
            activity.getWindow().getDecorView().postDelayed(new Runnable() {
                @Override public void run() {
                    placeWeatherBetweenStatusAndClock(orderParent, orderIcons, orderClock, orderBadge);
                }
            }, 100L);

'''
s = s[:prefs_pos] + order_block + s[prefs_pos:]

for token in [
    'CLEAN13.1H native checkbox Show weather=',
    'CLEAN13.1H Weather ordered between status icons and date/clock',
    'makeCheckbox(fragment, context, ID_SHOW_PARENT',
    'getDeclaredField("m4")',
    'nativeCloud, 16',
]:
    assert token in s, token

assert 'makeCheckedAction(' not in s

p.write_text(s)
print('PASS: CLEAN13.1H preserves G icon and fixes checkbox indicator + status order')
