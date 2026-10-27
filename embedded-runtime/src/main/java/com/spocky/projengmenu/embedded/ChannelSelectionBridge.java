package com.spocky.projengmenu.embedded;

import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Converts Projectivy 4.72's grouped single-choice channel picker into a true
 * multi-select picker.  The checked values are derived from each category's live
 * visibility flag, so every row currently enabled on Home is represented.
 *
 * Reflection is intentionally limited to Projectivy's obfuscated category model;
 * AndroidX preference APIs remain normal/public APIs.
 */
public final class ChannelSelectionBridge {
    private ChannelSelectionBridge() {}

    public static Preference upgradeGroupedPreference(
            PreferenceGroup group, Preference original, List<?> categories) {
        if (group == null || original == null || categories == null || categories.size() <= 1) {
            return original;
        }
        if (!(original instanceof ListPreference)) return original;

        try {
            ListPreference source = (ListPreference) original;
            MultiSelectListPreference multi = new MultiSelectListPreference(original.getContext());
            multi.setPersistent(false);
            multi.setKey(source.getKey());
            multi.setTitle(source.getTitle());
            multi.setSummary(source.getSummary());
            multi.setOrder(source.getOrder());
            multi.setEnabled(source.isEnabled());
            multi.setSelectable(source.isSelectable());
            multi.setIcon(source.getIcon());
            multi.setEntries(source.getEntries());
            multi.setEntryValues(source.getEntryValues());

            ArrayList<Object> refs = new ArrayList<>(categories);
            HashSet<String> selected = new HashSet<>();
            for (Object category : refs) {
                int id = categoryId(category);
                if (id != Integer.MIN_VALUE && categoryVisible(category)) {
                    selected.add(String.valueOf(id));
                }
            }
            multi.setValues(selected);

            multi.setOnPreferenceChangeListener((preference, newValue) -> {
                if (!(newValue instanceof Set)) return false;
                HashSet<String> next = new HashSet<>();
                for (Object value : (Set<?>) newValue) {
                    if (value != null) next.add(String.valueOf(value));
                }

                boolean changed = false;
                for (Object category : refs) {
                    int id = categoryId(category);
                    if (id == Integer.MIN_VALUE) continue;
                    boolean wanted = next.contains(String.valueOf(id));
                    if (categoryVisible(category) != wanted) {
                        setCategoryVisible(category, wanted);
                        changed = true;
                    }
                }
                if (changed) persistAndNotifyProjectivy();
                return true;
            });

            // Remove the old ListPreference before adding the replacement because
            // both intentionally use the same Projectivy key.
            group.removePreference(original);
            group.addPreference(multi);
            return multi;
        } catch (Throwable ignored) {
            return original;
        }
    }

    private static int categoryId(Object category) {
        if (category == null) return Integer.MIN_VALUE;
        try {
            Field field = category.getClass().getField("j");
            return field.getInt(category);
        } catch (Throwable ignored) {
            return Integer.MIN_VALUE;
        }
    }

    private static boolean categoryVisible(Object category) {
        if (category == null) return false;
        try {
            Field field = category.getClass().getField("n");
            return field.getBoolean(category);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void setCategoryVisible(Object category, boolean visible) {
        try {
            Field field = category.getClass().getField("n");
            field.setBoolean(category, visible);
        } catch (Throwable ignored) {}
    }

    private static void persistAndNotifyProjectivy() {
        try {
            Class<?> categories = Class.forName("w40");
            Method persist = categories.getDeclaredMethod("u");
            persist.setAccessible(true);
            persist.invoke(null);
            Method notify = categories.getDeclaredMethod("m");
            notify.setAccessible(true);
            notify.invoke(null);
        } catch (Throwable ignored) {}
    }
}
