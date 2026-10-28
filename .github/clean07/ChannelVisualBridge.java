package com.spocky.projengmenu.embedded;

import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Presentation-only fix for Projectivy grouped channel preferences.
 *
 * Projectivy already keeps each Android-TV channel visibility independently in
 * the category model. Its grouped ListPreference, however, renders one selected
 * value and can make a multi-channel provider look like a single channel.
 *
 * Keep the stock preference/click callback and decorate only its entries from
 * the real category visibility state.
 */
public final class ChannelVisualBridge {
    private static final String TAG = "ProjectivyChannels";
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

            // q0 is only the misleading last-opened radio/list value. The actual
            // visibility of every category remains in each category's n field.
            try {
                Field selected = preference.getClass().getField("q0");
                selected.set(preference, null);
            } catch (NoSuchFieldException ignored) {
                Field selected = preference.getClass().getDeclaredField("q0");
                selected.setAccessible(true);
                selected.set(preference, null);
            }

            Log.i(TAG, "CLEAN07B grouped channels decorated count=" + entries.length);
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07B grouped channel decoration failed", t);
        }
    }
}
