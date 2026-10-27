package com.spocky.projengmenu.embedded;

import android.text.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Visual-only checked-state fix for Projectivy grouped channel preferences. */
public final class ChannelVisualBridge {
    private ChannelVisualBridge() {}

    public static void decorateGroupedPreference(Object preference, List<?> categories) {
        if (preference == null || categories == null || categories.size() <= 1) return;
        try {
            Method getEntries = preference.getClass().getMethod("getEntries");
            Method getEntryValues = preference.getClass().getMethod("getEntryValues");
            Method setEntries = preference.getClass().getMethod("setEntries", CharSequence[].class);

            CharSequence[] entries = (CharSequence[]) getEntries.invoke(preference);
            CharSequence[] values = (CharSequence[]) getEntryValues.invoke(preference);
            if (entries == null || values == null || entries.length != values.length) return;

            Map<String, Boolean> visibleById = new HashMap<>();
            for (Object category : categories) {
                if (category == null) continue;
                Field id = category.getClass().getDeclaredField("j");
                Field visible = category.getClass().getDeclaredField("n");
                id.setAccessible(true);
                visible.setAccessible(true);
                visibleById.put(String.valueOf(id.getInt(category)), visible.getBoolean(category));
            }

            CharSequence[] decorated = new CharSequence[entries.length];
            for (int i = 0; i < entries.length; i++) {
                String id = String.valueOf(values[i]);
                decorated[i] = Boolean.TRUE.equals(visibleById.get(id))
                        ? TextUtils.concat("✓  ", entries[i]) : entries[i];
            }
            setEntries.invoke(preference, (Object) decorated);

            // Stock Projectivy stores the last opened row in ListPreference.q0,
            // which renders one misleading radio selection. Visibility is actually
            // held independently by each category's i5.n, so clear only this visual value.
            Field selected = preference.getClass().getField("q0");
            selected.set(preference, null);
        } catch (Throwable t) {
            android.util.Log.e("ProjectivyChannels", "Grouped channel state decoration failed", t);
        }
    }
}
