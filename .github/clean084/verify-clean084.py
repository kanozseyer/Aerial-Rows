#!/usr/bin/env python3
from pathlib import Path
import os, runpy, shutil, sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean084.py UNSIGNED_APK')
apk = Path(sys.argv[1])
verify084 = Path('/tmp/verify-clean084')
verify08 = Path('/tmp/verify-clean08')

# Reuse every CLEAN 08 + frozen CLEAN 07L invariant. 08.4 intentionally
# supersedes only 08.3's final explicit MainActivity restart mechanism.
if verify08.is_symlink() or verify08.exists():
    if verify08.is_dir() and not verify08.is_symlink(): shutil.rmtree(verify08)
    else: verify08.unlink()
os.symlink(verify084, verify08, target_is_directory=True)
runpy.run_path('.github/clean08/verify-clean08.py', run_name='__main__')

engine = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
profile = Path('.github/clean05/ProfileRestoreProvider.java').read_text()

for token in [
    # 08.1 Watchlists retained
    'navigator.trakt_lists_personal',
    'navigator.trakt_watchlists',
    'redlight:trakt:watchlist:movies',
    'redlight:trakt:watchlist:shows',
    # final colour retained
    'appearancePatch.put("appearance_cards_bg_tint", -13027016)',
    'root.put("appearance_cards_bg_tint", -13027016)',
    # native appearance lifecycle retained
    'appearancePatchJson',
    'getDeclaredMethod("h"',
    'Class.forName("hj3")',
    'migrate.invoke(null, false)',
    'CLEAN083 native appearance applied cardsBg=',
    # true HOME refresh is the only 08.4 change
    'canonical_002828_applied_v5_home_intent_refresh',
    'android.content.Intent.ACTION_MAIN',
    'android.content.Intent.CATEGORY_HOME',
    'android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED',
    'android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP',
    'CLEAN084 Android HOME intent dispatched after native appearance apply',
    '}, 1500L);',
]:
    assert token in engine or token in profile, token

assert 'FLAG_ACTIVITY_CLEAR_TASK' not in engine
assert 'CLEAN083 Home restarted after native appearance apply' not in engine
assert 'android.content.SharedPreferences.Editor appearance = prefs.edit();' not in engine
assert 'root.remove("action_categories_list")' in profile
assert 'CLEAN07I resume-sync' not in engine
assert 'CLEAN07J live-adapter registered' not in engine

with zipfile.ZipFile(apk) as z:
    c4 = z.read('classes4.dex')
    for token in [
        b'navigator.trakt_lists_personal',
        b'navigator.trakt_watchlists',
        b'canonical_002828_applied_v5_home_intent_refresh',
        b'CLEAN083 native appearance applied cardsBg=',
        b'CLEAN084 Android HOME intent dispatched after native appearance apply',
        b'redlight:trakt:watchlist:movies',
        b'redlight:trakt:watchlist:shows',
    ]:
        assert token in c4, token
    assert b'CLEAN083 Home restarted after native appearance apply' not in c4
    assert b'CLEAN07I resume-sync' not in c4
    assert b'CLEAN07J live-adapter registered' not in c4

print('PASS: CLEAN 08.4 = CLEAN 08.3 native appearance import + Android HOME refresh only')
