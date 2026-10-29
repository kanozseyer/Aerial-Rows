#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean13_1/WeatherStatusBarBridge.java')
s = p.read_text()

# CLEAN13.1F — final Weather UI polish after Shield feedback.
# Keep weather/network/order/idle behavior from 13.1E; only:
#   1) use Projectivy's proven native checkbox action for Show weather
#   2) give the custom Weather category icon its own rounded background tile

# Native checkbox builder uses reflection Constructor, matching the proven Show date path.
if 'import java.lang.reflect.Constructor;\n' not in s:
    s = s.replace('import java.lang.reflect.Field;\n',
                  'import java.lang.reflect.Constructor;\nimport java.lang.reflect.Field;\n', 1)

# Replace subtitle-based On/Off row with the same native checkbox pattern used by Show date.
old = '''            list.add(makeAction(fragment, ID_SHOW_PARENT, "Show weather",\n                    p.getBoolean(KEY_SHOW, true) ? "On" : "Off"));\n'''
new = '''            list.add(makeWeatherCheckbox(fragment, context, ID_SHOW_PARENT, "Show weather",\n                    p.getBoolean(KEY_SHOW, true)));\n            Log.i(TAG, "CLEAN13.1F native Show weather checkbox added");\n'''
assert s.count(old) == 1
s = s.replace(old, new, 1)

# Read the actual checked state from the native action instead of synthesizing a subtitle.
old = '''            if (id == ID_SHOW_PARENT || id == ID_SHOW_ON || id == ID_SHOW_OFF) {\n                boolean show = id == ID_SHOW_PARENT ? !p.getBoolean(KEY_SHOW, true) : id == ID_SHOW_ON;\n                p.edit().putBoolean(KEY_SHOW, show).commit();\n                updateLiveActionSubtitle(fragment, action, show ? "On" : "Off");\n                renderLastHome();\n                Activity a = activityOf(fragment);\n                if (a != null) render(a);\n                Log.i(TAG, "CLEAN13.1E show weather=" + show);\n                return true;\n            }\n'''
new = '''            if (id == ID_SHOW_PARENT || id == ID_SHOW_ON || id == ID_SHOW_OFF) {\n                boolean show;\n                if (id == ID_SHOW_PARENT) {\n                    try {\n                        show = ((Boolean) findMethod(action.getClass(), "c").invoke(action)).booleanValue();\n                    } catch (Throwable ignored) {\n                        show = !p.getBoolean(KEY_SHOW, true);\n                    }\n                } else {\n                    show = id == ID_SHOW_ON;\n                }\n                p.edit().putBoolean(KEY_SHOW, show).commit();\n                renderLastHome();\n                Activity a = activityOf(fragment);\n                if (a != null) render(a);\n                Log.i(TAG, "CLEAN13.1F show weather=" + show);\n                return true;\n            }\n'''
assert s.count(old) == 1
s = s.replace(old, new, 1)

# Insert a native checkbox builder copied from the already runtime-proven Show date path.
anchor = '    private static Object makeActionWithIcon(Object fragment, Context context, long id,\n'
assert s.count(anchor) == 1
checkbox = '''    private static Object makeWeatherCheckbox(Object fragment, Context context, long id,\n                                              String title, boolean checked) throws Exception {\n        Class<?> cb3 = Class.forName("cb3");\n        Class<?> lu1 = Class.forName("lu1");\n        Class<?> hv1 = Class.forName("hv1");\n        Constructor<?> ctor = hv1.getDeclaredConstructor(Context.class);\n        ctor.setAccessible(true);\n        Object builder = ctor.newInstance(context);\n        findMethod(builder.getClass(), "h", boolean.class).invoke(builder, false);\n        Method r0 = cb3.getDeclaredMethod("r0", long.class, CharSequence.class, Drawable.class, boolean.class, lu1);\n        r0.setAccessible(true);\n        r0.invoke(null, id, title, null, false, builder);\n        findMethod(builder.getClass(), "b", int.class).invoke(builder, -1);\n        field(builder, "l").setBoolean(builder, checked);\n        Method z0 = cb3.getDeclaredMethod("z0", lu1);\n        z0.setAccessible(true);\n        return z0.invoke(null, builder);\n    }\n\n'''
s = s.replace(anchor, checkbox + anchor, 1)

# Give Weather's custom settings icon an opaque rounded tile so it visually matches
# the other Status Bar category icons that include their own backgrounds.
old = '''    private static final class WeatherMenuDrawable extends Drawable {\n        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);\n        private final Path cloud = new Path();\n        private final int size;\n\n        WeatherMenuDrawable(Context context) {\n            size = dp(context, 32);\n            p.setColor(0xffffffff);\n'''
new = '''    private static final class WeatherMenuDrawable extends Drawable {\n        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);\n        private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);\n        private final Path cloud = new Path();\n        private final int size;\n\n        WeatherMenuDrawable(Context context) {\n            size = dp(context, 32);\n            bg.setColor(0xff424242);\n            bg.setStyle(Paint.Style.FILL);\n            p.setColor(0xffffffff);\n'''
assert s.count(old) == 1
s = s.replace(old, new, 1)

old = '''        @Override public void draw(Canvas c) {\n            RectF b = new RectF(getBounds());\n            float s = Math.min(b.width(), b.height());\n            if (s <= 0f) return;\n            float x = b.centerX() - s / 2f;\n            float y = b.centerY() - s / 2f;\n'''
new = '''        @Override public void draw(Canvas c) {\n            RectF outer = new RectF(getBounds());\n            float outerSize = Math.min(outer.width(), outer.height());\n            if (outerSize <= 0f) return;\n            float radius = outerSize * 0.22f;\n            c.drawRoundRect(outer, radius, radius, bg);\n\n            float inset = outerSize * 0.18f;\n            RectF b = new RectF(outer.left + inset, outer.top + inset,\n                    outer.right - inset, outer.bottom - inset);\n            float s = Math.min(b.width(), b.height());\n            float x = b.centerX() - s / 2f;\n            float y = b.centerY() - s / 2f;\n'''
assert s.count(old) == 1
s = s.replace(old, new, 1)

old = '''        @Override public void setAlpha(int alpha) { p.setAlpha(alpha); invalidateSelf(); }\n        @Override public void setColorFilter(android.graphics.ColorFilter cf) { p.setColorFilter(cf); invalidateSelf(); }\n'''
new = '''        @Override public void setAlpha(int alpha) {\n            p.setAlpha(alpha); bg.setAlpha(alpha); invalidateSelf();\n        }\n        @Override public void setColorFilter(android.graphics.ColorFilter cf) {\n            p.setColorFilter(cf); bg.setColorFilter(cf); invalidateSelf();\n        }\n'''
assert s.count(old) == 1
s = s.replace(old, new, 1)

# Version the runtime-facing logs for this candidate while preserving behavior.
s = s.replace('CLEAN13.1E Weather menu icon applied=', 'CLEAN13.1F Weather menu icon applied=')
s = s.replace('CLEAN13.1E status order=settings/status icons > weather > date > clock',
              'CLEAN13.1F status order=settings/status icons > weather > date > clock')
s = s.replace('CLEAN13.1E status order=settings/status icons > date > clock (weather hidden)',
              'CLEAN13.1F status order=settings/status icons > date > clock (weather hidden)')

for token in [
    'CLEAN13.1F native Show weather checkbox added',
    'CLEAN13.1F show weather=',
    'CLEAN13.1F Weather menu icon applied=',
    'bg.setColor(0xff424242)',
    'makeWeatherCheckbox',
]:
    assert token in s, token

p.write_text(s)
print('PASS: CLEAN13.1F native weather toggle + rounded menu-icon background prepared')
