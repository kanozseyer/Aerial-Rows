#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean13_1/WeatherStatusBarBridge.java')
s = p.read_text()

# CLEAN13.1I starts from exact G. Keep G's proven Weather settings sibling,
# native Projectivy cloud icon/background, and G checked-row implementation untouched.
for token in [
    'CLEAN13.1G Weather parent uses native Projectivy cloud icon',
    'getDeclaredField("m4")',
    'nativeCloud, 16',
    'makeCheckedAction(fragment, ID_SHOW_PARENT',
    'CLEAN13.1B Weather sibling added to Status Bar',
]:
    assert token in s, token

# Replace B's hard-coded child-index ordering with a layout-direction-aware order.
# Projectivy can apply its own unified Status Bar pass after Weather render, so I also
# keep a tiny pre-draw watcher which restores the relative order only when necessary.
old = r'''    private static void enforceStatusOrder(ViewGroup parent, ViewGroup icons, View badge, View clock) {
        moveChild(parent, icons, 0);
        moveChild(parent, badge, Math.min(1, parent.getChildCount() - 1));
        moveChild(parent, clock, Math.min(2, parent.getChildCount() - 1));
        parent.requestLayout();
        parent.invalidate();
        Log.i(TAG, "CLEAN13.1B status order=settings/status icons > weather > date > clock");
    }
'''
new = r'''    private static void enforceStatusOrder(ViewGroup parent, ViewGroup icons, View badge, View clock) {
        if (parent == null || icons == null || badge == null || clock == null) return;
        if (icons.getParent() != parent || badge.getParent() != parent || clock.getParent() != parent) return;

        final boolean rtl = parent.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        if (rtl) {
            // LinearLayout lays child index 0 at the visual right in RTL. To obtain
            // visual left-to-right: status icons -> Weather -> date/clock, store the
            // three views in reverse index order.
            moveChild(parent, clock, 0);
            moveChild(parent, badge, Math.min(1, parent.getChildCount() - 1));
            moveChild(parent, icons, Math.min(2, parent.getChildCount() - 1));
        } else {
            moveChild(parent, icons, 0);
            moveChild(parent, badge, Math.min(1, parent.getChildCount() - 1));
            moveChild(parent, clock, Math.min(2, parent.getChildCount() - 1));
        }
        parent.requestLayout();
        parent.invalidate();
        logStatusGeometry(parent, icons, badge, clock, "enforce");
    }

    private static void logStatusGeometry(ViewGroup parent, View icons, View badge, View clock, String phase) {
        Log.i(TAG, "CLEAN13.1I status geometry phase=" + phase
                + " layoutDirection=" + (parent.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL ? "RTL" : "LTR")
                + " indices=" + parent.indexOfChild(icons) + "," + parent.indexOfChild(badge) + "," + parent.indexOfChild(clock)
                + " left=" + icons.getLeft() + "," + badge.getLeft() + "," + clock.getLeft()
                + " right=" + icons.getRight() + "," + badge.getRight() + "," + clock.getRight());
    }
'''
assert s.count(old) == 1, f'enforceStatusOrder count={s.count(old)}'
s = s.replace(old, new, 1)

# Add a dedicated order watcher map next to the existing adaptive-pill watcher map.
anchor = '    private static final Map<ViewGroup, AdaptivePillWatcher> PILL_WATCHERS = new WeakHashMap<>();\n'
assert s.count(anchor) == 1
s = s.replace(anchor, anchor + '    private static final Map<ViewGroup, StatusOrderWatcher> ORDER_WATCHERS = new WeakHashMap<>();\n', 1)

# Install the order watcher next to the already-proven idle-pill watcher.
needle = '''            enforceStatusOrder(parent, icons, badge, clock);\n            installAdaptivePillWatcher(parent, icons);\n'''
replacement = '''            enforceStatusOrder(parent, icons, badge, clock);\n            installStatusOrderWatcher(parent, icons, badge, clock);\n            installAdaptivePillWatcher(parent, icons);\n'''
assert s.count(needle) == 1, f'render watcher anchor count={s.count(needle)}'
s = s.replace(needle, replacement, 1)

# Insert watcher helper before adaptive pill helper.
anchor = '    private static void installAdaptivePillWatcher(ViewGroup parent, ViewGroup icons) {\n'
assert s.count(anchor) == 1
helper = r'''    private static void installStatusOrderWatcher(ViewGroup parent, ViewGroup icons, View badge, View clock) {
        synchronized (ORDER_WATCHERS) {
            if (ORDER_WATCHERS.containsKey(parent)) return;
            StatusOrderWatcher watcher = new StatusOrderWatcher(parent, icons, badge, clock);
            ORDER_WATCHERS.put(parent, watcher);
            parent.getViewTreeObserver().addOnPreDrawListener(watcher);
            Log.i(TAG, "CLEAN13.1I status order watcher attached");
        }
    }

'''
s = s.replace(anchor, helper + anchor, 1)

# Insert watcher class before the adaptive pill watcher class.
anchor = '    private static final class AdaptivePillWatcher implements ViewTreeObserver.OnPreDrawListener {\n'
assert s.count(anchor) == 1
watcher = r'''    private static final class StatusOrderWatcher implements ViewTreeObserver.OnPreDrawListener {
        private final WeakReference<ViewGroup> parentRef;
        private final WeakReference<ViewGroup> iconsRef;
        private final WeakReference<View> badgeRef;
        private final WeakReference<View> clockRef;
        private boolean loggedStable;

        StatusOrderWatcher(ViewGroup parent, ViewGroup icons, View badge, View clock) {
            parentRef = new WeakReference<>(parent);
            iconsRef = new WeakReference<>(icons);
            badgeRef = new WeakReference<>(badge);
            clockRef = new WeakReference<>(clock);
        }

        @Override public boolean onPreDraw() {
            ViewGroup parent = parentRef.get();
            ViewGroup icons = iconsRef.get();
            View badge = badgeRef.get();
            View clock = clockRef.get();
            if (parent == null || icons == null || badge == null || clock == null) return true;
            if (icons.getParent() != parent || badge.getParent() != parent || clock.getParent() != parent) return true;

            boolean rtl = parent.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            int ii = parent.indexOfChild(icons);
            int bi = parent.indexOfChild(badge);
            int ci = parent.indexOfChild(clock);
            boolean correct = rtl ? (ci < bi && bi < ii) : (ii < bi && bi < ci);
            if (!correct) {
                loggedStable = false;
                Log.i(TAG, "CLEAN13.1I correcting status order after native pass");
                enforceStatusOrder(parent, icons, badge, clock);
            } else if (!loggedStable) {
                logStatusGeometry(parent, icons, badge, clock, "stable");
                loggedStable = true;
            }
            return true;
        }
    }

'''
s = s.replace(anchor, watcher + anchor, 1)

# Hard guards: I must not contain H's known-bad hv1 checkbox experiment.
assert 'Class.forName("hv1")' not in s
assert 'CLEAN13.1H native checkbox Show weather=' not in s
assert 'CLEAN13.1H Weather ordered between status icons and date/clock' not in s

for token in [
    'CLEAN13.1I status order watcher attached',
    'CLEAN13.1I correcting status order after native pass',
    'CLEAN13.1I status geometry phase=',
    'CLEAN13.1G Weather parent uses native Projectivy cloud icon',
    'makeCheckedAction(fragment, ID_SHOW_PARENT',
]:
    assert token in s, token

p.write_text(s)
print('PASS: CLEAN13.1I exact-G Weather menu retained; order made layout-direction-aware')
