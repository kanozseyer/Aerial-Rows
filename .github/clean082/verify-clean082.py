#!/usr/bin/env python3
from pathlib import Path
import os, runpy, shutil, sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean082.py UNSIGNED_APK')
apk = Path(sys.argv[1])
verify082 = Path('/tmp/verify-clean082')
verify081 = Path('/tmp/verify-clean081')

# Reuse every CLEAN 08.1 + CLEAN 08 + frozen 07L invariant.
if verify081.is_symlink() or verify081.exists():
    if verify081.is_dir() and not verify081.is_symlink(): shutil.rmtree(verify081)
    else: verify081.unlink()
os.symlink(verify082, verify081, target_is_directory=True)
runpy.run_path('.github/clean081/verify-clean081.py', run_name='__main__')

engine = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
profile = Path('.github/clean05/ProfileRestoreProvider.java').read_text()

for token in [
    'navigator.trakt_lists_personal',
    'navigator.trakt_watchlists',
    'canonical_002828_applied_v3_final_cardbg',
    'appearance.putInt("appearance_cards_bg_tint", -13027016)',
    'redlight:trakt:watchlist:movies',
    'redlight:trakt:watchlist:shows',
]:
    assert token in engine, token
assert 'appearance.putInt("appearance_cards_bg_tint", -12566464)' not in engine
assert 'root.put("appearance_cards_bg_tint", -13027016)' in profile
assert 'root.put("appearance_cards_bg_tint", -12566464)' not in profile
assert 'root.remove("action_categories_list")' in profile

with zipfile.ZipFile(apk) as z:
    c4 = z.read('classes4.dex')
    for token in [
        b'navigator.trakt_lists_personal',
        b'navigator.trakt_watchlists',
        b'canonical_002828_applied_v3_final_cardbg',
        b'redlight:trakt:watchlist:movies',
        b'redlight:trakt:watchlist:shows',
    ]:
        assert token in c4, token
    assert b'CLEAN07I resume-sync' not in c4
    assert b'CLEAN07J live-adapter registered' not in c4

print('PASS: CLEAN 08.2 = CLEAN 08.1 + exact final #393938 card background only')
