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

/** CLEAN 07F: bind Red Light's grouped Projectivy preference to the stock
 * Leanback multi-choice path. No secondary dialog is created. */
public final class ChannelVisualBridge {
    private static final String TAG = "ProjectivyChannels";
    private static final Map<Object, Binding> BINDINGS = new WeakHashMap<>();

    private ChannelVisualBridge() {}

    public static void decorateGroupedPreference(Object preference, List<?> categories) {
        if (preference == null || categories == null || categories.size() <= 1) return;
        try {
            Snapshot s = snapshot(preference, categories, true);
            synchronized (BINDINGS) {
                if (s == null || !s.redLightGroup) {
                    BINDINGS.remove(preference);
                    return;
                }
                ChangeInvoker change = findChangeInvoker(preference);
                if (change == null) {
                    BINDINGS.remove(preference);
                    Log.e(TAG, "CLEAN07F no Projectivy visibility callback");
                    return;
                }
                BINDINGS.put(preference, new Binding(new ArrayList<Object>((List<Object>)(List<?>)categories), change));
            }
            Log.i(TAG, "CLEAN07F grouped preference bound count=" + s.values.length
                    + " checked=" + getSelectedValues(preference).size()
                    + " entriesField=" + s.entriesField + " valuesField=" + s.valuesField);
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07F grouped preference binding failed", t);
        }
    }

    public static boolean isRedLightGroupedPreference(Object preference) {
        synchronized (BINDINGS) {
            return preference != null && BINDINGS.containsKey(preference);
        }
    }

    public static Set<String> getSelectedValues(Object preference) {
        Binding b;
        synchronized (BINDINGS) { b = BINDINGS.get(preference); }
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
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07F selected-state read failed", t);
        }
        return out;
    }

    public static boolean applySelectedValues(Object preference, Set<?> desiredRaw) {
        Binding b;
        synchronized (BINDINGS) { b = BINDINGS.get(preference); }
        if (b == null || desiredRaw == null) return false;
        HashSet<String> desired = new HashSet<>();
        for (Object x : desiredRaw) if (x != null) desired.add(String.valueOf(x));
        int changed = 0;
        try {
            for (Object category : b.categories) {
                if (category == null) continue;
                Field id = findField(category.getClass(), "j");
                Field visible = findField(category.getClass(), "n");
                if (id == null || visible == null) continue;
                id.setAccessible(true); visible.setAccessible(true);
                String value = String.valueOf(id.getInt(category));
                boolean current = visible.getBoolean(category);
                boolean wanted = desired.contains(value);
                if (current == wanted) continue;
                if (!b.change.invoke(value)) {
                    Log.e(TAG, "CLEAN07F visibility callback rejected value=" + value);
                    return false;
                }
                changed++;
            }
            Log.i(TAG, "CLEAN07F toggle-set desired=" + desired.size() + " changed=" + changed + " accepted=true");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07F visibility apply failed", t);
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
            int matches = idMatches(a, visibleById.keySet());
            if (matches > best) { best = matches; valuesField = f; values = a; }
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

        CharSequence[] labels = new CharSequence[entries.length];
        int red = 0;
        for (int i = 0; i < entries.length; i++) {
            String label = cleanLabel(entries[i] == null ? "" : entries[i].toString());
            labels[i] = label;
            if (label.startsWith("Red Light")) red++;
        }
        if (cleanEntries) entriesField.set(preference, labels);
        return new Snapshot(values, red >= 2 && red * 2 >= labels.length,
                entriesField.getName(), valuesField.getName());
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
            for (Method m : allMethods(preference.getClass())) {
                Class<?>[] p = m.getParameterTypes();
                if ((m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)
                        && p.length == 1 && p[0] == Object.class) {
                    m.setAccessible(true);
                    final Method call = m;
                    return new ChangeInvoker() {
                        @Override public boolean invoke(String value) throws Exception {
                            Object r = call.invoke(preference, value);
                            return !(r instanceof Boolean) || ((Boolean) r).booleanValue();
                        }
                    };
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07F callback discovery failed", t);
        }
        return null;
    }

    private static Method findBooleanCallback(Class<?> iface, int params, Class<?> preferenceClass) {
        try {
            for (Method m : iface.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if ((m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)
                        && p.length == params && p.length > 0
                        && p[0].isAssignableFrom(preferenceClass)) return m;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static int idMatches(CharSequence[] a, Set<String> ids) {
        if (a == null) return -1;
        int n = 0;
        for (CharSequence x : a) if (x != null && ids.contains(x.toString())) n++;
        return n;
    }

    private static Field findField(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { return k.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    private static List<Field> allFields(Class<?> c) {
        ArrayList<Field> out = new ArrayList<>();
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            Field[] fs = k.getDeclaredFields();
            for (Field f : fs) out.add(f);
        }
        return out;
    }

    private static List<Method> allMethods(Class<?> c) {
        ArrayList<Method> out = new ArrayList<>();
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            Method[] ms = k.getDeclaredMethods();
            for (Method m : ms) out.add(m);
        }
        return out;
    }

    private interface ChangeInvoker { boolean invoke(String value) throws Exception; }

    private static final class Binding {
        final List<Object> categories;
        final ChangeInvoker change;
        Binding(List<Object> categories, ChangeInvoker change) { this.categories = categories; this.change = change; }
    }

    private static final class Snapshot {
        final CharSequence[] values;
        final boolean redLightGroup;
        final String entriesField;
        final String valuesField;
        Snapshot(CharSequence[] values, boolean redLightGroup, String entriesField, String valuesField) {
            this.values = values; this.redLightGroup = redLightGroup;
            this.entriesField = entriesField; this.valuesField = valuesField;
        }
    }
}
