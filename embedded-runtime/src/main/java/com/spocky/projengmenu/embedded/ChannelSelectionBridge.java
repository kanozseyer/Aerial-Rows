package com.spocky.projengmenu.embedded;

import android.util.Log;

import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Builds Projectivy grouped channel providers as a real multi-select from the start. */
public final class ChannelSelectionBridge {
    private static final String TAG = "ProjectivyChannels";

    private ChannelSelectionBridge() {}

    public static Preference buildGroupedPreference(
            PreferenceGroup group, String sourceTitle, List<?> categories) {
        if (group == null || sourceTitle == null || categories == null || categories.size() <= 1) {
            return null;
        }

        try {
            final String key = "key-" + sourceTitle;
            Preference existing = group.findPreference(key);
            MultiSelectListPreference multi;
            if (existing instanceof MultiSelectListPreference) {
                multi = (MultiSelectListPreference) existing;
            } else {
                if (existing != null) group.removePreference(existing);
                multi = new MultiSelectListPreference(group.getContext());
                multi.setPersistent(false);
                multi.setKey(key);
                group.addPreference(multi);
            }

            ArrayList<Object> refs = new ArrayList<>();
            for (Object category : categories) if (category != null) refs.add(category);
            refs.sort(Comparator.comparing(ChannelSelectionBridge::categoryTitle,
                    String.CASE_INSENSITIVE_ORDER));

            CharSequence[] entries = new CharSequence[refs.size()];
            CharSequence[] values = new CharSequence[refs.size()];
            HashSet<String> selected = new HashSet<>();
            for (int i = 0; i < refs.size(); i++) {
                Object category = refs.get(i);
                int id = categoryId(category);
                entries[i] = categoryTitle(category);
                values[i] = String.valueOf(id);
                if (id != Integer.MIN_VALUE && categoryVisible(category)) {
                    selected.add(String.valueOf(id));
                }
            }

            multi.setTitle(sourceTitle);
            multi.setSummary(refs.size() + " channels");
            multi.setEntries(entries);
            multi.setEntryValues(values);
            multi.setValues(selected);
            multi.setEnabled(true);
            multi.setSelectable(true);

            final ArrayList<Object> callbackRefs = refs;
            multi.setOnPreferenceChangeListener((preference, newValue) -> {
                if (!(newValue instanceof Set)) return false;
                HashSet<String> next = new HashSet<>();
                for (Object value : (Set<?>) newValue) {
                    if (value != null) next.add(String.valueOf(value));
                }

                boolean changed = false;
                for (Object category : callbackRefs) {
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
            return multi;
        } catch (Throwable t) {
            Log.e(TAG, "Unable to build grouped channel multi-select for " + sourceTitle, t);
            return null;
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

    private static String categoryTitle(Object category) {
        if (category == null) return "";
        try {
            Method method = category.getClass().getMethod("h");
            Object value = method.invoke(category);
            if (value != null) return String.valueOf(value);
        } catch (Throwable ignored) {}
        try {
            Field field = category.getClass().getField("t");
            Object value = field.get(category);
            if (value != null) return String.valueOf(value);
        } catch (Throwable ignored) {}
        return "Channel " + categoryId(category);
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
        } catch (Throwable t) {
            Log.e(TAG, "Unable to update channel visibility", t);
        }
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
        } catch (Throwable t) {
            Log.e(TAG, "Unable to persist Projectivy channel visibility", t);
        }
    }
}
