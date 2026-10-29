package com.spocky.projengmenu.embedded;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** CLEAN13D: native Projectivy Status Bar clock/date presets and container styling. */
public final class StatusBarCustomizationBridge {
    private static final String TAG = "ProjectivyStatusBar";
    private static final String PREFS = "clean13d_clock_date_options";

    private static final String KEY_CLOCK = "clock_format";
    private static final String KEY_SHOW_DATE = "show_date";
    private static final String KEY_DATE = "date_format";
    private static final String KEY_BACKGROUND = "background_mode";

    private static final String CLOCK_SYSTEM = "system";
    private static final String CLOCK_24 = "24";
    private static final String CLOCK_12 = "12";
    private static final String CLOCK_24_SECONDS = "24_seconds";
    private static final String CLOCK_CUSTOM = "custom";

    private static final String DATE_DEFAULT = "EEE d MMM";
    private static final String DATE_SHORT = "d MMM";
    private static final String DATE_NUMERIC = "dd/MM";
    private static final String DATE_LONG = "d MMMM";
    private static final String DATE_FULL = "EEEE d MMMM";
    private static final String DATE_CUSTOM = "custom";

    private static final String BG_NONE = "none";
    private static final String BG_SEPARATE = "separate";
    private static final String BG_COMBINED = "combined";
    private static final String BG_UNIFIED = "unified";

    private static final long ID_CLOCK_PARENT = 0x13D00100L;
    private static final long ID_CLOCK_SYSTEM = 0x13D00101L;
    private static final long ID_CLOCK_24 = 0x13D00102L;
    private static final long ID_CLOCK_12 = 0x13D00103L;
    private static final long ID_CLOCK_24_SECONDS = 0x13D00104L;
    private static final long ID_CLOCK_CUSTOM = 0x13D00105L;
    private static final long ID_SHOW_DATE = 0x13D00200L;
    private static final long ID_DATE_PARENT = 0x13D00300L;
    private static final long ID_DATE_DEFAULT = 0x13D00301L;
    private static final long ID_DATE_SHORT = 0x13D00302L;
    private static final long ID_DATE_NUMERIC = 0x13D00303L;
    private static final long ID_DATE_LONG = 0x13D00304L;
    private static final long ID_DATE_FULL = 0x13D00305L;
    private static final long ID_DATE_CUSTOM = 0x13D00306L;
    private static final long ID_BG_PARENT = 0x13D00400L;
    private static final long ID_BG_NONE = 0x13D00401L;
    private static final long ID_BG_SEPARATE = 0x13D00402L;
    private static final long ID_BG_COMBINED = 0x13D00403L;
    private static final long ID_BG_UNIFIED = 0x13D00404L;

    private static final Map<ViewGroup, VisualState> STATES = new WeakHashMap<>();

    private StatusBarCustomizationBridge() {}

    public static void addClockDateActions(Object fragment, List list) {
        try {
            if (fragment == null || list == null) return;
            Context context = contextOf(fragment);
            if (context == null) return;
            SharedPreferences p = prefs(context);

            list.add(makeDropdown(fragment, ID_CLOCK_PARENT, "Clock format", clockLabel(p),
                    new long[]{ID_CLOCK_SYSTEM, ID_CLOCK_24, ID_CLOCK_12, ID_CLOCK_24_SECONDS, ID_CLOCK_CUSTOM},
                    new String[]{"System default", "24-hour — 17:32", "12-hour — 5:32 PM", "24-hour + seconds — 17:32:45", "Custom / advanced below"}));

            list.add(makeCheckbox(fragment, context, ID_SHOW_DATE, "Show date",
                    p.getBoolean(KEY_SHOW_DATE, true)));

            list.add(makeDropdown(fragment, ID_DATE_PARENT, "Date format", dateLabel(p),
                    new long[]{ID_DATE_DEFAULT, ID_DATE_SHORT, ID_DATE_NUMERIC, ID_DATE_LONG, ID_DATE_FULL, ID_DATE_CUSTOM},
                    new String[]{"Thu 29 Oct", "29 Oct", "29/10", "29 October", "Thursday 29 October", "Custom / advanced below"}));

            list.add(makeDropdown(fragment, ID_BG_PARENT, "Clock & date background", backgroundLabel(p),
                    new long[]{ID_BG_NONE, ID_BG_SEPARATE, ID_BG_COMBINED, ID_BG_UNIFIED},
                    new String[]{"None", "Separate", "Combined", "Combined with status icons"}));

            Log.i(TAG, "CLEAN13D native clock/date actions added");
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN13D add actions failed", t);
        }
    }

    public static boolean handleClockDateAction(Object fragment, Object action) {
        try {
            if (fragment == null || action == null) return false;
            long id = ((Number) field(action, "a").get(action)).longValue();
            Context context = contextOf(fragment);
            if (context == null) return false;
            SharedPreferences p = prefs(context);
            SharedPreferences.Editor e = p.edit();
            boolean handled = true;

            if (id == ID_SHOW_DATE) {
                Method checked = findMethod(action.getClass(), "c");
                e.putBoolean(KEY_SHOW_DATE, ((Boolean) checked.invoke(action)).booleanValue());
            } else if (id == ID_CLOCK_SYSTEM) e.putString(KEY_CLOCK, CLOCK_SYSTEM);
            else if (id == ID_CLOCK_24) e.putString(KEY_CLOCK, CLOCK_24);
            else if (id == ID_CLOCK_12) e.putString(KEY_CLOCK, CLOCK_12);
            else if (id == ID_CLOCK_24_SECONDS) e.putString(KEY_CLOCK, CLOCK_24_SECONDS);
            else if (id == ID_CLOCK_CUSTOM) e.putString(KEY_CLOCK, CLOCK_CUSTOM);
            else if (id == ID_DATE_DEFAULT) e.putString(KEY_DATE, DATE_DEFAULT);
            else if (id == ID_DATE_SHORT) e.putString(KEY_DATE, DATE_SHORT);
            else if (id == ID_DATE_NUMERIC) e.putString(KEY_DATE, DATE_NUMERIC);
            else if (id == ID_DATE_LONG) e.putString(KEY_DATE, DATE_LONG);
            else if (id == ID_DATE_FULL) e.putString(KEY_DATE, DATE_FULL);
            else if (id == ID_DATE_CUSTOM) {
                e.putString(KEY_DATE, DATE_CUSTOM);
                e.putString(KEY_CLOCK, CLOCK_CUSTOM);
            } else if (id == ID_BG_NONE) e.putString(KEY_BACKGROUND, BG_NONE);
            else if (id == ID_BG_SEPARATE) e.putString(KEY_BACKGROUND, BG_SEPARATE);
            else if (id == ID_BG_COMBINED) e.putString(KEY_BACKGROUND, BG_COMBINED);
            else if (id == ID_BG_UNIFIED) e.putString(KEY_BACKGROUND, BG_UNIFIED);
            else handled = false;

            if (!handled) return false;
            e.apply();
            applyClockPreference(context);
            refreshTitle(fragment);
            refreshMenu(fragment);
            Activity activity = activityOf(fragment);
            if (activity != null) {
                final WeakReference<Activity> ref = new WeakReference<>(activity);
                activity.getWindow().getDecorView().postDelayed(new Runnable() {
                    @Override public void run() {
                        Activity a = ref.get();
                        if (a != null && !a.isFinishing() && !a.isDestroyed()) applyVisual(a);
                    }
                }, 60L);
            }
            Log.i(TAG, "CLEAN13D action applied id=" + id + " clock=" + p.getString(KEY_CLOCK, CLOCK_SYSTEM)
                    + " date=" + p.getString(KEY_DATE, DATE_DEFAULT) + " bg=" + p.getString(KEY_BACKGROUND, BG_COMBINED));
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN13D handle action failed", t);
            return false;
        }
    }

    public static void applyToHome(final Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        try {
            applyClockPreference(activity);
            final WeakReference<Activity> ref = new WeakReference<>(activity);
            activity.getWindow().getDecorView().post(new Runnable() {
                @Override public void run() {
                    Activity a = ref.get();
                    if (a == null || a.isFinishing() || a.isDestroyed()) return;
                    try {
                        refreshClockView(a);
                        applyVisual(a);
                    } catch (Throwable t) {
                        Log.e(TAG, "CLEAN13D Home visual apply failed", t);
                    }
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN13D Home apply failed", t);
        }
    }

    private static void applyClockPreference(Context context) throws Exception {
        SharedPreferences p = prefs(context);
        String clockMode = p.getString(KEY_CLOCK, CLOCK_SYSTEM);
        String dateMode = p.getString(KEY_DATE, DATE_DEFAULT);
        if (CLOCK_CUSTOM.equals(clockMode) || DATE_CUSTOM.equals(dateMode)) {
            Log.i(TAG, "CLEAN13D native clock custom mode retained");
            return;
        }
        String time = timePattern(context, clockMode);
        boolean showDate = p.getBoolean(KEY_SHOW_DATE, true);
        String format = showDate ? dateMode + "   " + time : time;
        Object w1 = nativePreference("W1");
        Object x1 = nativePreference("X1");
        Method read = findMethod(w1.getClass(), "a");
        Method write = findMethod(w1.getClass(), "h", Object.class);
        String before = String.valueOf(read.invoke(w1));
        if (!format.equals(before)) write.invoke(w1, format);
        Method read2 = findMethod(x1.getClass(), "a");
        Method write2 = findMethod(x1.getClass(), "h", Object.class);
        String line2 = String.valueOf(read2.invoke(x1));
        if (line2.length() != 0) write2.invoke(x1, "");
        String verified = String.valueOf(read.invoke(w1));
        if (!format.equals(verified)) throw new IllegalStateException("CLEAN13D clock readback mismatch " + verified);
        Log.i(TAG, "CLEAN13D native clock format=" + verified);
    }

    private static String timePattern(Context context, String mode) {
        if (CLOCK_24.equals(mode)) return "HH:mm";
        if (CLOCK_12.equals(mode)) return "h:mm a";
        if (CLOCK_24_SECONDS.equals(mode)) return "HH:mm:ss";
        java.text.DateFormat df = DateFormat.getTimeFormat(context);
        if (df instanceof SimpleDateFormat) {
            String pattern = ((SimpleDateFormat) df).toPattern();
            if (pattern != null && pattern.trim().length() != 0) return pattern.trim();
        }
        return DateFormat.is24HourFormat(context) ? "HH:mm" : "h:mm a";
    }

    private static void refreshClockView(Activity activity) throws Exception {
        int id = activity.getResources().getIdentifier("title_text", "id", activity.getPackageName());
        View clock = activity.findViewById(id);
        if (clock != null) {
            try { findMethod(clock.getClass(), "a").invoke(clock); }
            catch (Throwable ignored) { clock.requestLayout(); clock.invalidate(); }
        }
    }

    private static void applyVisual(Activity activity) throws Exception {
        int clockId = activity.getResources().getIdentifier("title_text", "id", activity.getPackageName());
        int iconsId = activity.getResources().getIdentifier("statusbar_icons_container", "id", activity.getPackageName());
        View clockView = activity.findViewById(clockId);
        View iconsView = activity.findViewById(iconsId);
        if (!(clockView instanceof TextView) || !(iconsView instanceof ViewGroup)) return;
        TextView clock = (TextView) clockView;
        ViewGroup icons = (ViewGroup) iconsView;
        if (!(clock.getParent() instanceof ViewGroup) || clock.getParent() != icons.getParent()) return;
        ViewGroup parent = (ViewGroup) clock.getParent();

        VisualState state = STATES.get(parent);
        if (state == null) {
            state = new VisualState(parent, clock, icons);
            STATES.put(parent, state);
        }
        state.restore(parent, clock, icons);

        String mode = prefs(activity).getString(KEY_BACKGROUND, BG_COMBINED);
        ColorStateList tint = icons.getBackgroundTintList();
        int color = tint != null ? tint.getDefaultColor() : 0x66000000;

        if (BG_NONE.equals(mode)) {
            clock.setBackground(null);
        } else if (BG_SEPARATE.equals(mode)) {
            clock.setPadding(dp(activity, 14), state.clockPaddingTop, dp(activity, 14), state.clockPaddingBottom);
            clock.setBackground(new SegmentedPillDrawable(clock, color, dp(activity, 4)));
        } else if (BG_UNIFIED.equals(mode)) {
            Drawable pill = state.newIconBackground(activity, tint);
            parent.setBackground(pill);
            parent.setPadding(dp(activity, 4), state.parentPaddingTop, dp(activity, 4), state.parentPaddingBottom);
            icons.setBackground(null);
            moveChild(parent, clock, 0);
            clock.setPadding(dp(activity, 10), state.clockPaddingTop, dp(activity, 10), state.clockPaddingBottom);
        } else {
            Drawable pill = state.newIconBackground(activity, tint);
            clock.setBackground(pill);
            clock.setPadding(dp(activity, 14), state.clockPaddingTop, dp(activity, 14), state.clockPaddingBottom);
        }
        clock.invalidate();
        parent.invalidate();
        Log.i(TAG, "CLEAN13D visual mode=" + mode);
    }

    private static void moveChild(ViewGroup parent, View child, int index) {
        int current = parent.indexOfChild(child);
        if (current == index || current < 0) return;
        ViewGroup.LayoutParams lp = child.getLayoutParams();
        parent.removeView(child);
        parent.addView(child, Math.min(index, parent.getChildCount()), lp);
    }

    private static Object makeDropdown(Object fragment, long id, String title, String selected,
                                       long[] childIds, String[] childTitles) throws Exception {
        Class<?> cb3 = Class.forName("cb3");
        Class<?> lu1 = Class.forName("lu1");
        Method t0 = cb3.getDeclaredMethod("t0", cb3, long.class, CharSequence.class, Drawable.class, int.class);
        t0.setAccessible(true);
        Method z0 = cb3.getDeclaredMethod("z0", lu1);
        z0.setAccessible(true);
        Object parent = t0.invoke(null, fragment, id, title, null, 28);
        field(parent, "d").set(parent, selected);
        ArrayList<Object> children = new ArrayList<>();
        for (int i = 0; i < childIds.length; i++) {
            Object child = t0.invoke(null, fragment, childIds[i], childTitles[i], null, 28);
            children.add(z0.invoke(null, child));
        }
        field(parent, "k").set(parent, children);
        return z0.invoke(null, parent);
    }

    private static Object makeCheckbox(Object fragment, Context context, long id, String title, boolean checked) throws Exception {
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

    private static String clockLabel(SharedPreferences p) {
        String v = p.getString(KEY_CLOCK, CLOCK_SYSTEM);
        if (CLOCK_24.equals(v)) return "24-hour — 17:32";
        if (CLOCK_12.equals(v)) return "12-hour — 5:32 PM";
        if (CLOCK_24_SECONDS.equals(v)) return "24-hour + seconds — 17:32:45";
        if (CLOCK_CUSTOM.equals(v)) return "Custom / advanced below";
        return "System default";
    }

    private static String dateLabel(SharedPreferences p) {
        String v = p.getString(KEY_DATE, DATE_DEFAULT);
        if (DATE_SHORT.equals(v)) return "29 Oct";
        if (DATE_NUMERIC.equals(v)) return "29/10";
        if (DATE_LONG.equals(v)) return "29 October";
        if (DATE_FULL.equals(v)) return "Thursday 29 October";
        if (DATE_CUSTOM.equals(v)) return "Custom / advanced below";
        return "Thu 29 Oct";
    }

    private static String backgroundLabel(SharedPreferences p) {
        String v = p.getString(KEY_BACKGROUND, BG_COMBINED);
        if (BG_NONE.equals(v)) return "None";
        if (BG_SEPARATE.equals(v)) return "Separate";
        if (BG_UNIFIED.equals(v)) return "Combined with status icons";
        return "Combined";
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static Object nativePreference(String fieldName) throws Exception {
        Class<?> hj3 = Class.forName("hj3");
        Field f = hj3.getDeclaredField(fieldName);
        f.setAccessible(true);
        Object v = f.get(null);
        if (v == null) throw new IllegalStateException("native preference unavailable " + fieldName);
        return v;
    }

    private static Context contextOf(Object fragment) {
        try {
            Object c = findMethod(fragment.getClass(), "k").invoke(fragment);
            return c instanceof Context ? (Context) c : null;
        } catch (Throwable ignored) { return null; }
    }

    private static Activity activityOf(Object fragment) {
        Context c = contextOf(fragment);
        return c instanceof Activity ? (Activity) c : null;
    }

    private static void refreshTitle(Object fragment) {
        try {
            Field j0 = field(fragment, "J0");
            Object mainFragment = j0.get(fragment);
            if (mainFragment == null) return;
            Field k1 = field(mainFragment, "K1");
            Object title = k1.get(mainFragment);
            if (title != null) findMethod(title.getClass(), "i").invoke(title);
        } catch (Throwable t) {
            Log.w(TAG, "CLEAN13D title refresh deferred", t);
        }
    }

    private static void refreshMenu(Object fragment) {
        try { findMethod(fragment.getClass(), "K0").invoke(fragment); }
        catch (Throwable t) { Log.w(TAG, "CLEAN13D menu refresh deferred", t); }
    }

    private static Field field(Object object, String name) throws Exception {
        return field(object.getClass(), name);
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Class<?> c = type;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... params) throws Exception {
        Class<?> c = type;
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchMethodException(name);
    }

    private static int dp(Context c, int dp) {
        return Math.round(dp * c.getResources().getDisplayMetrics().density);
    }

    private static final class VisualState {
        final Drawable.ConstantState iconBackground;
        final ColorStateList iconTint;
        final int clockIndex;
        final int iconsIndex;
        final int parentPaddingLeft, parentPaddingTop, parentPaddingRight, parentPaddingBottom;
        final int clockPaddingLeft, clockPaddingTop, clockPaddingRight, clockPaddingBottom;

        VisualState(ViewGroup parent, TextView clock, ViewGroup icons) {
            Drawable bg = icons.getBackground();
            iconBackground = bg != null ? bg.getConstantState() : null;
            iconTint = icons.getBackgroundTintList();
            clockIndex = parent.indexOfChild(clock);
            iconsIndex = parent.indexOfChild(icons);
            parentPaddingLeft = parent.getPaddingLeft(); parentPaddingTop = parent.getPaddingTop();
            parentPaddingRight = parent.getPaddingRight(); parentPaddingBottom = parent.getPaddingBottom();
            clockPaddingLeft = clock.getPaddingLeft(); clockPaddingTop = clock.getPaddingTop();
            clockPaddingRight = clock.getPaddingRight(); clockPaddingBottom = clock.getPaddingBottom();
        }

        void restore(ViewGroup parent, TextView clock, ViewGroup icons) {
            parent.setBackground(null);
            parent.setPadding(parentPaddingLeft, parentPaddingTop, parentPaddingRight, parentPaddingBottom);
            clock.setBackground(null);
            clock.setPadding(clockPaddingLeft, clockPaddingTop, clockPaddingRight, clockPaddingBottom);
            Drawable bg = newIconBackground(parent.getContext(), iconTint);
            icons.setBackground(bg);
            icons.setBackgroundTintList(iconTint);
            if (parent.indexOfChild(icons) != iconsIndex) moveChild(parent, icons, Math.min(iconsIndex, Math.max(0, parent.getChildCount() - 1)));
            if (parent.indexOfChild(clock) != clockIndex) moveChild(parent, clock, Math.min(clockIndex, Math.max(0, parent.getChildCount() - 1)));
        }

        Drawable newIconBackground(Context context, ColorStateList tint) {
            Drawable d = iconBackground != null ? iconBackground.newDrawable(context.getResources()).mutate() : null;
            if (d != null && tint != null) d.setTintList(tint);
            return d;
        }
    }

    private static final class SegmentedPillDrawable extends Drawable {
        private final WeakReference<TextView> viewRef;
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final float gap;

        SegmentedPillDrawable(TextView view, int color, float gap) {
            viewRef = new WeakReference<>(view);
            paint.setColor(color);
            this.gap = gap;
        }

        @Override public void draw(Canvas canvas) {
            TextView v = viewRef.get();
            if (v == null) return;
            Layout layout = v.getLayout();
            CharSequence cs = v.getText();
            if (layout == null || cs == null || cs.length() == 0 || layout.getLineCount() == 0) {
                drawOne(canvas);
                return;
            }
            String s = cs.toString();
            int sep = s.indexOf("   ");
            if (sep <= 0 || sep + 3 >= s.length()) {
                drawOne(canvas);
                return;
            }
            float offset = v.getCompoundPaddingLeft();
            float x0 = offset + layout.getPrimaryHorizontal(0);
            float x1 = offset + layout.getPrimaryHorizontal(sep);
            float x2 = offset + layout.getPrimaryHorizontal(sep + 3);
            float x3 = offset + layout.getPrimaryHorizontal(s.length());
            float top = 0f;
            float bottom = getBounds().height();
            float radius = bottom / 2f;
            canvas.drawRoundRect(new RectF(Math.min(x0, x1) - gap, top, Math.max(x0, x1) + gap, bottom), radius, radius, paint);
            canvas.drawRoundRect(new RectF(Math.min(x2, x3) - gap, top, Math.max(x2, x3) + gap, bottom), radius, radius, paint);
        }

        private void drawOne(Canvas canvas) {
            RectF r = new RectF(getBounds());
            float radius = r.height() / 2f;
            canvas.drawRoundRect(r, radius, radius, paint);
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
        @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) { paint.setColorFilter(colorFilter); invalidateSelf(); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }
}
