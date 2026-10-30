package com.spocky.projengmenu.clean14;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

/**
 * CLEAN14 shared Projectivy HorizontalGridView carousel policy.
 *
 * The grid keeps one complete preceding card to the left of the focused card
 * in the middle of a row. Leanback's BOTH_EDGE alignment naturally clamps the
 * first and last items, so the row remains fixed while focus traverses the
 * remaining visible cards at the end.
 */
public final class CLEAN14CarouselBridge {
    private static final String TAG = "CLEAN14CarouselBridge";
    private static final int WINDOW_ALIGN_BOTH_EDGE = 3;
    private static final float FIXED_OFFSET_PERCENT = -1.0f;
    private static final float END_BOUNCE_DP = 2.0f;

    private static final Object LOCK = new Object();
    private static final WeakHashMap<ViewGroup, Watcher> WATCHERS =
            new WeakHashMap<>();

    private CLEAN14CarouselBridge() {
    }

    public static void attach(ViewGroup grid) {
        if (grid == null) return;

        Watcher watcher;
        synchronized (LOCK) {
            watcher = WATCHERS.get(grid);
            if (watcher == null) {
                watcher = new Watcher(grid);
                WATCHERS.put(grid, watcher);
                grid.addOnAttachStateChangeListener(watcher);
            }
        }

        if (grid.isAttachedToWindow()) {
            watcher.attachObservers();
        }
        watcher.applyAlignment();
        Log.i(TAG, "CLEAN14 shared carousel attached grid="
                + grid.getClass().getName());
    }

    private static final class Watcher implements
            View.OnAttachStateChangeListener,
            ViewTreeObserver.OnGlobalLayoutListener,
            ViewTreeObserver.OnGlobalFocusChangeListener,
            ViewTreeObserver.OnPreDrawListener {

        private final WeakReference<ViewGroup> gridRef;
        private ViewTreeObserver observer;
        private boolean preDrawWatching;
        private boolean pendingEndBounce;
        private boolean bouncing;

        Watcher(ViewGroup grid) {
            gridRef = new WeakReference<>(grid);
        }

        void attachObservers() {
            ViewGroup grid = gridRef.get();
            if (grid == null) return;

            ViewTreeObserver current = grid.getViewTreeObserver();
            if (current == null || !current.isAlive()) return;
            if (observer == current) {
                applyAlignment();
                return;
            }

            detachObservers();
            observer = current;
            observer.addOnGlobalLayoutListener(this);
            observer.addOnGlobalFocusChangeListener(this);
            applyAlignment();
        }

        void detachObservers() {
            ViewTreeObserver old = observer;
            if (old != null && old.isAlive()) {
                old.removeOnGlobalLayoutListener(this);
                old.removeOnGlobalFocusChangeListener(this);
                if (preDrawWatching) {
                    old.removeOnPreDrawListener(this);
                }
            }
            observer = null;
            preDrawWatching = false;
            pendingEndBounce = false;
        }

        @Override
        public void onViewAttachedToWindow(View view) {
            attachObservers();
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
            detachObservers();
        }

        @Override
        public void onGlobalLayout() {
            applyAlignment();
            runPendingEndBounceIfReady();
        }

        @Override
        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
            applyAlignment();

            ViewGroup grid = gridRef.get();
            if (grid == null || oldFocus == null || newFocus == null) return;

            int oldPosition = adapterPosition(grid, oldFocus);
            int newPosition = adapterPosition(grid, newFocus);
            int itemCount = adapterItemCount(grid);

            if (oldPosition >= 0 && newPosition > oldPosition
                    && itemCount > 0 && newPosition == itemCount - 1) {
                pendingEndBounce = true;
                startPreDrawWatch();
                runPendingEndBounceIfReady();
            } else if (newPosition >= 0
                    && (itemCount <= 0 || newPosition < itemCount - 1)) {
                pendingEndBounce = false;
                stopPreDrawWatch();
            }
        }

        @Override
        public boolean onPreDraw() {
            runPendingEndBounceIfReady();
            if (!pendingEndBounce) {
                stopPreDrawWatch();
            }
            return true;
        }

        void applyAlignment() {
            ViewGroup grid = gridRef.get();
            if (grid == null || !grid.isAttachedToWindow()) return;
            if (grid.getWidth() <= 0 || grid.getChildCount() <= 0) return;

            View firstCard = grid.getChildAt(0);
            if (firstCard == null || firstCard.getWidth() <= 0) return;

            int spacing = invokeInt(grid, "getHorizontalSpacing", 0);
            int itemOffset = invokeInt(grid, "getItemAlignmentOffset", 0);
            int paddingStart = grid.getPaddingStart();
            int maximumAnchor = grid.getWidth() - grid.getPaddingEnd();

            int targetOffset = paddingStart + firstCard.getWidth()
                    + spacing + itemOffset;
            if (maximumAnchor < paddingStart) maximumAnchor = paddingStart;
            if (targetOffset > maximumAnchor) targetOffset = maximumAnchor;
            if (targetOffset < paddingStart) targetOffset = paddingStart;

            boolean changed = false;
            changed |= setIntIfDifferent(grid, "getWindowAlignment",
                    "setWindowAlignment", WINDOW_ALIGN_BOTH_EDGE);
            changed |= setFloatIfDifferent(grid, "getWindowAlignmentOffsetPercent",
                    "setWindowAlignmentOffsetPercent", FIXED_OFFSET_PERCENT);
            changed |= setFloatIfDifferent(grid, "getItemAlignmentOffsetPercent",
                    "setItemAlignmentOffsetPercent", FIXED_OFFSET_PERCENT);
            changed |= setIntIfDifferent(grid, "getWindowAlignmentOffset",
                    "setWindowAlignmentOffset", targetOffset);

            if (changed) {
                grid.requestLayout();
                Log.i(TAG, "CLEAN14 alignment mode=3 offset=" + targetOffset
                        + " cardWidth=" + firstCard.getWidth()
                        + " spacing=" + spacing
                        + " paddingStart=" + paddingStart
                        + " itemOffset=" + itemOffset
                        + " viewport=" + grid.getWidth());
            }
        }

        private void runPendingEndBounceIfReady() {
            if (!pendingEndBounce || bouncing) return;

            ViewGroup grid = gridRef.get();
            if (grid == null || !grid.isAttachedToWindow()) return;

            boolean canScrollLeft = grid.canScrollHorizontally(-1);
            boolean canScrollRight = grid.canScrollHorizontally(1);

            if (!canScrollLeft && !canScrollRight) {
                // The row fits in the viewport; it has no scrollable end.
                pendingEndBounce = false;
                stopPreDrawWatch();
                return;
            }

            if (canScrollLeft && !canScrollRight) {
                pendingEndBounce = false;
                stopPreDrawWatch();
                startEndBounce(grid);
            }
        }

        private void startEndBounce(final ViewGroup grid) {
            if (bouncing) return;
            bouncing = true;

            final float originalTranslation = grid.getTranslationX();
            final float distance = END_BOUNCE_DP
                    * grid.getResources().getDisplayMetrics().density;

            Log.i(TAG, "CLEAN14 end bounce distancePx=" + distance);

            ViewPropertyAnimator first = grid.animate();
            first.translationX(originalTranslation - distance)
                    .setDuration(70L)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            ViewGroup current = gridRef.get();
                            if (current == null) {
                                bouncing = false;
                                return;
                            }

                            current.animate()
                                    .translationX(originalTranslation)
                                    .setDuration(135L)
                                    .setInterpolator(new OvershootInterpolator(1.05f))
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            bouncing = false;
                                        }
                                    })
                                    .start();
                        }
                    })
                    .start();
        }

        private void startPreDrawWatch() {
            if (preDrawWatching) return;
            ViewTreeObserver current = observer;
            if (current == null || !current.isAlive()) return;
            current.addOnPreDrawListener(this);
            preDrawWatching = true;
        }

        private void stopPreDrawWatch() {
            if (!preDrawWatching) return;
            ViewTreeObserver current = observer;
            if (current != null && current.isAlive()) {
                current.removeOnPreDrawListener(this);
            }
            preDrawWatching = false;
        }
    }

    private static int adapterPosition(ViewGroup grid, View focus) {
        try {
            Method findItem = grid.getClass().getMethod(
                    "findContainingItemView", View.class);
            Object item = findItem.invoke(grid, focus);
            if (!(item instanceof View)) return -1;

            Method getPosition = grid.getClass().getMethod(
                    "getChildAdapterPosition", View.class);
            Object value = getPosition.invoke(grid, item);
            return value instanceof Number ? ((Number) value).intValue() : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static int adapterItemCount(ViewGroup grid) {
        try {
            Method getAdapter = grid.getClass().getMethod("getAdapter");
            Object adapter = getAdapter.invoke(grid);
            if (adapter == null) return -1;
            Method getCount = adapter.getClass().getMethod("getItemCount");
            Object value = getCount.invoke(adapter);
            return value instanceof Number ? ((Number) value).intValue() : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static int invokeInt(Object target, String methodName, int fallback) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value instanceof Number ? ((Number) value).intValue() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static float invokeFloat(Object target, String methodName, float fallback) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value instanceof Number ? ((Number) value).floatValue() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static boolean setIntIfDifferent(Object target, String getter,
                                             String setter, int wanted) {
        try {
            Method get = target.getClass().getMethod(getter);
            Object current = get.invoke(target);
            if (current instanceof Number
                    && ((Number) current).intValue() == wanted) return false;

            Method set = target.getClass().getMethod(setter, int.class);
            set.invoke(target, wanted);
            return true;
        } catch (Throwable error) {
            Log.w(TAG, "CLEAN14 unable to apply " + setter, error);
            return false;
        }
    }

    private static boolean setFloatIfDifferent(Object target, String getter,
                                               String setter, float wanted) {
        try {
            Method get = target.getClass().getMethod(getter);
            Object current = get.invoke(target);
            if (current instanceof Number
                    && Math.abs(((Number) current).floatValue() - wanted) < 0.001f) {
                return false;
            }

            Method set = target.getClass().getMethod(setter, float.class);
            set.invoke(target, wanted);
            return true;
        } catch (Throwable error) {
            Log.w(TAG, "CLEAN14 unable to apply " + setter, error);
            return false;
        }
    }
}
