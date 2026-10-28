package com.spocky.projengmenu.embedded;

import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Presentation-only fix for Projectivy grouped channel preferences.
 *
 * Projectivy keeps every Android-TV channel visibility independently in the
 * category model, but its grouped ListPreference still paints only the last
 * clicked value as selected. This build's AndroidX preference API is R8-trimmed
 * and does not expose getEntries()/getEntryValues(), so discover the two actual
 * CharSequence[] fields by content instead of depending on method/field names.
 */
public final class ChannelVisualBridge {
    private static final String TAG = "ProjectivyChannels";
    private static final String CHECK = "✓  ";
    private ChannelVisualBridge() {}

    public static void decorateGroupedPreference(Object preference, List<?> categories) {
        if (preference == null || categories == null || categories.size() <= 1) return;
        try {
            Map<String, Boolean> visibleById = new HashMap<>();
            for (Object category : categories) {
                if (category == null) continue;
                Field id = findField(category.getClass(), "j");
                Field visible = findField(category.getClass(), "n");
                if (id == null || visible == null) continue;
                id.setAccessible(true);
                visible.setAccessible(true);
                visibleById.put(String.valueOf(id.getInt(category)), visible.getBoolean(category));
            }
            if (visibleById.isEmpty()) {
                Log.i(TAG, "CLEAN07C no category visibility state found");
                return;
            }

            List<Field> arrays = new ArrayList<>();
            for (Field f : allFields(preference.getClass())) {
                if (f.getType().equals(CharSequence[].class)) {
                    f.setAccessible(true);
                    if (f.get(preference) instanceof CharSequence[]) arrays.add(f);
                }
            }

            Field valuesField = null;
            CharSequence[] values = null;
            int bestMatches = -1;
            for (Field f : arrays) {
                CharSequence[] a = (CharSequence[]) f.get(preference);
                int matches = idMatches(a, visibleById.keySet());
                if (matches > bestMatches) {
                    bestMatches = matches;
                    valuesField = f;
                    values = a;
                }
            }
            if (valuesField == null || values == null || bestMatches <= 0) {
                Log.i(TAG, "CLEAN07C entry-values field not found arrays=" + arrays.size());
                return;
            }

            Field entriesField = null;
            CharSequence[] entries = null;
            for (Field f : arrays) {
                if (f == valuesField) continue;
                CharSequence[] a = (CharSequence[]) f.get(preference);
                if (a != null && a.length == values.length) {
                    entriesField = f;
                    entries = a;
                    if (idMatches(a, visibleById.keySet()) == 0) break;
                }
            }
            if (entriesField == null || entries == null) {
                Log.i(TAG, "CLEAN07C entries field not found arrays=" + arrays.size());
                return;
            }

            CharSequence[] decorated = new CharSequence[entries.length];
            int checked = 0;
            for (int i = 0; i < entries.length; i++) {
                String label = entries[i] == null ? "" : entries[i].toString();
                while (label.startsWith(CHECK)) label = label.substring(CHECK.length());
                String id = i < values.length && values[i] != null ? values[i].toString() : "";
                boolean active = Boolean.TRUE.equals(visibleById.get(id));
                if (active) checked++;
                decorated[i] = active ? TextUtils.concat(CHECK, label) : label;
            }
            entriesField.set(preference, decorated);

            // Clear only the ListPreference's single selected value. Prefer the
            // known R8 field q0; otherwise clear a String field whose current
            // value equals one of the entry values. Real visibility remains in i5.n.
            boolean cleared = false;
            Field selected = findField(preference.getClass(), "q0");
            if (selected != null && selected.getType().equals(String.class)) {
                selected.setAccessible(true);
                selected.set(preference, null);
                cleared = true;
            }
            if (!cleared) {
                Set<String> valueSet = new HashSet<>();
                for (CharSequence v : values) if (v != null) valueSet.add(v.toString());
                for (Field f : allFields(preference.getClass())) {
                    if (!f.getType().equals(String.class)) continue;
                    f.setAccessible(true);
                    Object v = f.get(preference);
                    if (v != null && valueSet.contains(v.toString())) {
                        f.set(preference, null);
                        cleared = true;
                        break;
                    }
                }
            }

            Log.i(TAG, "CLEAN07C grouped channels decorated count=" + entries.length
                    + " checked=" + checked + " selectedCleared=" + cleared
                    + " entriesField=" + entriesField.getName()
                    + " valuesField=" + valuesField.getName());
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07C grouped channel decoration failed", t);
        }
    }

    private static int idMatches(CharSequence[] a, Set<String> ids) {
        if (a == null) return -1;
        int n = 0;
        for (CharSequence x : a) if (x != null && ids.contains(x.toString())) n++;
        return n;
    }

    private static Field findField(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { return k.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) {}
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
}
