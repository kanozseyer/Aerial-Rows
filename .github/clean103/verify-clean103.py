#!/usr/bin/env python3
from pathlib import Path
import sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean103.py UNSIGNED_APK')
apk = Path(sys.argv[1])
base = Path('/tmp/Projectivy-CLEAN-09-1-Movie-Episode-Playback-Intent-Filter.apk')
verify = Path('/tmp/verify-clean103')
assert base.is_file() and apk.is_file()

with zipfile.ZipFile(base) as zb, zipfile.ZipFile(apk) as zn:
    bn = {i.filename for i in zb.infolist() if not i.filename.startswith('META-INF/')}
    nn = {i.filename for i in zn.infolist() if not i.filename.startswith('META-INF/')}
    assert bn == nn
    changed = [n for n in sorted(bn) if zb.read(n) != zn.read(n)]
    assert changed == ['classes4.dex'], changed
    assert zb.read('AndroidManifest.xml') == zn.read('AndroidManifest.xml')

engine = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
launch = Path('.github/clean09/RedLightLaunchActivity.java').read_text()
manifest = (verify / 'AndroidManifest.xml').read_text()

for token in [
    'type == 0 || type == 1 || type == 2',
    'source.getData()',
    'browseTarget.indexOf("#Intent;")',
    '"build_season_list".equals(mode)',
    'CLEAN10.3 browse clean uri=',
    'GUI.ActivateWindow',
    'parameters.put(target)',
    'parameters.put("return")',
    'CLEAN09 forwarding kind=',
]:
    assert token in engine + launch, token

# The proven CLEAN09.1 resolver remains unchanged in the manifest.
for token in [
    'com.spocky.projengmenu.embedded.RedLightLaunchActivity',
    'android.intent.action.VIEW',
    'android:scheme="plugin"',
    'android:host="plugin.video.redlight"',
]:
    assert token in manifest, token

# Key frozen invariants stay present.
for token in [
    'navigator.trakt_lists_personal',
    'navigator.trakt_watchlists',
    'redlight:trakt:watchlist:movies',
    'redlight:trakt:watchlist:shows',
    'canonical_002828_applied_v6_raw_alpha_process_reload',
    'appearance.putInt("appearance_cards_bg_tint", -13027016)',
    'CLEAN085 killing process after raw opaque appearance commit',
]:
    assert token in engine, token

with zipfile.ZipFile(apk) as z:
    c4 = z.read('classes4.dex')
    for token in [
        b'RedLightLaunchActivity', b'CLEAN10.3 browse clean uri=',
        b'GUI.ActivateWindow', b'build_season_list', b'CLEAN09 forwarding kind=',
        b'projectivy-clean07', b'redlight:trakt:watchlist:movies', b'redlight:trakt:watchlist:shows'
    ]:
        assert token in c4, token

print('PASS: CLEAN10.3 = frozen CLEAN09.1 + classes4-only sanitized TV browse URI delta')
