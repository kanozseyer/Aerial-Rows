package com.spocky.projengmenu.embedded;

import android.text.TextUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Visual-only checked-state fix for Projectivy grouped channel preferences. */
public final class ChannelVisualBridge {
    private ChannelVisualBridge() {}

    public static void decorateGroupedPreference(Object preference, List<?> categories) {
        if (preference == null || categories == null || categories.size() <= 1) return;
        try {
            // Projectivy 4.72's bundled AndroidX ListPreference is R8-obfuscated:
            // o0 = entries, p0 = entryValues, q0 = selected value.
            Class<?> cls = preference.getClass();
            Field entriesField = cls.getField("o0");
            Field valuesField = cls.getField("p0");
            Field selectedField = cls.getField("q0");

            CharSequence[] entries = (CharSequence[]) entriesField.get(preference);
            CharSequence[] values = (CharSequence[]) valuesField.get(preference);
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
            entriesField.set(preference, decorated);

            // The stock ListPreference remembers only the last row opened. That is
            // not the real visibility state; each channel uses its own i5.n above.
            selectedField.set(preference, null);
        } catch (Throwable t) {
            android.util.Log.e("ProjectivyChannels", "Grouped channel state decoration failed", t);
        }
    }
}
