package com.spocky.projengmenu.embedded;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** CLEAN13.1K: presentation-only monochrome treatment for Projectivy menu/header icons. */
public final class MenuIconBridge {
    private static final String TAG = "ProjectivyMenuIcons";
    private static boolean loggedHeader;
    private static boolean loggedPreference;

    private MenuIconBridge() {}

    public static void tintHeaderIcon(View view) {
        tint(view, true);
    }

    public static void tintPreferenceIcon(View view) {
        tint(view, false);
    }

    private static void tint(View view, boolean header) {
        if (!(view instanceof ImageView)) return;
        try {
            int color = projectivyIconColor();
            ((ImageView) view).setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
            if (header) {
                if (!loggedHeader) {
                    loggedHeader = true;
                    Log.i(TAG, "CLEAN13.1K monochrome left-lane/header icons enabled color=" + color);
                }
            } else if (!loggedPreference) {
                loggedPreference = true;
                Log.i(TAG, "CLEAN13.1K monochrome Projectivy preference icons enabled color=" + color);
            }
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN13.1K icon tint failed", t);
        }
    }

    private static int projectivyIconColor() throws Exception {
        Class<?> hj3 = Class.forName("hj3");
        Field iconColorField = hj3.getDeclaredField("i0");
        iconColorField.setAccessible(true);
        Object preference = iconColorField.get(null);
        Method read = findMethod(preference.getClass(), "a");
        Object raw = read.invoke(preference);
        int color = ((Number) raw).intValue();

        Class<?> h03 = Class.forName("h03");
        Method normalize = h03.getDeclaredMethod("b", int.class);
        normalize.setAccessible(true);
        return ((Number) normalize.invoke(null, color)).intValue();
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... params) throws Exception {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }
}
