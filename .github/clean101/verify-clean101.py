#!/usr/bin/env python3
from pathlib import Path
import sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean101.py UNSIGNED_APK')
apk = Path(sys.argv[1])
base = Path('/tmp/Projectivy-CLEAN-09-1-Movie-Episode-Playback-Intent-Filter.apk')
verify = Path('/tmp/verify-clean101')
assert base.is_file(), base
assert apk.is_file(), apk

# CLEAN10.1 must be exactly one payload-file delta over runtime-proven CLEAN09.1.
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

# All real media types reach the trampoline, but kind=show is distinct.
for token in [
    'programIntent(c, e)',
    'type == 0 || type == 1 || type == 2',
    'type == 0 ? "movie" : (type == 1 ? "show" : "episode")',
    'RedLightLaunchActivity.EXTRA_TARGET',
    'RedLightLaunchActivity.EXTRA_KIND',
    'return programIdentityIntent(c, pluginUri)',
    'projectivy-clean07',
]:
    assert token in engine, token

# Frozen CLEAN09.1 playback block must still be present exactly for movie/episode.
for token in [
    'if ("show".equals(kind))',
    'Intent kodi = new Intent(Intent.ACTION_VIEW)',
    'kodi.setComponent(new ComponentName(KODI_PACKAGE, KODI_SPLASH))',
    'kodi.setData(Uri.parse(target))',
    'CLEAN09 forwarding kind=',
]:
    assert token in launch, token

# TV-show navigation must use Kodi GUI navigation rather than ACTION_VIEW of the folder.
for token in [
    'CLEAN10 browse show bootstrap',
    'CLEAN10 browse show ActivateWindow sent attempt=',
    'GUI.ActivateWindow',
    'params.put("window", "videos")',
    'parameters.put(target)',
    'parameters.put("return")',
    'new InetSocketAddress("127.0.0.1", 9090)',
    'ProjectivyRedLightBrowse',
]:
    assert token in launch, token

# CLEAN09.1 resolver/manifest remains byte-identical.
for token in [
    'com.spocky.projengmenu.embedded.RedLightLaunchActivity',
    'android:exported="true"',
    'android:noHistory="true"',
    'android:launchMode="singleTop"',
    'android.intent.action.VIEW',
    'android:scheme="plugin"',
    'android:host="plugin.video.redlight"',
    'android.permission.INTERNET',
]:
    assert token in manifest, token

# Key frozen CLEAN08.5/CLEAN09 invariants remain.
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
assert 'CLEAN083 native appearance applied cardsBg=' not in engine
assert 'CLEAN084 Android HOME intent dispatched after native appearance apply' not in engine
assert 'CLEAN07I resume-sync' not in engine
assert 'CLEAN07J live-adapter registered' not in engine

with zipfile.ZipFile(apk) as z:
    c4 = z.read('classes4.dex')
    for token in [
        b'RedLightLaunchActivity',
        b'ProjectivyRedLightLaunch',
        b'CLEAN09 forwarding kind=',
        b'CLEAN10 browse show bootstrap',
        b'CLEAN10 browse show ActivateWindow sent attempt=',
        b'GUI.ActivateWindow',
        b'127.0.0.1',
        b'ProjectivyRedLightBrowse',
        b'projectivy-clean07',
        b'CLEAN085 killing process after raw opaque appearance commit',
    ]:
        assert token in c4, token

print('PASS: CLEAN10.1 = runtime-proven CLEAN09.1 + classes4-only TV-show GUI navigation delta')
