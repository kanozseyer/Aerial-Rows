package com.spocky.projengmenu.embedded;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * CLEAN 07E presentation-only correction.
 *
 * CLEAN 07D proved that Projectivy already persists each underlying Android-TV
 * channel independently, but it added a second AlertDialog on top of Projectivy's
 * own grouped-channel selector. 07E keeps Projectivy's original selector as the
 * only dialog. When that stock ListPreference dialog opens, its own ListView is
 * switched to multiple-choice mode and its item click listener is replaced with
 * the already-proven Projectivy preference-change callback. This keeps the stock
 * Projectivy styling/navigation while allowing several Red Light rows to remain
 * selected simultaneously.
 */
public final class ChannelVisualBridge {
    private static final String TAG = "ProjectivyChannels";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ChannelVisualBridge() {}

    public static void decorateGroupedPreference(final Object preference, final List<?> categories) {
        if (preference == null || categories == null || categories.size() <= 1) return;
        try {
            Snapshot s = snapshot(preference, categories, true);
            if (s == null || !s.redLightGroup) return;

            final Context context = findContext(preference);
            final ChangeInvoker change = findChangeInvoker(preference);
            if (!(context instanceof Activity)) {
                Log.e(TAG, "CLEAN07E no Activity Context; leaving stock selector intact");
                return;
            }
            if (change == null) {
                Log.e(TAG, "CLEAN07E no stock preference-change callback; leaving stock selector intact");
                return;
            }

            boolean installed = installClickObserver(preference, new ClickAction() {
                @Override public boolean onClick() {
                    // Return false so Projectivy/AndroidX continues with its normal
                    // ListPreference dialog. We only upgrade that dialog afterwards.
                    scheduleStockDialogUpgrade((Activity) context, preference, categories, change, 0);
                    return false;
                }
            });

            if (installed) {
                Log.i(TAG, "CLEAN07E stock selector observer bound count=" + s.labels.length
                        + " checked=" + countChecked(s.checked)
                        + " entriesField=" + s.entriesField
                        + " valuesField=" + s.valuesField);
            } else {
                Log.e(TAG, "CLEAN07E click observer not found; leaving stock selector intact");
            }
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07E grouped channel binding failed", t);
        }
    }

    private static void scheduleStockDialogUpgrade(final Activity activity,
                                                   final Object preference,
                                                   final List<?> categories,
                                                   final ChangeInvoker change,
                                                   final int attempt) {
        long delay = attempt == 0 ? 60L : 100L;
        MAIN.postDelayed(new Runnable() {
            @Override public void run() {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                try {
                    Snapshot s = snapshot(preference, categories, true);
                    if (s == null) return;
                    AlertDialog dialog = findMatchingPreferenceDialog(activity, s.labels.length);
                    if (dialog == null) {
                        if (attempt < 8) scheduleStockDialogUpgrade(activity, preference, categories, change, attempt + 1);
                        else Log.e(TAG, "CLEAN07E stock dialog not found");
                        return;
                    }
                    upgradeStockDialog(dialog, preference, categories, change);
                } catch (Throwable t) {
                    Log.e(TAG, "CLEAN07E stock dialog upgrade failed", t);
                }
            }
        }, delay);
    }

    private static AlertDialog findMatchingPreferenceDialog(Activity activity, int expectedCount) {
        try {
            Method fmMethod = findNoArgMethod(activity.getClass(), "getSupportFragmentManager");
            if (fmMethod == null) return null;
            fmMethod.setAccessible(true);
            Object fm = fmMethod.invoke(activity);
            if (fm == null) return null;

            Method fragmentsMethod = findNoArgMethod(fm.getClass(), "getFragments");
            if (fragmentsMethod == null) return null;
            fragmentsMethod.setAccessible(true);
            Object raw = fragmentsMethod.invoke(fm);
            if (!(raw instanceof List)) return null;

            List<?> fragments = (List<?>) raw;
            for (int i = fragments.size() - 1; i >= 0; i--) {
                Object f = fragments.get(i);
                if (f == null) continue;
                Dialog d = dialogFromFragment(f);
                if (!(d instanceof AlertDialog) || !d.isShowing()) continue;
                AlertDialog a = (AlertDialog) d;
                ListView list = a.getListView();
                ListAdapter adapter = list == null ? null : list.getAdapter();
                int count = adapter == null ? -1 : adapter.getCount();
                if (count == expectedCount) return a;
            }
        } catch (Throwable t) {
            Log.i(TAG, "CLEAN07E fragment dialog lookup unavailable");
        }
        return null;
    }

    private static Dialog dialogFromFragment(Object fragment) {
        try {
            for (Method m : allMethods(fragment.getClass())) {
                if (m.getParameterTypes().length != 0) continue;
                if (!Dialog.class.isAssignableFrom(m.getReturnType())) continue;
                m.setAccessible(true);
                Object v = m.invoke(fragment);
                if (v instanceof Dialog) return (Dialog) v;
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void upgradeStockDialog(final AlertDialog dialog,
                                           final Object preference,
                                           final List<?> categories,
                                           final ChangeInvoker change) throws Exception {
        final Snapshot s = snapshot(preference, categories, true);
        if (s == null) return;
        final ListView list = dialog.getListView();
        if (list == null || list.getAdapter() == null || list.getAdapter().getCount() != s.labels.length) {
            throw new IllegalStateException("unexpected stock list shape");
        }

        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        list.clearChoices();
        for (int i = 0; i < s.checked.length; i++) list.setItemChecked(i, s.checked[i]);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= s.values.length) return;
                boolean requested = list.isItemChecked(position);
                String value = s.values[position] == null ? "" : s.values[position].toString();
                boolean accepted = false;
                try {
                    accepted = change.invoke(value);
                } catch (Throwable t) {
                    Log.e(TAG, "CLEAN07E stock channel toggle failed value=" + value, t);
                }
                if (!accepted) list.setItemChecked(position, !requested);
                Log.i(TAG, "CLEAN07E toggle value=" + value
                        + " requested=" + requested + " accepted=" + accepted);
            }
        });

        Log.i(TAG, "CLEAN07E stock dialog upgraded count=" + s.labels.length
                + " checked=" + countChecked(s.checked));
    }

    private static Snapshot snapshot(Object preference, List<?> categories, boolean cleanEntries) throws Exception {
        LinkedHashMap<String, Boolean> visibleById = new LinkedHashMap<>();
        for (Object category : categories) {
            if (category == null) continue;
            Field id = findField(category.getClass(), "j");
            Field visible = findField(category.getClass(), "n");
            if (id == null || visible == null) continue;
            id.setAccessible(true);
            visible.setAccessible(true);
            visibleById.put(String.valueOf(id.getInt(category)), visible.getBoolean(category));
        }
        if (visibleById.isEmpty()) return null;

        List<Field> arrays = new ArrayList<>();
        for (Field f : allFields(preference.getClass())) {
            if (!f.getType().equals(CharSequence[].class)) continue;
            f.setAccessible(true);
            if (f.get(preference) instanceof CharSequence[]) arrays.add(f);
        }

        Field valuesField = null;
        CharSequence[] values = null;
        int bestMatches = -1;
        Set<String> ids = visibleById.keySet();
        for (Field f : arrays) {
            CharSequence[] a = (CharSequence[]) f.get(preference);
            int matches = idMatches(a, ids);
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
                if (idMatches(a, ids) == 0) break;
            }
        }
        if (entriesField == null || entries == null) return null;

        CharSequence[] labels = new CharSequence[entries.length];
        boolean[] checked = new boolean[entries.length];
        int redLight = 0;
        for (int i = 0; i < entries.length; i++) {
            String label = cleanLabel(entries[i] == null ? "" : entries[i].toString());
            labels[i] = label;
            if (label.startsWith("Red Light")) redLight++;

            String value = i < values.length && values[i] != null ? values[i].toString() : "";
            checked[i] = Boolean.TRUE.equals(visibleById.get(value));
        }

        if (cleanEntries) {
            // Replace only the displayed entry strings. IDs/values and the real
            // per-category visibility state remain untouched.
            entriesField.set(preference, labels);
        }

        boolean redLightGroup = redLight >= 2 && redLight * 2 >= labels.length;
        return new Snapshot(labels, values, checked, redLightGroup,
                entriesField.getName(), valuesField.getName());
    }

    private static String cleanLabel(String label) {
        if (label == null) return "";
        while (label.startsWith("✓  ")) label = label.substring(3);
        // U+FFFC is Android's Object Replacement Character. It came from a styled
        // span in Projectivy's original entry and became visible only when CLEAN07D
        // flattened the styled text. It is not part of the channel title/state.
        label = label.replace("\uFFFC", "").replace('\u00A0', ' ');
        return label.replaceAll("[\\s\\u2007\\u202F]+$", "");
    }

    private static Context findContext(Object preference) {
        Context fallback = null;
        try {
            for (Field f : allFields(preference.getClass())) {
                if (!Context.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object v = f.get(preference);
                if (v instanceof Activity) return (Context) v;
                if (v instanceof Context && fallback == null) fallback = (Context) v;
            }
        } catch (Throwable ignored) {}
        return fallback;
    }

    private static ChangeInvoker findChangeInvoker(final Object preference) {
        try {
            for (Field f : allFields(preference.getClass())) {
                Class<?> t = f.getType();
                if (!t.isInterface()) continue;
                Method callback = findBooleanCallback(t, 2, preference.getClass());
                if (callback == null) continue;
                f.setAccessible(true);
                final Object listener = f.get(preference);
                if (listener == null) continue;
                callback.setAccessible(true);
                final Method cb = callback;
                return new ChangeInvoker() {
                    @Override public boolean invoke(String value) throws Exception {
                        Object r = cb.invoke(listener, preference, value);
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
            Log.e(TAG, "CLEAN07E change-callback discovery failed", t);
        }
        return null;
    }

    private static boolean installClickObserver(final Object preference, final ClickAction action) {
        try {
            for (Method m : allMethods(preference.getClass())) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getReturnType() != void.class || p.length != 1 || !p[0].isInterface()) continue;
                if (findBooleanCallback(p[0], 1, preference.getClass()) == null) continue;
                Object proxy = makeClickProxy(p[0], preference, action);
                m.setAccessible(true);
                m.invoke(preference, proxy);
                return true;
            }
            for (Field f : allFields(preference.getClass())) {
                Class<?> t = f.getType();
                if (!t.isInterface() || findBooleanCallback(t, 1, preference.getClass()) == null) continue;
                f.setAccessible(true);
                f.set(preference, makeClickProxy(t, preference, action));
                return true;
            }
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07E click-observer discovery failed", t);
        }
        return false;
    }

    private static Object makeClickProxy(final Class<?> listenerType,
                                         final Object preference,
                                         final ClickAction action) {
        ClassLoader cl = listenerType.getClassLoader();
        if (cl == null) cl = preference.getClass().getClassLoader();
        return Proxy.newProxyInstance(cl, new Class<?>[]{listenerType}, new InvocationHandler() {
            @Override public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getDeclaringClass() == Object.class) {
                    String n = method.getName();
                    if ("toString".equals(n)) return "CLEAN07EChannelClickProxy";
                    if ("hashCode".equals(n)) return System.identityHashCode(proxy);
                    if ("equals".equals(n)) return proxy == (args == null ? null : args[0]);
                }
                if ((method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)
                        && method.getParameterTypes().length == 1) {
                    return action.onClick();
                }
                return null;
            }
        });
    }

    private static Method findNoArgMethod(Class<?> c, String name) {
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            try { return k.getDeclaredMethod(name); }
            catch (NoSuchMethodException ignored) {}
        }
        return null;
    }

    private static Method findBooleanCallback(Class<?> iface, int params, Class<?> preferenceClass) {
        try {
            for (Method m : iface.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if ((m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)
                        && p.length == params
                        && p.length > 0
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

    private static int countChecked(boolean[] a) {
        int n = 0;
        if (a != null) for (boolean b : a) if (b) n++;
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

    private static List<Method> allMethods(Class<?> c) {
        ArrayList<Method> out = new ArrayList<>();
        for (Class<?> k = c; k != null; k = k.getSuperclass()) {
            Method[] ms = k.getDeclaredMethods();
            for (Method m : ms) out.add(m);
        }
        return out;
    }

    private interface ClickAction { boolean onClick(); }
    private interface ChangeInvoker { boolean invoke(String value) throws Exception; }

    private static final class Snapshot {
        final CharSequence[] labels;
        final CharSequence[] values;
        final boolean[] checked;
        final boolean redLightGroup;
        final String entriesField;
        final String valuesField;
        Snapshot(CharSequence[] labels, CharSequence[] values, boolean[] checked,
                 boolean redLightGroup, String entriesField, String valuesField) {
            this.labels = labels;
            this.values = values;
            this.checked = checked;
            this.redLightGroup = redLightGroup;
            this.entriesField = entriesField;
            this.valuesField = valuesField;
        }
    }
}
