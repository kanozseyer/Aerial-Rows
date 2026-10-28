package com.spocky.projengmenu.embedded;

import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** CLEAN 07I: CLEAN 07H one-channel navigation plus authoritative selector
 * resync when the stock Leanback selector resumes after a channel detail page. */
public final class ChannelVisualBridge {
    private static final String TAG = "ProjectivyChannels";
    private static final Map<Object, Binding> BINDINGS = new WeakHashMap<>();

    private ChannelVisualBridge() {}

    public static void decorateGroupedPreference(Object preference, List<?> categories) {
        if (preference == null || categories == null || categories.size() <= 1) return;
        try {
            Snapshot s = snapshot(preference, categories, true);
            synchronized (BINDINGS) {
                if (s == null || !s.redLightGroup) { BINDINGS.remove(preference); return; }
                ChangeInvoker change = findChangeInvoker(preference);
                if (change == null) {
                    BINDINGS.remove(preference);
                    Log.e(TAG, "CLEAN07H no Projectivy channel-navigation callback");
                    return;
                }
                Binding prev = BINDINGS.get(preference);
                Binding b = new Binding(new ArrayList<Object>((List<Object>)(List<?>) categories), change);
                if (prev != null) { b.selectorFragment = prev.selectorFragment; b.selectorAdapter = prev.selectorAdapter; }
                BINDINGS.put(preference, b);
            }
            Log.i(TAG, "CLEAN07H grouped preference bound count=" + s.values.length
                    + " checked=" + getSelectedValues(preference).size());
        } catch (Throwable t) { Log.e(TAG, "CLEAN07H grouped preference binding failed", t); }
    }

    public static boolean isRedLightGroupedPreference(Object preference) {
        synchronized (BINDINGS) { return preference != null && BINDINGS.containsKey(preference); }
    }

    public static Set<String> getSelectedValues(Object preference) {
        Binding b;
        synchronized (BINDINGS) { b = BINDINGS.get(preference); }
        return selectedValues(b);
    }

    public static boolean openChannel(Object preference, String value, Object fragment, Object adapter) {
        Binding b;
        synchronized (BINDINGS) {
            b = BINDINGS.get(preference);
            if (b != null) { b.selectorFragment = fragment; b.selectorAdapter = adapter; }
        }
        if (b == null || value == null) return false;
        try {
            boolean accepted = b.change.invoke(value);
            syncSelector(b, "open");
            Log.i(TAG, "CLEAN07H open-channel value=" + value + " accepted=" + accepted);
            return accepted;
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07H channel navigation failed value=" + value, t);
            return false;
        }
    }

    /** Called from ie2.onResume (obfuscated H()) after returning from the stock
     * Projectivy per-channel customization page. The real i5.n Visible flags are
     * authoritative; redraw the already-live adapter from them immediately. */
    public static void onSelectorResumed(Object preference, Object fragment) {
        Binding b;
        synchronized (BINDINGS) {
            b = BINDINGS.get(preference);
            if (b != null && fragment != null) b.selectorFragment = fragment;
        }
        boolean refreshed = syncSelector(b, "resume");
        int checked = b == null ? 0 : selectedValues(b).size();
        Log.i(TAG, "CLEAN07I resume-sync selectors=" + (refreshed ? 1 : 0) + " checked=" + checked);
    }

    public static void onCategoryVisibilityChanged(Object category) {
        if (category == null) return;
        int id = categoryId(category);
        int refreshed = 0;
        synchronized (BINDINGS) {
            for (Binding b : BINDINGS.values()) {
                if (b != null && containsCategory(b, id, category) && syncSelector(b, "visibility")) refreshed++;
            }
        }
        Log.i(TAG, "CLEAN07H visibility-sync category=" + id + " selectors=" + refreshed);
    }

    private static boolean containsCategory(Binding b, int id, Object target) {
        for (Object c : b.categories) {
            if (c == target) return true;
            if (id != Integer.MIN_VALUE && categoryId(c) == id) return true;
        }
        return false;
    }

    private static int categoryId(Object category) {
        if (category == null) return Integer.MIN_VALUE;
        try {
            Field f = findField(category.getClass(), "j");
            if (f == null) return Integer.MIN_VALUE;
            f.setAccessible(true);
            return f.getInt(category);
        } catch (Throwable ignored) { return Integer.MIN_VALUE; }
    }

    private static Set<String> selectedValues(Binding b) {
        HashSet<String> out = new HashSet<>();
        if (b == null) return out;
        try {
            for (Object category : b.categories) {
                if (category == null) continue;
                Field id = findField(category.getClass(), "j");
                Field visible = findField(category.getClass(), "n");
                if (id == null || visible == null) continue;
                id.setAccessible(true); visible.setAccessible(true);
                if (visible.getBoolean(category)) out.add(String.valueOf(id.getInt(category)));
            }
        } catch (Throwable t) { Log.e(TAG, "CLEAN07H selected-state read failed", t); }
        return out;
    }

    private static boolean syncSelector(Binding b, String reason) {
        if (b == null || b.selectorFragment == null || b.selectorAdapter == null) return false;
        try {
            HashSet<String> selected = new HashSet<>(selectedValues(b));
            Field a0 = findField(b.selectorFragment.getClass(), "A0");
            Field g = findField(b.selectorAdapter.getClass(), "g");
            if (a0 == null || g == null) return false;
            a0.setAccessible(true); g.setAccessible(true);
            a0.set(b.selectorFragment, new HashSet<String>(selected));
            g.set(b.selectorAdapter, new HashSet<String>(selected));
            Method redraw = findNoArgMethod(b.selectorAdapter.getClass(), "d");
            if (redraw != null) { redraw.setAccessible(true); redraw.invoke(b.selectorAdapter); }
            Log.i(TAG, "CLEAN07H selector-sync reason=" + reason + " checked=" + selected.size());
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07H selector sync failed reason=" + reason, t);
            return false;
        }
    }

    private static Snapshot snapshot(Object preference, List<?> categories, boolean cleanEntries) throws Exception {
        LinkedHashMap<String, Boolean> visibleById = new LinkedHashMap<>();
        for (Object category : categories) {
            if (category == null) continue;
            Field id = findField(category.getClass(), "j");
            Field visible = findField(category.getClass(), "n");
            if (id == null || visible == null) continue;
            id.setAccessible(true); visible.setAccessible(true);
            visibleById.put(String.valueOf(id.getInt(category)), visible.getBoolean(category));
        }
        if (visibleById.isEmpty()) return null;
        List<Field> arrays = new ArrayList<>();
        for (Field f : allFields(preference.getClass())) {
            if (!f.getType().equals(CharSequence[].class)) continue;
            f.setAccessible(true);
            if (f.get(preference) instanceof CharSequence[]) arrays.add(f);
        }
        Field valuesField = null; CharSequence[] values = null; int best = -1;
        for (Field f : arrays) {
            CharSequence[] a = (CharSequence[]) f.get(preference);
            int m = idMatches(a, visibleById.keySet());
            if (m > best) { best = m; valuesField = f; values = a; }
        }
        if (valuesField == null || values == null || best <= 0) return null;
        Field entriesField = null; CharSequence[] entries = null;
        for (Field f : arrays) {
            if (f == valuesField) continue;
            CharSequence[] a = (CharSequence[]) f.get(preference);
            if (a != null && a.length == values.length) {
                entriesField = f; entries = a;
                if (idMatches(a, visibleById.keySet()) == 0) break;
            }
        }
        if (entriesField == null || entries == null) return null;
        CharSequence[] labels = new CharSequence[entries.length]; int red = 0;
        for (int i = 0; i < entries.length; i++) {
            String label = cleanLabel(entries[i] == null ? "" : entries[i].toString());
            labels[i] = label; if (label.startsWith("Red Light")) red++;
        }
        if (cleanEntries) entriesField.set(preference, labels);
        return new Snapshot(values, red >= 2 && red * 2 >= labels.length);
    }

    private static String cleanLabel(String label) {
        if (label == null) return "";
        while (label.startsWith("✓  ")) label = label.substring(3);
        label = label.replace("\uFFFC", "").replace('\u00A0', ' ');
        return label.replaceAll("[\\s\\u2007\\u202F]+$", "");
    }

    private static ChangeInvoker findChangeInvoker(final Object preference) {
        try {
            for (Field f : allFields(preference.getClass())) {
                Class<?> t = f.getType();
                if (!t.isInterface()) continue;
                Method cb = findBooleanCallback(t, 2, preference.getClass());
                if (cb == null) continue;
                f.setAccessible(true);
                final Object listener = f.get(preference);
                if (listener == null) continue;
                cb.setAccessible(true);
                final Method call = cb;
                return new ChangeInvoker() {
                    @Override public boolean invoke(String value) throws Exception {
                        Object r = call.invoke(listener, preference, value);
                        return !(r instanceof Boolean) || ((Boolean) r).booleanValue();
                    }
                };
            }
        } catch (Throwable t) { Log.e(TAG, "CLEAN07H callback discovery failed", t); }
        return null;
    }

    private static Method findBooleanCallback(Class<?> iface, int params, Class<?> preferenceClass) {
        try {
            for (Method m : iface.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if ((m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)
                        && p.length == params && p[0].isAssignableFrom(preferenceClass)) return m;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static int idMatches(CharSequence[] a, Set<String> ids) {
        if (a == null) return -1;
        int n = 0; for (CharSequence x : a) if (x != null && ids.contains(x.toString())) n++;
        return n;
    }

    private static Field findField(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { return k.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    private static Method findNoArgMethod(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { return k.getDeclaredMethod(name); } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    private static List<Field> allFields(Class<?> c) {
        ArrayList<Field> out = new ArrayList<>();
        for (Class<?> k = c; k != null; k = k.getSuperclass()) for (Field f : k.getDeclaredFields()) out.add(f);
        return out;
    }

    private interface ChangeInvoker { boolean invoke(String value) throws Exception; }
    private static final class Binding {
        final List<Object> categories; final ChangeInvoker change;
        Object selectorFragment; Object selectorAdapter;
        Binding(List<Object> categories, ChangeInvoker change) { this.categories = categories; this.change = change; }
    }
    private static final class Snapshot {
        final CharSequence[] values; final boolean redLightGroup;
        Snapshot(CharSequence[] values, boolean redLightGroup) { this.values = values; this.redLightGroup = redLightGroup; }
    }
}
