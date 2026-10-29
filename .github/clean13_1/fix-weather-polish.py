#!/usr/bin/env python3
from pathlib import Path

wp = Path('.github/clean13_1/WeatherStatusBarBridge.java')
ws = wp.read_text()

# 1) Show weather already toggles correctly in CLEAN13.1D. Update the live GuidedAction
# object itself and notify the Leanback action adapter so the visible On/Off subtitle
# changes immediately without leaving/re-entering the Weather page.
old = r'''                p.edit().putBoolean(KEY_SHOW, show).commit();
                refreshMenu(fragment);
                renderLastHome();
                Activity a = activityOf(fragment);
                if (a != null) render(a);
                Log.i(TAG, "CLEAN13.1D show weather=" + show);
'''
new = r'''                p.edit().putBoolean(KEY_SHOW, show).commit();
                updateLiveActionSubtitle(fragment, action, show ? "On" : "Off");
                renderLastHome();
                Activity a = activityOf(fragment);
                if (a != null) render(a);
                Log.i(TAG, "CLEAN13.1E show weather=" + show);
'''
assert ws.count(old) == 1
ws = ws.replace(old, new, 1)

# 2) Enforce two explicit orders. Weather ON = icons > weather > date/clock.
# Weather OFF = icons > date/clock, with the hidden weather view moved to the end so
# it cannot perturb child indices or be exposed by another status-bar refresh.
old = r'''    private static void enforceStatusOrder(ViewGroup parent, ViewGroup icons, View badge, View clock) {
        moveChild(parent, icons, 0);
        moveChild(parent, badge, Math.min(1, parent.getChildCount() - 1));
        moveChild(parent, clock, Math.min(2, parent.getChildCount() - 1));
        parent.requestLayout();
        parent.invalidate();
        Log.i(TAG, "CLEAN13.1B status order=settings/status icons > weather > date > clock");
    }
'''
new = r'''    private static void enforceStatusOrder(ViewGroup parent, ViewGroup icons, View badge, View clock) {
        SharedPreferences p = prefs(parent.getContext());
        boolean weatherVisible = p.getBoolean(KEY_SHOW, true)
                && p.getString(KEY_QUERY, "").trim().length() != 0;
        moveChild(parent, icons, 0);
        if (weatherVisible) {
            moveChild(parent, badge, Math.min(1, parent.getChildCount() - 1));
            moveChild(parent, clock, Math.min(2, parent.getChildCount() - 1));
        } else {
            moveChild(parent, clock, Math.min(1, parent.getChildCount() - 1));
            moveChild(parent, badge, Math.max(0, parent.getChildCount() - 1));
        }
        parent.requestLayout();
        parent.invalidate();
        Log.i(TAG, weatherVisible
                ? "CLEAN13.1E status order=settings/status icons > weather > date > clock"
                : "CLEAN13.1E status order=settings/status icons > date > clock (weather hidden)");
    }
'''
assert ws.count(old) == 1
ws = ws.replace(old, new, 1)

# 3) Force the parent Weather row icon onto the final GuidedAction as well as the
# Projectivy builder. This follows the native GuidedAction icon path and avoids relying
# solely on the obfuscated cb3.t0 builder retaining a custom Drawable.
old = r'''    private static Object makeActionWithIcon(Object fragment, Context context, long id,
                                             String title, String subtitle) throws Exception {
        Class<?> cb3 = Class.forName("cb3");
        Class<?> lu1 = Class.forName("lu1");
        Method t0 = cb3.getDeclaredMethod("t0", cb3, long.class, CharSequence.class, Drawable.class, int.class);
        t0.setAccessible(true);
        Method z0 = cb3.getDeclaredMethod("z0", lu1);
        z0.setAccessible(true);
        Object action = t0.invoke(null, fragment, id, title, new WeatherMenuDrawable(context), 28);
        if (subtitle != null && subtitle.length() != 0) field(action, "d").set(action, subtitle);
        return z0.invoke(null, action);
    }
'''
new = r'''    private static Object makeActionWithIcon(Object fragment, Context context, long id,
                                             String title, String subtitle) throws Exception {
        Class<?> cb3 = Class.forName("cb3");
        Class<?> lu1 = Class.forName("lu1");
        Method t0 = cb3.getDeclaredMethod("t0", cb3, long.class, CharSequence.class, Drawable.class, int.class);
        t0.setAccessible(true);
        Method z0 = cb3.getDeclaredMethod("z0", lu1);
        z0.setAccessible(true);
        Drawable icon = new WeatherMenuDrawable(context);
        int size = dp(context, 32);
        icon.setBounds(0, 0, size, size);
        Object builder = t0.invoke(null, fragment, id, title, icon, 28);
        if (subtitle != null && subtitle.length() != 0) field(builder, "d").set(builder, subtitle);
        Object action = z0.invoke(null, builder);
        boolean applied = false;
        try {
            findMethod(action.getClass(), "setIcon", Drawable.class).invoke(action, icon);
            applied = true;
        } catch (Throwable ignored) {}
        if (!applied) {
            Class<?> c = action.getClass();
            while (c != null && !applied) {
                for (Field f : c.getDeclaredFields()) {
                    if (Drawable.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        f.set(action, icon);
                        applied = true;
                        break;
                    }
                }
                c = c.getSuperclass();
            }
        }
        Log.i(TAG, "CLEAN13.1E Weather menu icon applied=" + applied);
        return action;
    }
'''
assert ws.count(old) == 1
ws = ws.replace(old, new, 1)

# 4) Make the custom drawable report density-correct intrinsic dimensions.
old = r'''    private static final class WeatherMenuDrawable extends Drawable {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path cloud = new Path();

        WeatherMenuDrawable(Context context) {
            p.setColor(0xffffffff);
'''
new = r'''    private static final class WeatherMenuDrawable extends Drawable {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path cloud = new Path();
        private final int size;

        WeatherMenuDrawable(Context context) {
            size = dp(context, 32);
            p.setColor(0xffffffff);
'''
assert ws.count(old) == 1
ws = ws.replace(old, new, 1)
ws = ws.replace('''        @Override public int getIntrinsicWidth() { return 48; }\n        @Override public int getIntrinsicHeight() { return 48; }\n''',
                '''        @Override public int getIntrinsicWidth() { return size; }\n        @Override public int getIntrinsicHeight() { return size; }\n''', 1)

# 5) Immediate visual row update helper. AndroidX GuidedStepSupportFragment keeps the
# public findActionPositionById/notifyActionChanged API names even though Projectivy's
# own subclasses are obfuscated. Fall back to the previous K0 refresh if unavailable.
anchor = '    private static void renderLastHome() {\n'
assert ws.count(anchor) == 1
helper = r'''    private static void updateLiveActionSubtitle(Object fragment, Object action, String subtitle) {
        try {
            field(action, "d").set(action, subtitle);
            try {
                Method find = findMethod(fragment.getClass(), "findActionPositionById", long.class);
                int position = ((Number) find.invoke(fragment, ID_SHOW_PARENT)).intValue();
                if (position >= 0) {
                    findMethod(fragment.getClass(), "notifyActionChanged", int.class).invoke(fragment, position);
                    Log.i(TAG, "CLEAN13.1E Show weather menu row updated=" + subtitle + " position=" + position);
                    return;
                }
            } catch (Throwable ignored) {}
            refreshMenu(fragment);
            Log.i(TAG, "CLEAN13.1E Show weather menu row fallback refresh=" + subtitle);
        } catch (Throwable t) {
            Log.w(TAG, "CLEAN13.1E Show weather menu row update failed", t);
            refreshMenu(fragment);
        }
    }

'''
ws = ws.replace(anchor, helper + anchor, 1)

for token in [
    'CLEAN13.1E show weather=',
    'CLEAN13.1E Weather menu icon applied=',
    'CLEAN13.1E Show weather menu row updated=',
    'CLEAN13.1E status order=settings/status icons > date > clock (weather hidden)',
]:
    assert token in ws, token

wp.write_text(ws)

# 6) The frozen CLEAN13 unified-pill implementation moves ClockView to index 0.
# CLEAN13.1 owns the richer status-bar ordering, so remove that one competing move.
# This is source-only for this weather candidate; frozen projectivy-clean13-final remains untouched.
sp = Path('.github/clean13d/StatusBarCustomizationBridge.java')
ss = sp.read_text()
old = '''            icons.setBackground(null);\n            moveChild(parent, clock, 0);\n            clock.setPadding(dp(activity, 10), state.clockPaddingTop, dp(activity, 10), state.clockPaddingBottom);\n'''
new = '''            icons.setBackground(null);\n            // CLEAN13.1E: preserve native/status-weather child order; WeatherStatusBarBridge owns ordering.\n            clock.setPadding(dp(activity, 10), state.clockPaddingTop, dp(activity, 10), state.clockPaddingBottom);\n'''
assert ss.count(old) == 1
ss = ss.replace(old, new, 1)
sp.write_text(ss)

print('PASS: CLEAN13.1E menu icon/live On-Off label/stable hidden-weather order prepared')
