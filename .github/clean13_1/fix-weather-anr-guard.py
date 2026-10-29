#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean13_1/WeatherStatusBarBridge.java')
s = p.read_text()

# CLEAN13.1F ANR guard: do not request a new Home layout if the requested child order
# is already correct. This keeps the runtime-proven order/idle logic but removes
# unconditional layout churn from repeated render() passes.
old = '''    private static void enforceStatusOrder(ViewGroup parent, ViewGroup icons, View badge, View clock) {\n        SharedPreferences p = prefs(parent.getContext());\n        boolean weatherVisible = p.getBoolean(KEY_SHOW, true)\n                && p.getString(KEY_QUERY, "").trim().length() != 0;\n        moveChild(parent, icons, 0);\n        if (weatherVisible) {\n            moveChild(parent, badge, Math.min(1, parent.getChildCount() - 1));\n            moveChild(parent, clock, Math.min(2, parent.getChildCount() - 1));\n        } else {\n            moveChild(parent, clock, Math.min(1, parent.getChildCount() - 1));\n            moveChild(parent, badge, Math.max(0, parent.getChildCount() - 1));\n        }\n        parent.requestLayout();\n        parent.invalidate();\n        Log.i(TAG, weatherVisible\n                ? "CLEAN13.1F status order=settings/status icons > weather > date > clock"\n                : "CLEAN13.1F status order=settings/status icons > date > clock (weather hidden)");\n    }\n\n    private static void moveChild(ViewGroup parent, View child, int index) {\n        int current = parent.indexOfChild(child);\n        if (current < 0 || current == index) return;\n        ViewGroup.LayoutParams lp = child.getLayoutParams();\n        parent.removeView(child);\n        parent.addView(child, Math.max(0, Math.min(index, parent.getChildCount())), lp);\n    }\n'''
new = '''    private static void enforceStatusOrder(ViewGroup parent, ViewGroup icons, View badge, View clock) {\n        SharedPreferences p = prefs(parent.getContext());\n        boolean weatherVisible = p.getBoolean(KEY_SHOW, true)\n                && p.getString(KEY_QUERY, "").trim().length() != 0;\n        boolean changed = false;\n        changed |= moveChildIfNeeded(parent, icons, 0);\n        if (weatherVisible) {\n            changed |= moveChildIfNeeded(parent, badge, Math.min(1, parent.getChildCount() - 1));\n            changed |= moveChildIfNeeded(parent, clock, Math.min(2, parent.getChildCount() - 1));\n        } else {\n            changed |= moveChildIfNeeded(parent, clock, Math.min(1, parent.getChildCount() - 1));\n            changed |= moveChildIfNeeded(parent, badge, Math.max(0, parent.getChildCount() - 1));\n        }\n        if (changed) {\n            parent.requestLayout();\n            parent.invalidate();\n            Log.i(TAG, "CLEAN13.1F status order layout changed");\n        }\n        Log.i(TAG, weatherVisible\n                ? "CLEAN13.1F status order=settings/status icons > weather > date > clock"\n                : "CLEAN13.1F status order=settings/status icons > date > clock (weather hidden)");\n    }\n\n    private static boolean moveChildIfNeeded(ViewGroup parent, View child, int index) {\n        int current = parent.indexOfChild(child);\n        if (current < 0 || current == index) return false;\n        ViewGroup.LayoutParams lp = child.getLayoutParams();\n        parent.removeView(child);\n        parent.addView(child, Math.max(0, Math.min(index, parent.getChildCount())), lp);\n        return true;\n    }\n'''
assert s.count(old) == 1
s = s.replace(old, new, 1)

# WeatherBadgeView.setWeather used to requestLayout on every render even when the
# displayed state was identical. Only remeasure/repaint when something actually changed.
old = '''        void setWeather(String temperature, int code, boolean day, float textSize, int color) {\n            this.temperature = temperature;\n            this.code = code;\n            this.day = day;\n            this.textSize = Math.max(16f, textSize);\n            this.color = color;\n            requestLayout();\n            invalidate();\n        }\n'''
new = '''        void setWeather(String temperature, int code, boolean day, float textSize, int color) {\n            float normalizedSize = Math.max(16f, textSize);\n            boolean geometryChanged = !this.temperature.equals(temperature) || this.textSize != normalizedSize;\n            boolean paintChanged = geometryChanged || this.code != code || this.day != day || this.color != color;\n            this.temperature = temperature;\n            this.code = code;\n            this.day = day;\n            this.textSize = normalizedSize;\n            this.color = color;\n            if (geometryChanged) requestLayout();\n            if (paintChanged) invalidate();\n        }\n'''
assert s.count(old) == 1
s = s.replace(old, new, 1)

for token in ['moveChildIfNeeded', 'CLEAN13.1F status order layout changed', 'geometryChanged']:
    assert token in s, token

p.write_text(s)
print('PASS: CLEAN13.1F ANR guard removes redundant Home layout churn')
