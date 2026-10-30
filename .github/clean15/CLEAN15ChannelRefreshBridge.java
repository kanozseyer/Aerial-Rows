package com.spocky.projengmenu.embedded;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CLEAN15: adds a native-style refresh action to each channel-backed
 * Projectivy left navigation header. Existing app/category rows are untouched.
 *
 * Projectivy's native refresh entrypoint is global (w40.z(false)): it clears
 * its TV channel, Watch Next and per-channel Preview Program caches and
 * rebuilds the home model. Red Light rows additionally request a fresh Kodi
 * discovery pass, but only when Kodi's native JSON-RPC endpoint is already
 * ready. This bridge never starts or foregrounds Kodi.
 */
public final class CLEAN15ChannelRefreshBridge {
    private static final String TAG = "CLEAN15ChannelRefresh";
    private static final String RED_LIGHT_PREFIX = "redlight:";

    private static final WeakHashMap<ViewGroup, ImageView> BUTTONS =
            new WeakHashMap<>();
    private static final AtomicBoolean REFRESH_RUNNING = new AtomicBoolean(false);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private CLEAN15ChannelRefreshBridge() {
    }

    public static void install(ViewGroup header) {
        if (!(header instanceof LinearLayout) || header.getContext() == null) return;

        synchronized (BUTTONS) {
            if (BUTTONS.containsKey(header)) return;

            try {
                Method getter = header.getClass().getMethod("getSettingsView");
                Object settingsObject = getter.invoke(header);
                if (!(settingsObject instanceof ImageView)) return;

                ImageView settings = (ImageView) settingsObject;
                Class<?> iconClass = Class.forName(
                        "com.spocky.projengmenu.ui.home.header.IconHeaderImageView");
                Object instance = iconClass
                        .getConstructor(Context.class)
                        .newInstance(header.getContext());
                if (!(instance instanceof ImageView)) return;

                final ImageView button = (ImageView) instance;
                ViewGroup.LayoutParams original = settings.getLayoutParams();
                LinearLayout.LayoutParams params;
                if (original instanceof LinearLayout.LayoutParams) {
                    params = new LinearLayout.LayoutParams(
                            (LinearLayout.LayoutParams) original);
                } else {
                    params = new LinearLayout.LayoutParams(
                            original.width, original.height);
                }

                Drawable background = settings.getBackground();
                if (background != null) {
                    Drawable.ConstantState state = background.getConstantState();
                    Drawable copy = state != null
                            ? state.newDrawable(header.getResources()).mutate()
                            : background.mutate();
                    button.setBackground(copy);
                }

                button.setPadding(
                        settings.getPaddingLeft(),
                        settings.getPaddingTop(),
                        settings.getPaddingRight(),
                        settings.getPaddingBottom());
                button.setScaleType(settings.getScaleType());
                button.setImageResource(android.R.drawable.ic_popup_sync);
                button.setFocusable(true);
                button.setFocusableInTouchMode(true);
                button.setClickable(true);
                button.setContentDescription("Refresh channel");
                button.setAlpha(0.5f);
                button.setVisibility(View.GONE);

                int settingsIndex = header.indexOfChild(settings);
                if (settingsIndex < 0) return;
                header.addView(button, settingsIndex + 1, params);

                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        refresh(header, button);
                    }
                });

                BUTTONS.put(header, button);
                Log.i(TAG, "CLEAN15 channel refresh action installed");
            } catch (Throwable error) {
                Log.e(TAG, "CLEAN15 unable to install channel refresh action", error);
            }
        }
    }

    public static void bind(ViewGroup header, Object categoryTag) {
        ImageView button = buttonFor(header);
        if (button == null || categoryTag == null) return;

        try {
            Object model = readField(categoryTag, "c");
            if (model == null) return;

            Method isChannelMethod = model.getClass().getMethod("j");
            boolean isChannel = Boolean.TRUE.equals(isChannelMethod.invoke(model));
            button.setTag(categoryTag);

            String title = stringField(categoryTag, "b");
            button.setContentDescription(
                    title == null || title.trim().isEmpty()
                            ? "Refresh channel"
                            : "Refresh " + title);

            ArrayList<View> focusItems = focusItems(header);
            if (isChannel) {
                if (focusItems != null && !focusItems.contains(button)) {
                    int headerIndex = focusItems.indexOf(header);
                    if (headerIndex < 0) headerIndex = focusItems.size();
                    focusItems.add(headerIndex, button);
                }

                if (header.isSelected()) {
                    button.setVisibility(View.VISIBLE);
                    button.setAlpha(button.hasFocus() ? 1.0f : 0.5f);
                } else {
                    button.setVisibility(View.INVISIBLE);
                    button.setAlpha(0.5f);
                }
                Log.i(TAG, "CLEAN15 channel row refresh enabled title=" + title);
            } else {
                if (focusItems != null) focusItems.remove(button);
                if (button.hasFocus()) header.requestFocus();
                button.setVisibility(View.GONE);
            }
        } catch (Throwable error) {
            Log.e(TAG, "CLEAN15 unable to bind channel refresh action", error);
        }
    }

    public static void tint(ViewGroup header, int color) {
        ImageView button = buttonFor(header);
        if (button == null) return;
        try {
            Method tint = button.getClass().getMethod("a", int.class);
            tint.invoke(button, color);
        } catch (Throwable error) {
            Log.w(TAG, "CLEAN15 refresh icon tint could not be updated", error);
        }
    }

    private static ImageView buttonFor(ViewGroup header) {
        synchronized (BUTTONS) {
            return BUTTONS.get(header);
        }
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<View> focusItems(ViewGroup header) throws Exception {
        Field field = header.getClass().getField("C");
        Object value = field.get(header);
        return value instanceof ArrayList ? (ArrayList<View>) value : null;
    }

    private static Object readField(Object target, String name) throws Exception {
        Field field = target.getClass().getField(name);
        return field.get(target);
    }

    private static String stringField(Object target, String name) {
        try {
            Object value = readField(target, name);
            return value == null ? null : String.valueOf(value);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void refresh(final ViewGroup header, final ImageView button) {
        if (!REFRESH_RUNNING.compareAndSet(false, true)) {
            Log.i(TAG, "CLEAN15 refresh ignored; another refresh is active");
            return;
        }

        final RowInfo row = resolveRow(button.getTag());
        if (row == null) {
            REFRESH_RUNNING.set(false);
            Log.w(TAG, "CLEAN15 refresh clicked without a channel model");
            return;
        }

        final Context context = header.getContext().getApplicationContext();
        Log.i(TAG, "CLEAN15 refresh clicked channelId=" + row.channelId
                + " providerId=" + row.providerId
                + " title=" + row.title);

        if (row.providerId != null && row.providerId.startsWith(RED_LIGHT_PREFIX)) {
            refreshRedLight(context, button, row);
        } else {
            spinOnce(button);
            boolean dispatched = forceProjectivyRefresh();
            if (!dispatched) {
                REFRESH_RUNNING.set(false);
                return;
            }
            button.postDelayed(new Runnable() {
                @Override
                public void run() {
                    REFRESH_RUNNING.set(false);
                }
            }, 900L);
            Log.i(TAG, "CLEAN15 native channel refresh dispatched channelId="
                    + row.channelId);
        }
    }

    private static RowInfo resolveRow(Object tag) {
        try {
            Object model = readField(tag, "c");
            if (model == null) return null;

            RowInfo out = new RowInfo();
            out.title = stringField(tag, "b");

            Object channel = readField(model, "g");
            if (channel != null) {
                Object id = channel.getClass().getMethod("a").invoke(channel);
                if (id instanceof Number) out.channelId = ((Number) id).longValue();

                Object valuesObject = readField(channel, "a");
                if (valuesObject instanceof ContentValues) {
                    ContentValues values = (ContentValues) valuesObject;
                    out.providerId = values.getAsString("internal_provider_id");
                }
            } else {
                Object categoryId = readField(model, "j");
                if (categoryId instanceof Number) {
                    out.channelId = ((Number) categoryId).longValue() - 2000L;
                }
            }
            return out;
        } catch (Throwable error) {
            Log.e(TAG, "CLEAN15 unable to resolve channel identity", error);
            return null;
        }
    }

    private static void refreshRedLight(final Context context,
                                        final ImageView button,
                                        final RowInfo row) {
        button.setEnabled(false);
        final ObjectAnimator spinner = ObjectAnimator.ofFloat(
                button, "rotation", 0.0f, 360.0f);
        spinner.setDuration(700L);
        spinner.setInterpolator(new DecelerateInterpolator());
        spinner.setRepeatCount(ValueAnimator.INFINITE);
        spinner.start();

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean success = syncRedLightWithoutLaunchingKodi(context);
                final boolean completed = success;

                MAIN.post(new Runnable() {
                    @Override
                    public void run() {
                        spinner.cancel();
                        button.setRotation(0.0f);
                        button.setEnabled(true);
                        REFRESH_RUNNING.set(false);

                        if (completed) {
                            forceProjectivyRefresh();
                            Log.i(TAG, "CLEAN15 Red Light row refresh completed channelId="
                                    + row.channelId + " success=true");
                        } else {
                            Toast.makeText(context,
                                    "Kodi is not ready or the Red Light refresh failed. "
                                            + "Cached channel cards were kept.",
                                    Toast.LENGTH_LONG).show();
                            Log.i(TAG, "CLEAN15 Red Light row refresh completed channelId="
                                    + row.channelId + " success=false");
                        }
                    }
                });
            }
        }, "CLEAN15-RedLight-Refresh");
        worker.setDaemon(true);
        worker.start();

        Log.i(TAG, "CLEAN15 Red Light manual sync started channelId=" + row.channelId);
    }

    private static boolean syncRedLightWithoutLaunchingKodi(Context context) {
        AtomicBoolean providerRunning = null;
        try {
            Class<?> provider = Class.forName(
                    "com.spocky.projengmenu.embedded.RedLightDiscoveryProvider");

            Field runningField = provider.getDeclaredField("RUNNING");
            runningField.setAccessible(true);
            Object runningObject = runningField.get(null);
            if (!(runningObject instanceof AtomicBoolean)) return false;
            providerRunning = (AtomicBoolean) runningObject;

            if (!providerRunning.compareAndSet(false, true)) {
                Log.i(TAG, "CLEAN15 Red Light sync skipped; provider sync already active");
                return false;
            }

            Method ready = provider.getDeclaredMethod("isKodiNativeReady");
            ready.setAccessible(true);
            if (!Boolean.TRUE.equals(ready.invoke(null))) {
                Log.i(TAG, "CLEAN15 Kodi is not ready; refusing to launch Kodi");
                return false;
            }

            Method lease = provider.getDeclaredMethod(
                    "ensureKodiProviderLease", Context.class);
            lease.setAccessible(true);
            if (!Boolean.TRUE.equals(lease.invoke(null, context))) {
                Log.i(TAG, "CLEAN15 Kodi provider lease unavailable");
                return false;
            }

            Class<?> engine = Class.forName(
                    "com.spocky.projengmenu.embedded.RedLightDiscoveryEngine");
            Method sync = engine.getMethod("syncOnce", Context.class);
            boolean success = Boolean.TRUE.equals(sync.invoke(null, context));
            Log.i(TAG, "CLEAN15 Red Light discovery success=" + success);
            return success;
        } catch (Throwable error) {
            Log.e(TAG, "CLEAN15 Red Light manual sync failed", error);
            return false;
        } finally {
            if (providerRunning != null) providerRunning.set(false);
        }
    }

    private static boolean forceProjectivyRefresh() {
        try {
            Class<?> controller = Class.forName("w40");
            Method refresh = controller.getMethod("z", boolean.class);
            refresh.invoke(null, false);
            Log.i(TAG, "CLEAN15 forced Projectivy channel refresh dispatched");
            return true;
        } catch (Throwable error) {
            Log.e(TAG, "CLEAN15 forced Projectivy channel refresh failed", error);
            return false;
        }
    }

    private static void spinOnce(final ImageView button) {
        ObjectAnimator spin = ObjectAnimator.ofFloat(button, "rotation", 0.0f, 360.0f);
        spin.setDuration(650L);
        spin.setInterpolator(new DecelerateInterpolator());
        spin.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                button.setRotation(0.0f);
            }
        });
        spin.start();
    }

    private static final class RowInfo {
        long channelId = -1L;
        String providerId;
        String title;
    }
}
