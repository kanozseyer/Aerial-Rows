#!/usr/bin/env python3
from pathlib import Path

p = Path('.github/clean05/ProfileRestoreProvider.java')
text = p.read_text()

old_constants = '''    private static final String AERIAL_ID = "8c00feaf-c7be-43c2-80f9-d49853aaa99a";\n    private static final AtomicBoolean APPLYING = new AtomicBoolean(false);\n'''
new_constants = '''    private static final String AERIAL_ID = "8c00feaf-c7be-43c2-80f9-d49853aaa99a";\n    // CLEAN13: one-time native Projectivy clock-format migration.\n    private static final String CLEAN13_STATE = "clean13_date_clock_state";\n    private static final String CLEAN13_DONE = "date_next_to_clock_v1";\n    private static final String CLEAN13_CLOCK_L1 = "appearance_statusbar_clock_format_l1";\n    private static final AtomicBoolean APPLYING = new AtomicBoolean(false);\n'''
if text.count(old_constants) != 1:
    raise SystemExit(f'CLEAN13 constants anchor mismatch: {text.count(old_constants)}')
text = text.replace(old_constants, new_constants, 1)

old_done = '''        app = (Application) ac;\n        if (state(app).getBoolean(DONE, false)) {\n            Log.i(TAG, "CLEAN05 profile already applied");\n            return true;\n        }\n'''
new_done = '''        app = (Application) ac;\n        if (state(app).getBoolean(DONE, false)) {\n            // Existing installations already have the canonical profile, so apply\n            // CLEAN13 immediately before MainActivity reads the clock preference.\n            applyClean13DateClock(app);\n            Log.i(TAG, "CLEAN05 profile already applied");\n            return true;\n        }\n'''
if text.count(old_done) != 1:
    raise SystemExit(f'CLEAN13 existing-install anchor mismatch: {text.count(old_done)}')
text = text.replace(old_done, new_done, 1)

old_profile_commit = '''            e.putString("appearance_wallpaper_provider", AERIAL_ID);\n            e.putString("appearance_wallpaper_provider_packagename", app.getPackageName());\n            e.commit();\n\n            Method reset = dj3.getDeclaredMethod("m");\n'''
new_profile_commit = '''            e.putString("appearance_wallpaper_provider", AERIAL_ID);\n            e.putString("appearance_wallpaper_provider_packagename", app.getPackageName());\n            e.commit();\n\n            // Fresh installations receive CLEAN13 only after the bundled canonical\n            // profile has imported, otherwise that import would overwrite the clock.\n            applyClean13DateClock(app);\n\n            Method reset = dj3.getDeclaredMethod("m");\n'''
if text.count(old_profile_commit) != 1:
    raise SystemExit(f'CLEAN13 fresh-install anchor mismatch: {text.count(old_profile_commit)}')
text = text.replace(old_profile_commit, new_profile_commit, 1)

anchor = '''    private void unregister() {\n'''
if text.count(anchor) != 1:
    raise SystemExit(f'CLEAN13 helper anchor mismatch: {text.count(anchor)}')

helper = '''    private static void applyClean13DateClock(Context c) {\n        try {\n            SharedPreferences migration = c.getSharedPreferences(CLEAN13_STATE, Context.MODE_PRIVATE);\n            if (migration.getBoolean(CLEAN13_DONE, false)) {\n                Log.i(TAG, "CLEAN13 date clock already applied");\n                return;\n            }\n\n            SharedPreferences prefs = projectivyPrefs(c);\n            String before = prefs.getString(CLEAN13_CLOCK_L1, "");\n            if (before == null) before = "";\n            String trimmed = before.trim();\n            String after;\n\n            // Preserve Projectivy/user's existing 12h/24h time expression. Only append\n            // a date when Line 1 does not already contain a date pattern. Month is M\n            // (minutes are m), so this does not mistake an ordinary time for a date.\n            if (containsDatePattern(trimmed)) {\n                after = before;\n            } else {\n                String time = trimmed.isEmpty() ? "HH:mm" : before;\n                after = time + "   EEE d MMM";\n                if (!prefs.edit().putString(CLEAN13_CLOCK_L1, after).commit())\n                    throw new IllegalStateException("could not persist CLEAN13 clock format");\n            }\n\n            if (!migration.edit().putBoolean(CLEAN13_DONE, true).commit())\n                throw new IllegalStateException("could not persist CLEAN13 completion marker");\n\n            // Projectivy normally observes the same preference from its native Clock\n            // settings. Reset its cached settings singleton so the first Home render\n            // definitely sees the migrated value.\n            try {\n                Class<?> dj3 = Class.forName("dj3");\n                Method reset = dj3.getDeclaredMethod("m");\n                reset.setAccessible(true);\n                reset.invoke(null);\n            } catch (Throwable ignored) {}\n\n            Log.i(TAG, "CLEAN13 date clock APPLIED before=" + before + " after=" + after);\n        } catch (Throwable t) {\n            Log.e(TAG, "CLEAN13 date clock FAILED", t);\n        }\n    }\n\n    private static boolean containsDatePattern(String s) {\n        if (s == null || s.isEmpty()) return false;\n        // Projectivy 4.72 uses Java date/time format patterns for Clock Line 1/2.\n        // Date tokens are y (year), M/L (month), d/D (day), E (weekday).\n        boolean quoted = false;\n        for (int i = 0; i < s.length(); i++) {\n            char ch = s.charAt(i);\n            if (ch == '\\'') {\n                if (i + 1 < s.length() && s.charAt(i + 1) == '\\'') { i++; continue; }\n                quoted = !quoted;\n                continue;\n            }\n            if (!quoted && (ch == 'y' || ch == 'Y' || ch == 'M' || ch == 'L'\n                    || ch == 'd' || ch == 'D' || ch == 'E')) return true;\n        }\n        return false;\n    }\n\n'''
text = text.replace(anchor, helper + anchor, 1)
p.write_text(text)

check = p.read_text()
for token in [
    'CLEAN13 date clock APPLIED before=',
    'appearance_statusbar_clock_format_l1',
    'after = time + "   EEE d MMM"',
    'applyClean13DateClock(app);',
    'containsDatePattern(trimmed)',
    'CLEAN05 restore SUCCESS',
]:
    if token not in check:
        raise SystemExit('CLEAN13 source verification failed: ' + token)

print('PASS: CLEAN13 native Projectivy date-next-to-clock migration prepared')
