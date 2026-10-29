#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean13_1/WeatherStatusBarBridge.java')
s = p.read_text()

# Imports needed for wrapped Activity resolution, page args, adaptive layout and weak state maps.
s = s.replace('import android.content.Context;\n', 'import android.content.Context;\nimport android.content.ContextWrapper;\n', 1)
s = s.replace('import android.os.Handler;\n', 'import android.os.Bundle;\nimport android.os.Handler;\n', 1)
s = s.replace('import android.view.ViewGroup;\n', 'import android.view.ViewGroup;\nimport android.view.ViewTreeObserver;\n', 1)
s = s.replace('import java.util.List;\n', 'import java.util.List;\nimport java.util.Map;\nimport java.util.WeakHashMap;\n', 1)

# A dedicated action ID on the native Status Bar parent (qg).
anchor = '    private static final long ID_ATTRIBUTION = 0x131500L;\n'
assert s.count(anchor) == 1
s = s.replace(anchor, anchor + '    private static final long ID_WEATHER_PAGE = 0x131000L;\n', 1)

anchor = '    private static final AtomicBoolean IN_FLIGHT = new AtomicBoolean(false);\n'
assert s.count(anchor) == 1
s = s.replace(anchor, anchor + '''    private static final Map<Object, Boolean> WEATHER_PAGES = new WeakHashMap<>();\n    private static final Map<ViewGroup, AdaptivePillWatcher> PILL_WATCHERS = new WeakHashMap<>();\n''', 1)

# Add the native Status Bar sibling entry and reuse Projectivy's pg page as a weather-only page.
anchor = '    public static void addWeatherActions(Object fragment, List list) {\n'
assert s.count(anchor) == 1
methods = r'''    public static void addWeatherParentAction(Object fragment, List list) {
        try {
            if (fragment == null || list == null) return;
            Context context = contextOf(fragment);
            if (context == null) return;
            SharedPreferences p = prefs(context);
            String query = p.getString(KEY_QUERY, "").trim();
            String resolved = p.getString(KEY_RESOLVED, "").trim();
            String subtitle;
            if (p.contains(KEY_TEMP_C)) {
                double tempC = Double.parseDouble(p.getString(KEY_TEMP_C, "0"));
                boolean f = UNIT_F.equals(p.getString(KEY_UNIT, UNIT_C));
                long display = Math.round(f ? tempC * 9.0 / 5.0 + 32.0 : tempC);
                String place = resolved.length() != 0 ? resolved : query;
                subtitle = display + (f ? "°F" : "°C") + (place.length() != 0 ? " • " + place : "");
            } else if (resolved.length() != 0) subtitle = resolved;
            else if (query.length() != 0) subtitle = query;
            else subtitle = "Configure weather";

            Object weather = makeAction(fragment, ID_WEATHER_PAGE, "Weather", subtitle);
            int insert = list.size();
            for (int i = 0; i < list.size(); i++) {
                try {
                    Object a = list.get(i);
                    long id = ((Number) field(a, "a").get(a)).longValue();
                    if (id == 20L) { insert = i + 1; break; }
                } catch (Throwable ignored) {}
            }
            list.add(Math.min(insert, list.size()), weather);
            Log.i(TAG, "CLEAN13.1B Weather sibling added to Status Bar");
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN13.1B add Weather parent failed", t);
        }
    }

    public static boolean handleWeatherParentAction(Object fragment, Object action) {
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

    public static boolean populateWeatherPage(Object fragment, List list) {
        if (!isWeatherPage(fragment)) return false;
        addWeatherActions(fragment, list);
        Log.i(TAG, "CLEAN13.1B Weather native page populated");
        return true;
    }

    public static boolean handleWeatherPageAction(Object fragment, Object action) {
        if (!isWeatherPage(fragment)) return false;
        return handleWeatherAction(fragment, action);
    }

    public static boolean isWeatherPage(Object fragment) {
        if (fragment == null) return false;
        synchronized (WEATHER_PAGES) {
            return Boolean.TRUE.equals(WEATHER_PAGES.get(fragment));
        }
    }

    private static void openWeatherPage(Object fragment) throws Exception {
        Class<?> pg = Class.forName("pg");
        java.lang.reflect.Constructor<?> ctor = pg.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object child = ctor.newInstance();
        synchronized (WEATHER_PAGES) { WEATHER_PAGES.put(child, Boolean.TRUE); }

        Bundle args = null;
        try {
            Object existing = field(fragment, "z").get(fragment);
            if (existing instanceof Bundle) args = new Bundle((Bundle) existing);
        } catch (Throwable ignored) {}
        if (args == null) args = new Bundle();
        findMethod(child.getClass(), "U", Bundle.class).invoke(child, args);
        Method nav = findCompatibleOneArgMethod(fragment.getClass(), "u0", child.getClass());
        nav.invoke(fragment, child);
        Log.i(TAG, "CLEAN13.1B Weather native page opened");
    }

'''
s = s.replace(anchor, methods + anchor, 1)

# Log a missing Activity instead of silently doing nothing; activityOf below now unwraps ContextWrappers.
old = '''    private static void openLocationDialog(final Object fragment) {\n        final Activity activity = activityOf(fragment);\n        if (activity == null || activity.isFinishing()) return;\n'''
new = '''    private static void openLocationDialog(final Object fragment) {\n        final Activity activity = activityOf(fragment);\n        if (activity == null || activity.isFinishing()) {\n            Log.e(TAG, "CLEAN13.1B location dialog failed: Activity unavailable");\n            return;\n        }\n        Log.i(TAG, "CLEAN13.1B location dialog opened");\n'''
assert s.count(old) == 1
s = s.replace(old, new, 1)

# Put status content in the exact requested order: status/settings icons -> weather -> date+clock.
old = '''                int index = Math.min(parent.indexOfChild(clock) + 1, parent.getChildCount());\n'''
assert s.count(old) == 1
s = s.replace(old, '''                int index = Math.min(parent.indexOfChild(icons) + 1, parent.getChildCount());\n''', 1)

anchor = '''            SharedPreferences p = prefs(activity);\n'''
assert s.count(anchor) >= 1
# Only replace the occurrence inside render by anchoring on the preceding badge log block.
needle = '''                Log.i(TAG, "CLEAN13.1 weather badge attached");\n            }\n\n            SharedPreferences p = prefs(activity);\n'''
replacement = '''                Log.i(TAG, "CLEAN13.1 weather badge attached");\n            }\n\n            enforceStatusOrder(parent, icons, badge, clock);\n            installAdaptivePillWatcher(parent, icons);\n\n            SharedPreferences p = prefs(activity);\n'''
assert s.count(needle) == 1
s = s.replace(needle, replacement, 1)

# Helpers for exact order and idle-mode pill collapse/restore.
anchor = '    private static void applyBadgeBackground(Context context, WeatherBadgeView badge, ViewGroup icons) {\n'
assert s.count(anchor) == 1
helpers = r'''    private static void enforceStatusOrder(ViewGroup parent, ViewGroup icons, View badge, View clock) {
        moveChild(parent, icons, 0);
        moveChild(parent, badge, Math.min(1, parent.getChildCount() - 1));
        moveChild(parent, clock, Math.min(2, parent.getChildCount() - 1));
        parent.requestLayout();
        parent.invalidate();
        Log.i(TAG, "CLEAN13.1B status order=settings/status icons > weather > date > clock");
    }

    private static void moveChild(ViewGroup parent, View child, int index) {
        int current = parent.indexOfChild(child);
        if (current < 0 || current == index) return;
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        parent.removeView(child);
        parent.addView(child, Math.max(0, Math.min(index, parent.getChildCount())), lp);
    }

    private static void installAdaptivePillWatcher(ViewGroup parent, ViewGroup icons) {
        synchronized (PILL_WATCHERS) {
            if (PILL_WATCHERS.containsKey(parent)) return;
            AdaptivePillWatcher watcher = new AdaptivePillWatcher(parent, icons);
            PILL_WATCHERS.put(parent, watcher);
            parent.getViewTreeObserver().addOnPreDrawListener(watcher);
            Log.i(TAG, "CLEAN13.1B idle pill watcher attached");
        }
    }

'''
s = s.replace(anchor, helpers + anchor, 1)

# Resolve Activity through ContextThemeWrapper/ContextWrapper, which Projectivy uses on TV settings pages.
old = '''    private static Activity activityOf(Object fragment) {\n        Context c = contextOf(fragment);\n        return c instanceof Activity ? (Activity) c : null;\n    }\n'''
new = '''    private static Activity activityOf(Object fragment) {\n        Context c = contextOf(fragment);\n        for (int i = 0; i < 12 && c != null; i++) {\n            if (c instanceof Activity) return (Activity) c;\n            if (!(c instanceof ContextWrapper)) break;\n            Context next = ((ContextWrapper) c).getBaseContext();\n            if (next == c) break;\n            c = next;\n        }\n        try {\n            Class<?> k = fragment.getClass();\n            while (k != null) {\n                for (Field f : k.getDeclaredFields()) {\n                    f.setAccessible(true);\n                    Object v = f.get(fragment);\n                    if (v instanceof Activity) return (Activity) v;\n                    if (v instanceof Context) {\n                        Context x = (Context) v;\n                        for (int i = 0; i < 8 && x != null; i++) {\n                            if (x instanceof Activity) return (Activity) x;\n                            if (!(x instanceof ContextWrapper)) break;\n                            Context next = ((ContextWrapper) x).getBaseContext();\n                            if (next == x) break;\n                            x = next;\n                        }\n                    }\n                }\n                k = k.getSuperclass();\n            }\n        } catch (Throwable ignored) {}\n        return null;\n    }\n'''
assert s.count(old) == 1
s = s.replace(old, new, 1)

# Reflection helper for inherited u0(Lcb3;) without compile-time access to obfuscated classes.
anchor = '''    private static Method findMethod(Class<?> type, String name, Class<?>... params) throws Exception {\n'''
assert s.count(anchor) == 1
compat = r'''    private static Method findCompatibleOneArgMethod(Class<?> type, String name, Class<?> argType) throws Exception {
        Class<?> c = type;
        while (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getName().equals(name) && p.length == 1 && p[0].isAssignableFrom(argType)) {
                    m.setAccessible(true);
                    return m;
                }
            }
            c = c.getSuperclass();
        }
        throw new NoSuchMethodException(name + " compatible with " + argType.getName());
    }

'''
s = s.replace(anchor, compat + anchor, 1)

# Adaptive unified pill: if Projectivy fades/hides the Settings/status icons in idle mode,
# collapse that container to width 0 so the parent pill shrinks around Weather + Date + Clock.
anchor = '    private static final class WeatherBadgeView extends View {\n'
assert s.count(anchor) == 1
watcher = r'''    private static final class AdaptivePillWatcher implements ViewTreeObserver.OnPreDrawListener {
        private final WeakReference<ViewGroup> parentRef;
        private final WeakReference<ViewGroup> iconsRef;
        private int restoreWidth;
        private boolean collapsed;

        AdaptivePillWatcher(ViewGroup parent, ViewGroup icons) {
            parentRef = new WeakReference<>(parent);
            iconsRef = new WeakReference<>(icons);
            ViewGroup.LayoutParams lp = icons.getLayoutParams();
            restoreWidth = lp != null && lp.width != 0 ? lp.width : ViewGroup.LayoutParams.WRAP_CONTENT;
        }

        @Override public boolean onPreDraw() {
            ViewGroup parent = parentRef.get();
            ViewGroup icons = iconsRef.get();
            if (parent == null || icons == null) return true;
            boolean unified = "unified".equals(parent.getContext()
                    .getSharedPreferences(CLOCK_PREFS, Context.MODE_PRIVATE)
                    .getString("background_mode", "unified"));
            boolean active = icons.getVisibility() == View.VISIBLE && icons.getAlpha() > 0.04f && hasVisibleIcon(icons);
            boolean shouldCollapse = unified && !active;
            ViewGroup.LayoutParams lp = icons.getLayoutParams();
            if (lp == null) return true;

            if (!collapsed && !shouldCollapse && lp.width != 0) restoreWidth = lp.width;
            if (shouldCollapse != collapsed) {
                if (shouldCollapse) {
                    if (lp.width != 0) restoreWidth = lp.width;
                    lp.width = 0;
                } else {
                    lp.width = restoreWidth == 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : restoreWidth;
                }
                icons.setLayoutParams(lp);
                collapsed = shouldCollapse;
                parent.requestLayout();
                parent.invalidate();
                Log.i(TAG, "CLEAN13.1B idle pill iconsCollapsed=" + collapsed);
            }
            return true;
        }

        private boolean hasVisibleIcon(ViewGroup icons) {
            if (icons.getChildCount() == 0) return icons.getAlpha() > 0.04f;
            for (int i = 0; i < icons.getChildCount(); i++) {
                View child = icons.getChildAt(i);
                if (child != null && child.getVisibility() == View.VISIBLE && child.getAlpha() > 0.04f) return true;
            }
            return false;
        }
    }

'''
s = s.replace(anchor, watcher + anchor, 1)

for token in [
    'CLEAN13.1B Weather sibling added to Status Bar',
    'CLEAN13.1B Weather native page populated',
    'CLEAN13.1B location dialog opened',
    'CLEAN13.1B status order=settings/status icons > weather > date > clock',
    'CLEAN13.1B idle pill iconsCollapsed=',
    'findCompatibleOneArgMethod',
    'ContextWrapper',
]:
    assert token in s, token

p.write_text(s)
print('PASS: CLEAN13.1B Weather page, location fix, requested order and adaptive idle pill prepared')
