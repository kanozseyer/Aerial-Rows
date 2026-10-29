#!/usr/bin/env python3
from pathlib import Path
import sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean102.py UNSIGNED_APK')
apk = Path(sys.argv[1])
base = Path('/tmp/Projectivy-CLEAN-09-1-Movie-Episode-Playback-Intent-Filter.apk')
verify = Path('/tmp/verify-clean102')
assert base.is_file(), base
assert apk.is_file(), apk

# CLEAN09.1 is rebuilt and fully verified first. CLEAN10.2 may change only classes4.dex.
with zipfile.ZipFile(base) as zb, zipfile.ZipFile(apk) as zn:
    bn = {i.filename for i in zb.infolist() if not i.filename.startswith('META-INF/')}
    nn = {i.filename for i in zn.infolist() if not i.filename.startswith('META-INF/')}
    assert bn == nn, (sorted(bn-nn), sorted(nn-bn))
    changed = [name for name in sorted(bn) if zb.read(name) != zn.read(name)]
    assert changed == ['classes4.dex'], changed
    assert zb.read('AndroidManifest.xml') == zn.read('AndroidManifest.xml')

engine = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
launch = Path('.github/clean09/RedLightLaunchActivity.java').read_text()
manifest = (verify / 'AndroidManifest.xml').read_text()

# TV-show cards join the bridge, but browse-vs-play is URI-authoritative because
# Projectivy can strip REDLIGHT_KIND while re-pinning preview-program intents.
for token in [
    'type == 0 || type == 1 || type == 2',
    'type == 0 ? "movie" : (type == 1 ? "show" : "episode")',
    'getQueryParameter("mode")',
    '"build_season_list".equals(mode)',
    'browseShow(target)',
    'CLEAN10 browse show bootstrap uri=',
    'CLEAN10 browse show ActivateWindow sent attempt=',
    'GUI.ActivateWindow',
    '127.0.0.1',
    '9090',
    'parameters.put(target)',
    'parameters.put("return")',
]:
    assert token in engine + launch, token

# Frozen CLEAN09.1 playable path remains present and unchanged in semantics.
for token in [
    'Intent kodi = new Intent(Intent.ACTION_VIEW)',
    'kodi.setComponent(new ComponentName(KODI_PACKAGE, KODI_SPLASH))',
    'kodi.setData(targetUri)',
    'Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP',
    'CLEAN09 forwarding kind=',
]:
    assert token in launch, token

# CLEAN09.1 manifest resolver must remain identical.
for token in [
    'com.spocky.projengmenu.embedded.RedLightLaunchActivity',
    'android:exported="true"',
    'android:noHistory="true"',
    'android:launchMode="singleTop"',
    'android.intent.action.VIEW',
    'android.intent.category.DEFAULT',
    'android:scheme="plugin"',
    'android:host="plugin.video.redlight"',
]:
    assert token in manifest, token

# Frozen historical invariants remain.
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
assert 'CLEAN083 native appearance applied cardsBg=' not in engine
assert 'CLEAN084 Android HOME intent dispatched after native appearance apply' not in engine
assert 'CLEAN07I resume-sync' not in engine
assert 'CLEAN07J live-adapter registered' not in engine

with zipfile.ZipFile(apk) as z:
    c4 = z.read('classes4.dex')
    for token in [
        b'ProfileRestoreProvider',
        b'RedLightDiscoveryProvider',
        b'ChannelVisualBridge',
        b'RedLightLaunchActivity',
        b'ProjectivyRedLightLaunch',
        b'CLEAN09 forwarding kind=',
        b'CLEAN10 browse show bootstrap uri=',
        b'CLEAN10 browse show ActivateWindow sent attempt=',
        b'build_season_list',
        b'GUI.ActivateWindow',
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

print('PASS: CLEAN10.2 = runtime-proven CLEAN09.1 + classes4-only URI-authoritative TV navigation')
