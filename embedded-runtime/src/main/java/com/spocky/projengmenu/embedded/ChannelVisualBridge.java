package com.spocky.projengmenu.embedded;

import android.text.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Visual-only fix for Projectivy grouped channel preferences. */
public final class ChannelVisualBridge {
    private static final Map<Object, Boolean> GROUPED =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ChannelVisualBridge() {}

    /**
     * Projectivy intentionally uses a single-value ListPreference for a provider
     * containing several TV channels, even though each channel has independent
     * visibility state. Keep its working callback/model untouched; only make every
     * currently-visible channel visibly checked and clear the misleading single
     * radio selection.
     */
    public static void decorateGroupedPreference(Object preference, List<?> categories) {
        if (preference == null || categories == null || categories.size() <= 1) return;
        try {
            Method getEntries = preference.getClass().getMethod("getEntries");
            Method getEntryValues = preference.getClass().getMethod("getEntryValues");
            Method setEntries = preference.getClass().getMethod("setEntries", CharSequence[].class);
            Method setValue = preference.getClass().getMethod("setValue", String.class);

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
                if (Boolean.TRUE.equals(visibleById.get(id))) {
                    decorated[i] = TextUtils.concat("✓  ", entries[i]);
                } else {
                    decorated[i] = entries[i];
                }
            }
            setEntries.invoke(preference, (Object) decorated);
            GROUPED.put(preference, Boolean.TRUE);

            // This value controls only the one-radio-button rendering. Channel
            // visibility itself lives on each category and is left untouched.
            setValue.invoke(preference, new Object[]{null});
        } catch (Throwable ignored) {}
    }

    /** Used from Projectivy's stock dialog click path to suppress only the stale
     * single-value radio state for preferences decorated above. */
    public static boolean isGroupedChannelPreference(Object preference) {
        if (preference == null) return false;
        try { return Boolean.TRUE.equals(GROUPED.get(preference)); }
        catch (Throwable ignored) { return false; }
    }
}
