package com.spocky.projengmenu.embedded;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CLEAN 07D presentation-only fix for Projectivy grouped channel preferences.
 *
 * Projectivy already stores each Android-TV channel visibility independently,
 * but the stock grouped control is a single-choice ListPreference.  Do not fake
 * multi-selection by changing channel names.  Intercept only the Red Light
 * grouped preference and show a real Android multi-choice dialog with one
 * checkbox per underlying channel.  Every toggle is forwarded through
 * Projectivy's existing preference-change callback, so the real channel model
 * remains authoritative.
 */
public final class ChannelVisualBridge {
    private static final String TAG = "ProjectivyChannels";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ChannelVisualBridge() {}

    public static void decorateGroupedPreference(final Object preference, final List<?> categories) {
        if (preference == null || categories == null || categories.size() <= 1) return;
        try {
            Snapshot s = snapshot(preference, categories);
            if (s == null || !s.redLightGroup) return;

            // Bind after Projectivy finishes constructing the preference so our
            // click listener is installed after its stock single-choice wiring.
            MAIN.post(new Runnable() {
                @Override public void run() {
                    bindRealMultiChoice(preference, categories);
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07D grouped channel binding failed", t);
        }
    }

    private static void bindRealMultiChoice(final Object preference, final List<?> categories) {
        try {
            final Snapshot s = snapshot(preference, categories);
            if (s == null || !s.redLightGroup) return;

            final Context context = findContext(preference);
            final ChangeInvoker change = findChangeInvoker(preference);
            if (context == null) {
                Log.e(TAG, "CLEAN07D no preference Context; leaving stock selector intact");
                return;
            }
            if (change == null) {
                Log.e(TAG, "CLEAN07D no stock preference-change callback; leaving stock selector intact");
                return;
            }

            boolean installed = installClickInterceptor(preference, new ClickAction() {
                @Override public boolean onClick() {
                    showMultiChoiceDialog(context, preference, categories, change);
                    return true;
                }
            });

            if (installed) {
                Log.i(TAG, "CLEAN07D multi-choice bound count=" + s.labels.length
                        + " checked=" + countChecked(s.checked)
                        + " entriesField=" + s.entriesField
                        + " valuesField=" + s.valuesField);
            } else {
                Log.e(TAG, "CLEAN07D click interceptor not found; leaving stock selector intact");
            }
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07D grouped channel binding failed", t);
        }
    }

    private static void showMultiChoiceDialog(final Context context,
                                              final Object preference,
                                              final List<?> categories,
                                              final ChangeInvoker change) {
        try {
            final Snapshot s = snapshot(preference, categories);
            if (s == null) return;

            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("Red Light channels")
                    .setMultiChoiceItems(s.labels, s.checked,
                            new DialogInterface.OnMultiChoiceClickListener() {
                                @Override public void onClick(final DialogInterface d,
                                                              final int which,
                                                              final boolean isChecked) {
                                    if (which < 0 || which >= s.values.length) return;
                                    String value = s.values[which] == null ? "" : s.values[which].toString();
                                    boolean accepted = false;
                                    try {
                                        accepted = change.invoke(value);
                                    } catch (Throwable t) {
                                        Log.e(TAG, "CLEAN07D stock channel toggle failed value=" + value, t);
                                    }

                                    if (!accepted && d instanceof AlertDialog) {
                                        ((AlertDialog) d).getListView().setItemChecked(which, !isChecked);
                                    }
                                    Log.i(TAG, "CLEAN07D toggle value=" + value
                                            + " requested=" + isChecked
                                            + " accepted=" + accepted);
                                }
                            })
                    .setPositiveButton("Done", null)
                    .create();

            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override public void onDismiss(DialogInterface d) {
                    try {
                        Snapshot now = snapshot(preference, categories);
                        Log.i(TAG, "CLEAN07D dialog dismissed checked="
                                + (now == null ? -1 : countChecked(now.checked)));
                    } catch (Throwable ignored) {}
                }
            });
            dialog.show();
            Log.i(TAG, "CLEAN07D dialog shown count=" + s.labels.length
                    + " checked=" + countChecked(s.checked));
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07D dialog failed", t);
        }
    }

    private static Snapshot snapshot(Object preference, List<?> categories) throws Exception {
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
            String label = entries[i] == null ? "" : entries[i].toString();
            // Strip the old CLEAN07C text-only marker if this Preference instance
            // happened to survive an in-process update/rebuild.
            while (label.startsWith("✓  ")) label = label.substring(3);
            labels[i] = label;
            if (label.startsWith("Red Light")) redLight++;

            String value = i < values.length && values[i] != null ? values[i].toString() : "";
            checked[i] = Boolean.TRUE.equals(visibleById.get(value));
        }

        boolean redLightGroup = redLight >= 2 && redLight * 2 >= labels.length;
        return new Snapshot(labels, values, checked, redLightGroup,
                entriesField.getName(), valuesField.getName());
    }

    private static Context findContext(Object preference) {
        Context fallback = null;
        try {
            for (Field f : allFields(preference.getClass())) {
                if (!Context.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object v = f.get(preference);
                if (v instanceof android.app.Activity) return (Context) v;
                if (v instanceof Context && fallback == null) fallback = (Context) v;
            }
        } catch (Throwable ignored) {}
        return fallback;
    }

    private static ChangeInvoker findChangeInvoker(final Object preference) {
        try {
            // Prefer Projectivy/AndroidX's existing OnPreferenceChangeListener field.
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

            // Fallback to Preference.callChangeListener(Object), even if R8 renamed it.
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
            Log.e(TAG, "CLEAN07D change-callback discovery failed", t);
        }
        return null;
    }

    private static boolean installClickInterceptor(final Object preference, final ClickAction action) {
        try {
            // Find a setter accepting the one-argument boolean click-listener interface.
            for (Method m : allMethods(preference.getClass())) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getReturnType() != void.class || p.length != 1 || !p[0].isInterface()) continue;
                if (findBooleanCallback(p[0], 1, preference.getClass()) == null) continue;
                Object proxy = makeClickProxy(p[0], preference, action);
                m.setAccessible(true);
                m.invoke(preference, proxy);
                return true;
            }

            // Fallback: set the click-listener field directly.
            for (Field f : allFields(preference.getClass())) {
                Class<?> t = f.getType();
                if (!t.isInterface() || findBooleanCallback(t, 1, preference.getClass()) == null) continue;
                f.setAccessible(true);
                f.set(preference, makeClickProxy(t, preference, action));
                return true;
            }
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN07D click-interceptor discovery failed", t);
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
                    if ("toString".equals(n)) return "CLEAN07DChannelClickProxy";
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

    private static Method findBooleanCallback(Class<?> iface, int params, Class<?> preferenceClass) {
        try {
            for (Method m : iface.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if ((m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)
                        && p.length == params
                        && p.length > 0
                        && p[0].isAssignableFrom(preferenceClass)) {
                    return m;
                }
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
