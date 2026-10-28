#!/usr/bin/env python3
from pathlib import Path
import os, runpy, shutil, sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean08.py UNSIGNED_APK')
apk = Path(sys.argv[1])
verify8 = Path('/tmp/verify-clean08')
verify7l = Path('/tmp/verify-clean07l')

# Reuse every frozen CLEAN 07L invariant against the CLEAN 08 decoded tree.
if verify7l.is_symlink() or verify7l.exists():
    if verify7l.is_dir() and not verify7l.is_symlink(): shutil.rmtree(verify7l)
    else: verify7l.unlink()
os.symlink(verify8, verify7l, target_is_directory=True)
runpy.run_path('.github/clean07l/verify-clean07l.py', run_name='__main__')

# CLEAN 05 must still defer the Home category list. CLEAN 08 is the only stage
# allowed to materialize action_categories_list after live channels exist.
profile_src = Path('.github/clean05/ProfileRestoreProvider.java').read_text()
assert 'root.remove("action_categories_list")' in profile_src

# The prepared discovery source must contain dynamic identity reconciliation,
# requested appearance carry-over, catalog signature tracking and native reload.
src = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
for token in [
    'CLEAN08 APPLIED categories=',
    'CLEAN08 catalog unchanged',
    'redlight_catalog_signature_v1',
    'redlight_identity_map_v1',
    'appearance_cards_bg_tint',
    'appearance_statusbar_maincontainer_icon_background_tint',
    'Class.forName("dj3")',
    'Class.forName("w40")',
    'redlight:shelf:trending:movies',
    'redlight:shelf:trending:shows',
    'redlight:in_progress_episodes',
    'redlight:continue_watching',
    'redlight:trakt:watchlist:movies',
    'redlight:trakt:watchlist:shows',
    'return restoreCanonicalHomeLayout(c);',
]:
    assert token in src, token

# Final APK must actually contain the CLEAN 08 runtime, not merely source files.
with zipfile.ZipFile(apk) as z:
    c4 = z.read('classes4.dex')
    for token in [
        b'ProjectivyHomeRestore',
        b'CLEAN08 APPLIED categories=',
        b'redlight_catalog_signature_v1',
        b'redlight_identity_map_v1',
        b'appearance_cards_bg_tint',
        b'appearance_statusbar_maincontainer_icon_background_tint',
        b'redlight:shelf:trending:movies',
        b'redlight:trakt:watchlist:shows',
    ]:
        assert token in c4, token
    assert b'CLEAN07I resume-sync' not in c4
    assert b'CLEAN07J live-adapter registered' not in c4

print('PASS: CLEAN 08 = frozen 07L + canonical 002828 appearance/layout + live redlight: semantic reconciliation')
