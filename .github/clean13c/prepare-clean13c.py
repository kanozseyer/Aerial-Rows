#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean07/RedLightDiscoveryProvider.java')
text = p.read_text()

old_constants = '''    private static final long REFRESH_INTERVAL_MS = 5L * 60L * 1000L;\n    private static volatile ContentProviderClient kodiProviderLease;\n'''
new_constants = '''    private static final long REFRESH_INTERVAL_MS = 5L * 60L * 1000L;\n    private static final String CLEAN13C_PROFILE_STATE = "clean05_profile_restore_state";\n    private static final String CLEAN13C_PROFILE_DONE = "canonical_profile_applied_cda71dba_v1";\n    private static final String CLEAN13C_STATE = "clean13c_date_clock_state";\n    private static final String CLEAN13C_DONE = "native_date_next_to_clock_v1";\n    private static final AtomicBoolean DATE_APPLYING = new AtomicBoolean(false);\n    private static volatile ContentProviderClient kodiProviderLease;\n'''
if text.count(old_constants) != 1:
    raise SystemExit(f'CLEAN13C constants anchor mismatch: {text.count(old_constants)}')
text = text.replace(old_constants, new_constants, 1)

old_resume = '''                if (a == null || !a.getClass().getName().equals("com.spocky.projengmenu.ui.home.MainActivity")) return;\n\n                long now = SystemClock.elapsedRealtime();\n'''
new_resume = '''                if (a == null || !a.getClass().getName().equals("com.spocky.projengmenu.ui.home.MainActivity")) return;\n\n                // CLEAN13C uses Projectivy's own native preference object only after Home\n                // has safely reached RESUMED. This avoids CLEAN13A's early-init crash and\n                // CLEAN13B's incorrect direct SharedPreferences write.\n                if (applyClean13CDateClock(a)) return;\n\n                long now = SystemClock.elapsedRealtime();\n'''
if text.count(old_resume) != 1:
    raise SystemExit(f'CLEAN13C resume anchor mismatch: {text.count(old_resume)}')
text = text.replace(old_resume, new_resume, 1)

anchor = '''    private static boolean isKodiNativeReady() {\n'''
if text.count(anchor) != 1:
    raise SystemExit(f'CLEAN13C helper anchor mismatch: {text.count(anchor)}')

helper = r'''    private static boolean applyClean13CDateClock(Activity activity) {
        try {
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) return false;

            android.content.SharedPreferences profile = activity.getSharedPreferences(
                    CLEAN13C_PROFILE_STATE, Context.MODE_PRIVATE);
            if (!profile.getBoolean(CLEAN13C_PROFILE_DONE, false)) {
                Log.i(TAG, "CLEAN13C native clock WAIT profile restore incomplete");
                return false;
            }

            android.content.SharedPreferences state = activity.getSharedPreferences(
                    CLEAN13C_STATE, Context.MODE_PRIVATE);
            if (state.getBoolean(CLEAN13C_DONE, false)) return false;
            if (!DATE_APPLYING.compareAndSet(false, true)) return true;

            final WeakReference<Activity> ref = new WeakReference<>(activity);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    Activity a = ref.get();
                    if (a == null || a.isFinishing() || a.isDestroyed()) return;

                    Class<?> hj3 = Class.forName("hj3");
                    java.lang.reflect.Field w1Field = hj3.getDeclaredField("W1");
                    w1Field.setAccessible(true);
                    Object w1 = w1Field.get(null);
                    if (w1 == null) throw new IllegalStateException("hj3.W1 unavailable");

                    java.lang.reflect.Method read = w1.getClass().getDeclaredMethod("a");
                    read.setAccessible(true);
                    java.lang.reflect.Method write = w1.getClass().getDeclaredMethod("h", Object.class);
                    write.setAccessible(true);

                    Object beforeObj = read.invoke(w1);
                    String before = beforeObj == null ? "" : String.valueOf(beforeObj);
                    String after = before;

                    if (!containsDatePattern(before)) {
                        String time = before.trim().isEmpty()
                                ? (android.text.format.DateFormat.is24HourFormat(a) ? "HH:mm" : "h:mm a")
                                : before;
                        after = time + "   EEE d MMM";
                        write.invoke(w1, after);
                    }

                    Object verifiedObj = read.invoke(w1);
                    String verified = verifiedObj == null ? "" : String.valueOf(verifiedObj);
                    if (!after.equals(verified)) {
                        throw new IllegalStateException("native clock readback mismatch: " + verified);
                    }

                    if (!a.getSharedPreferences(CLEAN13C_STATE, Context.MODE_PRIVATE)
                            .edit().putBoolean(CLEAN13C_DONE, true).commit()) {
                        throw new IllegalStateException("could not persist CLEAN13C completion marker");
                    }

                    Log.i(TAG, "CLEAN13C native clock APPLIED before=" + before
                            + " after=" + after + " verified=" + verified);

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Activity home = ref.get();
                        if (home == null || home.isFinishing() || home.isDestroyed()) return;
                        try {
                            Log.i(TAG, "CLEAN13C requesting safe Home recreate");
                            home.recreate();
                        } catch (Throwable t) {
                            Log.e(TAG, "CLEAN13C Home recreate failed", t);
                        }
                    }, 250L);
                } catch (Throwable t) {
                    Log.e(TAG, "CLEAN13C native clock FAILED", t);
                } finally {
                    DATE_APPLYING.set(false);
                }
            }, 450L);
            return true;
        } catch (Throwable t) {
            DATE_APPLYING.set(false);
            Log.e(TAG, "CLEAN13C native clock FAILED", t);
            return false;
        }
    }

    private static boolean containsDatePattern(String s) {
        if (s == null || s.isEmpty()) return false;
        boolean quoted = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\'') {
                if (i + 1 < s.length() && s.charAt(i + 1) == '\'') { i++; continue; }
                quoted = !quoted;
                continue;
            }
            if (!quoted && (ch == 'y' || ch == 'Y' || ch == 'M' || ch == 'L'
                    || ch == 'd' || ch == 'D' || ch == 'E')) return true;
        }
        return false;
    }

'''
text = text.replace(anchor, helper + anchor, 1)
p.write_text(text)

check = p.read_text()
for token in [
    'CLEAN13C native clock APPLIED before=',
    'CLEAN13C requesting safe Home recreate',
    'getDeclaredField("W1")',
    'getDeclaredMethod("h", Object.class)',
    'EEE d MMM',
    'CLEAN12 refresh throttled ageMs=',
    'CLEAN12 Kodi native not ready - cached rows retained; no provider lease',
]:
    if token not in check:
        raise SystemExit('CLEAN13C source verification failed: ' + token)
for forbidden in [
    'activity.getPackageName() + "_preferences"',
    'CLEAN13B date clock APPLIED',
    'Class.forName("dj3")',
    'getDeclaredMethod("m")',
]:
    if forbidden in check:
        raise SystemExit('CLEAN13C forbidden path present: ' + forbidden)

print('PASS: CLEAN13C uses native hj3.W1 preference setter after MainActivity resume')
