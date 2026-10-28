#!/usr/bin/env python3
from pathlib import Path
import os, runpy, shutil, sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean085.py UNSIGNED_APK')
apk = Path(sys.argv[1])
verify085 = Path('/tmp/verify-clean085')
verify08 = Path('/tmp/verify-clean08')

# Reuse every CLEAN 08 + frozen CLEAN 07L invariant against the 08.5 tree.
if verify08.is_symlink() or verify08.exists():
    if verify08.is_dir() and not verify08.is_symlink(): shutil.rmtree(verify08)
    else: verify08.unlink()
os.symlink(verify085, verify08, target_is_directory=True)
runpy.run_path('.github/clean08/verify-clean08.py', run_name='__main__')

engine = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
profile = Path('.github/clean05/ProfileRestoreProvider.java').read_text()

for token in [
    'navigator.trakt_lists_personal',
    'navigator.trakt_watchlists',
    'redlight:trakt:watchlist:movies',
    'redlight:trakt:watchlist:shows',
    'canonical_002828_applied_v6_raw_alpha_process_reload',
    'appearance.putInt("appearance_cards_bg_tint", -13027016)',
    'CLEAN085 killing process after raw opaque appearance commit',
    'android.os.Process.killProcess(android.os.Process.myPid())',
]:
    assert token in engine, token

assert 'root.put("appearance_cards_bg_tint", -13027016)' in profile
assert 'root.remove("action_categories_list")' in profile
assert 'appearancePatchJson' not in engine
assert 'CLEAN083 native appearance applied cardsBg=' not in engine
assert 'CLEAN084 Android HOME intent dispatched after native appearance apply' not in engine
assert 'CLEAN07I resume-sync' not in engine
assert 'CLEAN07J live-adapter registered' not in engine

with zipfile.ZipFile(apk) as z:
    c4 = z.read('classes4.dex')
    for token in [
        b'navigator.trakt_lists_personal',
        b'navigator.trakt_watchlists',
        b'canonical_002828_applied_v6_raw_alpha_process_reload',
        b'CLEAN085 killing process after raw opaque appearance commit',
        b'redlight:trakt:watchlist:movies',
        b'redlight:trakt:watchlist:shows',
    ]:
        assert token in c4, token
    assert b'CLEAN083 native appearance applied cardsBg=' not in c4
    assert b'CLEAN084 Android HOME intent dispatched after native appearance apply' not in c4
    assert b'CLEAN07I resume-sync' not in c4
    assert b'CLEAN07J live-adapter registered' not in c4

print('PASS: CLEAN 08.5 = CLEAN 08.2 + raw opaque ARGB + one full process reload only')
