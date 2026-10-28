#!/usr/bin/env python3
from pathlib import Path
import os, runpy, shutil, sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean082.py UNSIGNED_APK')
apk = Path(sys.argv[1])
verify082 = Path('/tmp/verify-clean082')
verify8 = Path('/tmp/verify-clean08')

# Reuse every CLEAN 08 + frozen CLEAN 07L invariant. CLEAN 08.2 intentionally
# replaces CLEAN 08.1's exact marker/background value, so 08.1's verifier itself
# cannot be chained verbatim; its behavioral invariants are asserted below.
if verify8.is_symlink() or verify8.exists():
    if verify8.is_dir() and not verify8.is_symlink(): shutil.rmtree(verify8)
    else: verify8.unlink()
os.symlink(verify082, verify8, target_is_directory=True)
runpy.run_path('.github/clean08/verify-clean08.py', run_name='__main__')

engine = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
profile = Path('.github/clean05/ProfileRestoreProvider.java').read_text()

# CLEAN 08.1 Watchlist behavior must remain present, while only its card colour
# and one-shot completion marker are superseded by CLEAN 08.2.
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
assert 'canonical_002828_applied_v2_cardbg_watchlists' not in engine
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
    assert b'canonical_002828_applied_v2_cardbg_watchlists' not in c4
    assert b'CLEAN07I resume-sync' not in c4
    assert b'CLEAN07J live-adapter registered' not in c4

print('PASS: CLEAN 08.2 = CLEAN 08 + retained 08.1 Watchlists + exact final #393938 card background only')
