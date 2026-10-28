#!/usr/bin/env python3
from pathlib import Path
import os, runpy, shutil, sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean081.py UNSIGNED_APK')
apk = Path(sys.argv[1])
verify081 = Path('/tmp/verify-clean081')
verify8 = Path('/tmp/verify-clean08')

# Reuse every CLEAN 08 + frozen CLEAN 07L invariant against the 08.1 decoded tree.
if verify8.is_symlink() or verify8.exists():
    if verify8.is_dir() and not verify8.is_symlink(): shutil.rmtree(verify8)
    else: verify8.unlink()
os.symlink(verify081, verify8, target_is_directory=True)
runpy.run_path('.github/clean08/verify-clean08.py', run_name='__main__')

engine = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
profile = Path('.github/clean05/ProfileRestoreProvider.java').read_text()

for token in [
    'navigator.trakt_lists_personal',
    'navigator.trakt_watchlists',
    'canonical_002828_applied_v2_cardbg_watchlists',
    'appearance.putInt("appearance_cards_bg_tint", -12566464)',
    'redlight:trakt:watchlist:movies',
    'redlight:trakt:watchlist:shows',
]:
    assert token in engine, token
assert 'root.put("appearance_cards_bg_tint", -12566464)' in profile
assert 'root.remove("action_categories_list")' in profile

with zipfile.ZipFile(apk) as z:
    c4 = z.read('classes4.dex')
    for token in [
        b'navigator.trakt_lists_personal',
        b'navigator.trakt_watchlists',
        b'canonical_002828_applied_v2_cardbg_watchlists',
        b'redlight:trakt:watchlist:movies',
        b'redlight:trakt:watchlist:shows',
    ]:
        assert token in c4, token
    assert b'CLEAN07I resume-sync' not in c4
    assert b'CLEAN07J live-adapter registered' not in c4

print('PASS: CLEAN 08.1 = CLEAN 08 + exact #404040 card background + restored Trakt Watchlist traversal')
