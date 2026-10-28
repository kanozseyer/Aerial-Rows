#!/usr/bin/env python3
from pathlib import Path

# CLEAN 08.3 keeps CLEAN 08.2 behavior and changes only how appearance colours
# are applied during a CLEAN08 reconciliation/update. Instead of raw
# SharedPreferences writes, replay the two colour values through Projectivy's
# own typed importer + migration/reset lifecycle, then reload Home once.

engine = Path('.github/clean07/RedLightDiscoveryEngine.java')
text = engine.read_text()

old_marker = 'private static final String CLEAN08_APPLIED = "canonical_002828_applied_v3_final_cardbg";'
new_marker = 'private static final String CLEAN08_APPLIED = "canonical_002828_applied_v4_native_appearance";'
if text.count(old_marker) != 1:
    raise SystemExit(f'CLEAN083 marker anchor mismatch: {text.count(old_marker)}')
text = text.replace(old_marker, new_marker, 1)

old_block = '''            // Explicitly carry the two requested appearance colours from the exact\n            // canonical backup before rebuilding the category model. CLEAN 05 already\n            // imports all non-layout settings; these writes make the dependency\n            // intentional and verifiable in CLEAN 08 as well.\n            android.content.SharedPreferences.Editor appearance = prefs.edit();\n            // CLEAN 08.1: exact card background from projectivy-settings-20261028_214702.plbackup.\n            appearance.putInt("appearance_cards_bg_tint", -13027016);\n            if (canonical.has("appearance_statusbar_maincontainer_icon_background_tint"))\n                appearance.putInt("appearance_statusbar_maincontainer_icon_background_tint",\n                        canonical.getInt("appearance_statusbar_maincontainer_icon_background_tint"));\n            appearance.commit();\n            Log.i(CLEAN08_TAG, "CLEAN08 appearance cardsBg="\n                    + prefs.getInt("appearance_cards_bg_tint", 0)\n                    + " iconBg=" + prefs.getInt("appearance_statusbar_maincontainer_icon_background_tint", 0));\n\n'''
new_block = '''            // CLEAN 08.3: do NOT raw-write live appearance prefs here. Build a tiny\n            // typed Projectivy import payload and apply it on the main thread together\n            // with the layout transaction below. This mirrors the native manual restore\n            // lifecycle that is proven to refresh Projectivy's live appearance objects.\n            JSONObject appearancePatch = new JSONObject();\n            appearancePatch.put("appearance_cards_bg_tint", -13027016);\n            if (canonical.has("appearance_statusbar_maincontainer_icon_background_tint"))\n                appearancePatch.put("appearance_statusbar_maincontainer_icon_background_tint",\n                        canonical.getInt("appearance_statusbar_maincontainer_icon_background_tint"));\n            final String appearancePatchJson = appearancePatch.toString();\n\n'''
if text.count(old_block) != 1:
    raise SystemExit(f'CLEAN083 raw appearance block mismatch: {text.count(old_block)}')
text = text.replace(old_block, new_block, 1)

old_main = '''                try {\n                    if (!prefs.edit().putString("action_categories_list", layout).commit())\n                        throw new IllegalStateException("could not persist action_categories_list");\n\n                    Class<?> settings = Class.forName("dj3");\n                    java.lang.reflect.Method reset = settings.getDeclaredMethod("m");\n                    reset.setAccessible(true);\n                    reset.invoke(null);\n\n                    Class<?> categories = Class.forName("w40");\n'''
new_main = '''                try {\n                    // Projectivy native typed preference import: same core path used by\n                    // the working manual .plbackup restore, but only for the two colours.\n                    Class<?> settings = Class.forName("dj3");\n                    java.lang.reflect.Method merge = settings.getDeclaredMethod(\n                            "h", android.content.SharedPreferences.class, String.class);\n                    merge.setAccessible(true);\n                    merge.invoke(null, prefs, appearancePatchJson);\n\n                    Class<?> migrations = Class.forName("hj3");\n                    java.lang.reflect.Method migrate = migrations.getDeclaredMethod("a", boolean.class);\n                    migrate.setAccessible(true);\n                    migrate.invoke(null, false);\n\n                    if (!prefs.edit().putString("action_categories_list", layout).commit())\n                        throw new IllegalStateException("could not persist action_categories_list");\n\n                    java.lang.reflect.Method reset = settings.getDeclaredMethod("m");\n                    reset.setAccessible(true);\n                    reset.invoke(null);\n\n                    Log.i(CLEAN08_TAG, "CLEAN083 native appearance applied cardsBg="\n                            + prefs.getInt("appearance_cards_bg_tint", 0)\n                            + " iconBg=" + prefs.getInt(\n                                    "appearance_statusbar_maincontainer_icon_background_tint", 0));\n\n                    Class<?> categories = Class.forName("w40");\n'''
if text.count(old_main) != 1:
    raise SystemExit(f'CLEAN083 main apply anchor mismatch: {text.count(old_main)}')
text = text.replace(old_main, new_main, 1)

old_success = '''                    Log.i(CLEAN08_TAG, "CLEAN08 APPLIED categories=" + out.length()\n                            + " liveRed=" + redByKey.size()\n                            + " matched=" + matchedFinal\n                            + " removed=" + removedFinal\n                            + " addedHidden=" + addedFinal\n                            + " mode=" + (applied ? "reconcile" : "canonical"));\n                    ok.set(true);\n'''
new_success = '''                    Log.i(CLEAN08_TAG, "CLEAN08 APPLIED categories=" + out.length()\n                            + " liveRed=" + redByKey.size()\n                            + " matched=" + matchedFinal\n                            + " removed=" + removedFinal\n                            + " addedHidden=" + addedFinal\n                            + " mode=" + (applied ? "reconcile" : "canonical"));\n                    ok.set(true);\n\n                    // A real Activity reload is required for card renderers that already\n                    // captured the previous live appearance object. Run once because the\n                    // v4 marker/signature is persisted above before this restart occurs.\n                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {\n                        try {\n                            android.content.Intent restart = homeIntent(c);\n                            restart.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK\n                                    | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);\n                            c.startActivity(restart);\n                            Log.i(CLEAN08_TAG, "CLEAN083 Home restarted after native appearance apply");\n                        } catch (Throwable restartError) {\n                            Log.i(CLEAN08_TAG, "CLEAN083 Home restart unavailable");\n                        }\n                    }, 350L);\n'''
if text.count(old_success) != 1:
    raise SystemExit(f'CLEAN083 success anchor mismatch: {text.count(old_success)}')
text = text.replace(old_success, new_success, 1)

engine.write_text(text)

check = engine.read_text()
required = [
    'canonical_002828_applied_v4_native_appearance',
    'appearancePatchJson',
    'getDeclaredMethod("h", android.content.SharedPreferences.class, String.class)',
    'Class.forName("hj3")',
    'migrate.invoke(null, false)',
    'CLEAN083 native appearance applied cardsBg=',
    'FLAG_ACTIVITY_CLEAR_TASK',
    'CLEAN083 Home restarted after native appearance apply',
    'navigator.trakt_lists_personal',
    'redlight:trakt:watchlist:movies',
    'redlight:trakt:watchlist:shows',
]
for token in required:
    if token not in check:
        raise SystemExit('CLEAN083 verification failed: ' + token)

# The broken update path must be gone from CLEAN08 runtime.
if 'android.content.SharedPreferences.Editor appearance = prefs.edit();' in check:
    raise SystemExit('CLEAN083 still contains raw CLEAN08 appearance editor')

print('PASS: CLEAN 08.3 prepared — native typed appearance import + migration + one Home reload')
