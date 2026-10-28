package com.spocky.projengmenu.embedded;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * CLEAN 07F: turn Projectivy's existing grouped Red Light selector into a
 * visibility multi-selector without adding a second dialog.
 *
 * Projectivy 4.72 already contains a native Leanback multi-choice rendering path
 * in ie2/ge2/he2. The stock grouped channel preference is a ListPreference whose
 * click normally opens one CategoryPreferencesFragment. CLEAN 07F keeps that
 * exact Projectivy panel but, for the Red Light group only, initializes its
 * circles from each category's real i5.n (Visible) state and writes each toggle
 * back to that same state. Other grouped providers retain stock navigation.
 */
public final class ChannelVisualBridge {
    private static final String TAG = "ProjectivyChannels";
    private static final Map<Object, Group> GROUPS =
            Collections.synchronizedMap(new WeakHashMap<Object, Group>());

    private ChannelVisualBridge() {}

    /** Called from the existing g70 grouped-preference hook. */
    public static void decorateGroupedPreference(Object preference, List<?> categories) {
        if (preference == null || categories == null || categories.size() <= 1) return;
        try {
            Group group = buildGroup(preference, categories, true);
            if (group == null || !group.redLight) return;
            GROUPS.put(preference, group);
            Log.i(TAG, "CLEAN07F grouped Red Light registered count=" + group.values.length
                    + " visible=" + visibleCount(group));
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07F grouped Red Light registration failed", t);
        }
    }

    /**
     * Called directly inside ie2.A() while the stock Projectivy Leanback list
     * dialog is being initialized. Returning true means this ordinary
     * ListPreference should use ie2's already-built multi-choice adapter path.
     */
    public static boolean configureNativeVisibilityDialog(Object fragment, Object preference) {
        if (fragment == null || preference == null) return false;
        try {
            Group group = groupFor(preference);
            if (group == null || !group.redLight) return false;

            HashSet<String> selected = selectedIds(group);
            setField(fragment, "v0", Boolean.TRUE);
            setField(fragment, "A0", selected);
            setField(fragment, "w0", group.labels.clone());
            setField(fragment, "x0", group.values.clone());
            try { setField(fragment, "B0", null); } catch (Throwable ignored) {}

            Log.i(TAG, "CLEAN07F native visibility dialog count=" + group.values.length
                    + " selected=" + selected.size());
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07F native visibility dialog bind failed", t);
            return false;
        }
    }

    /**
     * Called from he2's native multi-choice click branch before its normal
     * MultiSelectListPreference cast. For our synthetic Red Light multi mode we
     * consume the click, update real category visibility, persist the category
     * model, refresh observers, and keep the same panel open.
     */
    public static boolean applyNativeVisibilitySelection(Object fragment, Set<?> requested) {
        if (fragment == null || requested == null) return false;

        Object preference = null;
        Group group = null;
        try {
            preference = invokeNoArg(fragment, "X");
            if (preference == null) return false;
            group = groupFor(preference);
            if (group == null || !group.redLight) return false;

            HashSet<String> wanted = new HashSet<String>();
            for (Object v : requested) if (v != null) wanted.add(String.valueOf(v));

            int changed = 0;
            int enabled = 0;
            int disabled = 0;
            ArrayList<Object> newlyEnabled = new ArrayList<Object>();

            for (Map.Entry<String, Object> e : group.categoriesById.entrySet()) {
                Object category = e.getValue();
                boolean desired = wanted.contains(e.getKey());
                boolean actual = getBooleanField(category, "n");
                if (desired == actual) continue;

                setBooleanField(category, "n", desired);
                changed++;
                if (desired) {
                    enabled++;
                    newlyEnabled.add(category);
                } else {
                    disabled++;
                }
            }

            // Update the dialog's canonical selection snapshot immediately. ge2
            // already owns the same mutable HashSet for its visible check states.
            setField(fragment, "A0", new HashSet<String>(wanted));

            if (changed > 0) {
                persistAndNotify();
                for (final Object category : newlyEnabled) {
                    requestCategoryLoad(category);
                }
            }

            Log.i(TAG, "CLEAN07F visibility apply selected=" + wanted.size()
                    + " changed=" + changed + " enabled=" + enabled
                    + " disabled=" + disabled);
            return true;
        } catch (Throwable t) {
            // If this is our Red Light group, consume the click even on an update
            // error so he2 never falls through to a MultiSelectListPreference cast
            // on the underlying stock ListPreference. Re-sync the on-screen set
            // from the authoritative category state where possible.
            if (group != null && group.redLight) {
                try {
                    HashSet<String> actual = selectedIds(group);
                    requested.clear();
                    @SuppressWarnings("unchecked") Set<Object> mutable = (Set<Object>) requested;
                    mutable.addAll(actual);
                    setField(fragment, "A0", actual);
                } catch (Throwable ignored) {}
                Log.e(TAG, "CLEAN07F visibility apply failed; restored authoritative state", t);
                return true;
            }
            Log.e(TAG, "CLEAN07F visibility apply lookup failed", t);
            return false;
        }
    }

    private static void persistAndNotify() throws Exception {
        Class<?> w40 = Class.forName("w40");
        invokeStaticNoArg(w40, "u"); // persist action/category state
        invokeStaticNoArg(w40, "m"); // notify current Home/category observers
    }

    private static void requestCategoryLoad(final Object category) {
        try {
            final Class<?> w40 = Class.forName("w40");
            Method load = null;
            for (Method m : w40.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers()) || !"o".equals(m.getName())) continue;
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 2 && Runnable.class.isAssignableFrom(p[1])) {
                    load = m;
                    break;
                }
            }
            if (load == null) return;
            load.setAccessible(true);
            load.invoke(null, category, new Runnable() {
                @Override public void run() {
                    try {
                        invokeStaticNoArg(w40, "u");
                        invokeStaticNoArg(w40, "m");
                        Log.i(TAG, "CLEAN07F enabled channel load complete");
                    } catch (Throwable t) {
                        Log.i(TAG, "CLEAN07F enabled channel post-load refresh unavailable");
                    }
                }
            });
        } catch (Throwable t) {
            Log.i(TAG, "CLEAN07F enabled channel load unavailable; visibility already persisted");
        }
    }

    private static void invokeStaticNoArg(Class<?> c, String name) throws Exception {
        Method m = null;
        for (Method x : c.getDeclaredMethods()) {
            if (Modifier.isStatic(x.getModifiers()) && name.equals(x.getName())
                    && x.getParameterTypes().length == 0) {
                m = x;
                break;
            }
        }
        if (m == null) throw new NoSuchMethodException(c.getName() + "." + name + "()");
        m.setAccessible(true);
        m.invoke(null);
    }

    private static Group groupFor(Object preference) throws Exception {
        Group g = GROUPS.get(preference);
        if (g != null) return g;

        // Defensive reconstruction for fragment recreation: resolve the stored
        // category IDs back through w40.f(int). This remains limited to lists whose
        // visible labels are clearly Red Light rows.
        List<Field> arrays = charSequenceArrays(preference);
        if (arrays.size() < 2) return null;

        Field valuesField = null;
        CharSequence[] values = null;
        Class<?> w40 = Class.forName("w40");
        Method findCategory = null;
        for (Method m : w40.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers()) && "f".equals(m.getName())) {
                Class<?>[] p = m.getParameterTypes();
                if (p.length == 1 && p[0] == int.class) {
                    findCategory = m;
                    break;
                }
            }
        }
        if (findCategory == null) return null;
        findCategory.setAccessible(true);

        int best = -1;
        ArrayList<Object> bestCategories = null;
        for (Field f : arrays) {
            CharSequence[] a = (CharSequence[]) f.get(preference);
            if (a == null) continue;
            int matches = 0;
            ArrayList<Object> cats = new ArrayList<Object>();
            for (CharSequence x : a) {
                Object cat = null;
                if (x != null) {
                    try { cat = findCategory.invoke(null, Integer.parseInt(x.toString())); }
                    catch (Throwable ignored) {}
                }
                cats.add(cat);
                if (cat != null) matches++;
            }
            if (matches > best) {
                best = matches;
                valuesField = f;
                values = a;
                bestCategories = cats;
            }
        }
        if (best <= 0 || valuesField == null || values == null || bestCategories == null) return null;

        Field entriesField = null;
        CharSequence[] entries = null;
        for (Field f : arrays) {
            if (f == valuesField) continue;
            CharSequence[] a = (CharSequence[]) f.get(preference);
            if (a != null && a.length == values.length) {
                entriesField = f;
                entries = a;
                break;
            }
        }
        if (entriesField == null || entries == null) return null;

        Group rebuilt = buildGroup(preference, bestCategories, true);
        if (rebuilt != null && rebuilt.redLight) GROUPS.put(preference, rebuilt);
        return rebuilt;
    }

    private static Group buildGroup(Object preference, List<?> categories, boolean cleanDisplay) throws Exception {
        LinkedHashMap<String, Object> categoryById = new LinkedHashMap<String, Object>();
        for (Object category : categories) {
            if (category == null) continue;
            Field id = findField(category.getClass(), "j");
            if (id == null) continue;
            id.setAccessible(true);
            categoryById.put(String.valueOf(id.getInt(category)), category);
        }
        if (categoryById.size() < 2) return null;

        List<Field> arrays = charSequenceArrays(preference);
        if (arrays.size() < 2) return null;

        Field valuesField = null;
        CharSequence[] values = null;
        int bestMatches = -1;
        for (Field f : arrays) {
            CharSequence[] a = (CharSequence[]) f.get(preference);
            int matches = idMatches(a, categoryById.keySet());
            if (matches > bestMatches) {
                bestMatches = matches;
                valuesField = f;
                values = a;
            }
        }
        if (valuesField == null || values == null || bestMatches <= 0) return null;

        Field entriesField = null;
        CharSequence[] entries = null;
        for (Field f : arrays) {
            if (f == valuesField) continue;
            CharSequence[] a = (CharSequence[]) f.get(preference);
            if (a != null && a.length == values.length) {
                entriesField = f;
                entries = a;
                if (idMatches(a, categoryById.keySet()) == 0) break;
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
        boolean redLight = red >= 2 && red * 2 >= labels.length;
        if (cleanDisplay && redLight) entriesField.set(preference, labels);

        LinkedHashMap<String, Object> ordered = new LinkedHashMap<String, Object>();
        for (CharSequence value : values) {
            if (value == null) continue;
            Object category = categoryById.get(value.toString());
            if (category != null) ordered.put(value.toString(), category);
        }
        return new Group(labels, values.clone(), ordered, redLight);
    }

    private static HashSet<String> selectedIds(Group group) throws Exception {
        HashSet<String> out = new HashSet<String>();
        for (Map.Entry<String, Object> e : group.categoriesById.entrySet()) {
            if (getBooleanField(e.getValue(), "n")) out.add(e.getKey());
        }
        return out;
    }

    private static int visibleCount(Group group) throws Exception {
        return selectedIds(group).size();
    }

    private static List<Field> charSequenceArrays(Object o) throws Exception {
        ArrayList<Field> out = new ArrayList<Field>();
        for (Field f : allFields(o.getClass())) {
            if (!f.getType().equals(CharSequence[].class)) continue;
            f.setAccessible(true);
            if (f.get(o) instanceof CharSequence[]) out.add(f);
        }
        return out;
    }

    private static int idMatches(CharSequence[] a, Set<String> ids) {
        if (a == null) return -1;
        int n = 0;
        for (CharSequence x : a) if (x != null && ids.contains(x.toString())) n++;
        return n;
    }

    private static String cleanLabel(String label) {
        if (label == null) return "";
        while (label.startsWith("✓  ")) label = label.substring(3);
        label = label.replace("\uFFFC", "").replace('\u00A0', ' ');
        return label.replaceAll("[\\s\\u2007\\u202F]+$", "");
    }

    private static boolean getBooleanField(Object o, String name) throws Exception {
        Field f = findField(o.getClass(), name);
        if (f == null) throw new NoSuchFieldException(name);
        f.setAccessible(true);
        return f.getBoolean(o);
    }

    private static void setBooleanField(Object o, String name, boolean value) throws Exception {
        Field f = findField(o.getClass(), name);
        if (f == null) throw new NoSuchFieldException(name);
        f.setAccessible(true);
        f.setBoolean(o, value);
    }

    private static void setField(Object o, String name, Object value) throws Exception {
        Field f = findField(o.getClass(), name);
        if (f == null) throw new NoSuchFieldException(name);
        f.setAccessible(true);
        f.set(o, value);
    }

    private static Object invokeNoArg(Object o, String name) throws Exception {
        Method m = null;
        for (Class<?> c = o.getClass(); c != null && m == null; c = c.getSuperclass()) {
            try { m = c.getDeclaredMethod(name); }
            catch (NoSuchMethodException ignored) {}
        }
        if (m == null) throw new NoSuchMethodException(name);
        m.setAccessible(true);
        return m.invoke(o);
    }

    private static Field findField(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { return k.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) {}
        }
        return null;
    }

    private static List<Field> allFields(Class<?> c) {
        ArrayList<Field> out = new ArrayList<Field>();
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            Field[] fs = k.getDeclaredFields();
            Collections.addAll(out, fs);
        }
        return out;
    }

    private static final class Group {
        final CharSequence[] labels;
        final CharSequence[] values;
        final LinkedHashMap<String, Object> categoriesById;
        final boolean redLight;

        Group(CharSequence[] labels, CharSequence[] values,
              LinkedHashMap<String, Object> categoriesById, boolean redLight) {
            this.labels = labels;
            this.values = values;
            this.categoriesById = categoriesById;
            this.redLight = redLight;
        }
    }
}
