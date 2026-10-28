#!/usr/bin/env python3
from pathlib import Path
import sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean09.py UNSIGNED_APK')
apk = Path(sys.argv[1])
base = Path('/tmp/Projectivy-CLEAN-08-5-Raw-Alpha-Process-Reload.apk')
verify09 = Path('/tmp/verify-clean09')
assert base.is_file(), base

# The frozen CLEAN08.5 build is executed immediately before CLEAN09 and runs its
# complete historical verifier chain itself. Prove the CLEAN09 payload then
# changes exactly two files on top of that verified artifact.
allowed = {'AndroidManifest.xml', 'classes4.dex'}
with zipfile.ZipFile(base) as zb, zipfile.ZipFile(apk) as zn:
    bn = {i.filename for i in zb.infolist() if not i.filename.startswith('META-INF/')}
    nn = {i.filename for i in zn.infolist() if not i.filename.startswith('META-INF/')}
    assert bn == nn, (sorted(bn-nn), sorted(nn-bn))
    changed = []
    for name in sorted(bn):
        if zb.read(name) != zn.read(name):
            changed.append(name)
    assert set(changed) == allowed, changed
    base_c4 = zb.read('classes4.dex')
    c4 = zn.read('classes4.dex')
    assert b'RedLightLaunchActivity' not in base_c4
    assert b'RedLightLaunchActivity' in c4

engine = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
profile = Path('.github/clean05/ProfileRestoreProvider.java').read_text()
launch = Path('.github/clean09/RedLightLaunchActivity.java').read_text()
manifest = (verify09 / 'AndroidManifest.xml').read_text()

# CLEAN09 source scope: movie + episode only. TV-show/folder navigation remains
# on CLEAN07's inert identity route until CLEAN10.
for token in [
    'programIntent(c, e)',
    'type == 0 || type == 2',
    'RedLightLaunchActivity.class.getName()',
    'RedLightLaunchActivity.EXTRA_TARGET',
    'RedLightLaunchActivity.EXTRA_KIND',
    'type == 0 ? "movie" : "episode"',
    'return programIdentityIntent(c, pluginUri)',
    'projectivy-clean07',
]:
    assert token in engine, token
block = engine.split('private static Intent programIntent', 1)[1].split('private static Intent programIdentityIntent', 1)[0]
assert 'type == 1' not in block

# Key frozen CLEAN08.5 invariants must remain in the rebuilt embedded runtime.
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

for token in [
    'ProjectivyRedLightLaunch',
    'CLEAN09 forwarding kind=',
    'plugin://plugin.video.redlight/',
    'org.xbmc.kodi.Splash',
    'Intent.ACTION_VIEW',
]:
    assert token in launch, token

assert 'com.spocky.projengmenu.embedded.RedLightLaunchActivity' in manifest
assert 'android:exported="true"' in manifest
assert 'android:noHistory="true"' in manifest
assert 'android:launchMode="singleTop"' in manifest

with zipfile.ZipFile(apk) as z:
    c4 = z.read('classes4.dex')
    for token in [
        b'ProfileRestoreProvider',
        b'RedLightDiscoveryProvider',
        b'ChannelVisualBridge',
        b'RedLightLaunchActivity',
        b'ProjectivyRedLightLaunch',
        b'CLEAN09 forwarding kind=',
        b'org.xbmc.kodi.Splash',
        b'REDLIGHT_TARGET',
        b'REDLIGHT_KIND',
        b'projectivy-clean07',
        b'CLEAN085 killing process after raw opaque appearance commit',
        b'redlight:trakt:watchlist:movies',
        b'redlight:trakt:watchlist:shows',
    ]:
        assert token in c4, token
    assert b'CLEAN083 native appearance applied cardsBg=' not in c4
    assert b'CLEAN084 Android HOME intent dispatched after native appearance apply' not in c4
    assert b'CLEAN07I resume-sync' not in c4
    assert b'CLEAN07J live-adapter registered' not in c4

print('PASS: CLEAN09 = verified frozen CLEAN08.5 + exact two-file movie/episode playback delta')
