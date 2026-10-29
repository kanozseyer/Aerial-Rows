#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean07/RedLightDiscoveryProvider.java')
text = p.read_text()

old_constants = '''    private static final long REFRESH_INTERVAL_MS = 5L * 60L * 1000L;\n    private static volatile ContentProviderClient kodiProviderLease;\n'''
new_constants = '''    private static final long REFRESH_INTERVAL_MS = 5L * 60L * 1000L;\n    private static final String CLEAN13B_PROFILE_STATE = "clean05_profile_restore_state";\n    private static final String CLEAN13B_PROFILE_DONE = "canonical_profile_applied_cda71dba_v1";\n    private static final String CLEAN13B_STATE = "clean13b_date_clock_state";\n    private static final String CLEAN13B_DONE = "date_next_to_clock_v1";\n    private static final String CLEAN13B_CLOCK_L1 = "appearance_statusbar_clock_format_l1";\n    private static volatile ContentProviderClient kodiProviderLease;\n'''
if text.count(old_constants) != 1:
    raise SystemExit(f'CLEAN13B constants anchor mismatch: {text.count(old_constants)}')
text = text.replace(old_constants, new_constants, 1)

old_resume = '''                if (a == null || !a.getClass().getName().equals("com.spocky.projengmenu.ui.home.MainActivity")) return;\n\n                long now = SystemClock.elapsedRealtime();\n'''
new_resume = '''                if (a == null || !a.getClass().getName().equals("com.spocky.projengmenu.ui.home.MainActivity")) return;\n\n                // CLEAN13B deliberately runs only after MainActivity has safely reached RESUMED.\n                // CLEAN13A called Projectivy internals from ContentProvider.onCreate(), before\n                // q32/w40 initialization, which caused an ExceptionInInitializerError.\n                if (applyClean13BDateClock(a)) return;\n\n                long now = SystemClock.elapsedRealtime();\n'''
if text.count(old_resume) != 1:
    raise SystemExit(f'CLEAN13B resume anchor mismatch: {text.count(old_resume)}')
text = text.replace(old_resume, new_resume, 1)

anchor = '''    private static boolean isKodiNativeReady() {\n'''
if text.count(anchor) != 1:
    raise SystemExit(f'CLEAN13B helper anchor mismatch: {text.count(anchor)}')

helper = r'''    private static boolean applyClean13BDateClock(Activity activity) {
        try {
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) return false;

            // Fresh installs: wait until CLEAN05 has finished importing the canonical profile,
            // otherwise that import could overwrite the clock format we are about to set.
            android.content.SharedPreferences profile = activity.getSharedPreferences(
                    CLEAN13B_PROFILE_STATE, Context.MODE_PRIVATE);
            if (!profile.getBoolean(CLEAN13B_PROFILE_DONE, false)) {
                Log.i(TAG, "CLEAN13B date clock WAIT profile restore incomplete");
                return false;
            }

            android.content.SharedPreferences state = activity.getSharedPreferences(
                    CLEAN13B_STATE, Context.MODE_PRIVATE);
            if (state.getBoolean(CLEAN13B_DONE, false)) return false;

            android.content.SharedPreferences prefs = activity.getSharedPreferences(
                    activity.getPackageName() + "_preferences", Context.MODE_PRIVATE);
            String before = prefs.getString(CLEAN13B_CLOCK_L1, "");
            if (before == null) before = "";
            String trimmed = before.trim();

            // Preserve an existing user clock expression. If the preference is absent,
            // derive the time expression from Android's current 12/24-hour setting.
            String after = before;
            if (!containsDatePattern(trimmed)) {
                String time;
                if (trimmed.isEmpty()) {
                    time = android.text.format.DateFormat.is24HourFormat(activity)
                            ? "HH:mm" : "h:mm a";
                } else {
                    time = before;
                }
                after = time + "   EEE d MMM";
                if (!prefs.edit().putString(CLEAN13B_CLOCK_L1, after).commit())
                    throw new IllegalStateException("could not persist CLEAN13B clock format");
            }

            if (!state.edit().putBoolean(CLEAN13B_DONE, true).commit())
                throw new IllegalStateException("could not persist CLEAN13B completion marker");

            Log.i(TAG, "CLEAN13B date clock APPLIED before=" + before + " after=" + after);

            // No reflection, dj3.m(), w40, or other Projectivy singleton touch here.
            // Recreate Home from the already-safe resumed lifecycle so Projectivy naturally
            // rereads the preference through its normal initialization path.
            final WeakReference<Activity> ref = new WeakReference<>(activity);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Activity a = ref.get();
                if (a == null || a.isFinishing() || a.isDestroyed()) return;
                try {
                    Log.i(TAG, "CLEAN13B requesting safe Home recreate");
                    a.recreate();
                } catch (Throwable t) {
                    Log.e(TAG, "CLEAN13B Home recreate failed", t);
                }
            }, 250L);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "CLEAN13B date clock FAILED", t);
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
    'CLEAN13B date clock APPLIED before=',
    'CLEAN13B requesting safe Home recreate',
    'appearance_statusbar_clock_format_l1',
    'EEE d MMM',
    'DateFormat.is24HourFormat(activity)',
    'CLEAN12 refresh throttled ageMs=',
    'CLEAN12 Kodi native not ready - cached rows retained; no provider lease',
]:
    if token not in check:
        raise SystemExit('CLEAN13B source verification failed: ' + token)
for forbidden in ['Class.forName("dj3")', 'getDeclaredMethod("m")']:
    if forbidden in check:
        raise SystemExit('CLEAN13B forbidden startup-reset code present: ' + forbidden)

print('PASS: CLEAN13B date migration moved to safe MainActivity resume lifecycle')
