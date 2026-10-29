#!/usr/bin/env python3
from pathlib import Path
import sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean10.py UNSIGNED_APK')
apk = Path(sys.argv[1])
base = Path('/tmp/Projectivy-CLEAN-09-1-Movie-Episode-Playback-Intent-Filter.apk')
verify10 = Path('/tmp/verify-clean10')
assert base.is_file(), base
assert apk.is_file(), apk

# CLEAN09 final is built and fully verified immediately before CLEAN10. Prove
# this stage changes exactly classes4.dex on top of that runtime-proven base.
with zipfile.ZipFile(base) as zb, zipfile.ZipFile(apk) as zn:
    bn = {i.filename for i in zb.infolist() if not i.filename.startswith('META-INF/')}
    nn = {i.filename for i in zn.infolist() if not i.filename.startswith('META-INF/')}
    assert bn == nn, (sorted(bn-nn), sorted(nn-bn))
    changed = [name for name in sorted(bn) if zb.read(name) != zn.read(name)]
    assert changed == ['classes4.dex'], changed
    assert zb.read('AndroidManifest.xml') == zn.read('AndroidManifest.xml')

engine = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
launch = Path('.github/clean09/RedLightLaunchActivity.java').read_text()
manifest = (verify10 / 'AndroidManifest.xml').read_text()

# CLEAN10 scope: all three actual media-card types use the same bridge. Other
# folders/navigation entries retain CLEAN07's inert identity path.
for token in [
    'programIntent(c, e)',
    'type == 0 || type == 1 || type == 2',
    'type == 0 ? "movie" : (type == 1 ? "show" : "episode")',
    'RedLightLaunchActivity.class.getName()',
    'RedLightLaunchActivity.EXTRA_TARGET',
    'RedLightLaunchActivity.EXTRA_KIND',
    'return programIdentityIntent(c, pluginUri)',
    'projectivy-clean07',
]:
    assert token in engine, token
assert '(type == 0 || type == 2)' not in engine

# The CLEAN09 trampoline itself is deliberately unchanged and still forwards
# only plugin.video.redlight URIs to Kodi's exported Splash activity.
for token in [
    'ProjectivyRedLightLaunch',
    'CLEAN09 forwarding kind=',
    'plugin://plugin.video.redlight/',
    'org.xbmc.kodi.Splash',
    'Intent.ACTION_VIEW',
]:
    assert token in launch, token

# CLEAN09.1's narrow resolver must survive byte-identically in the manifest.
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

# Key frozen CLEAN08.5 / CLEAN09 invariants remain in the rebuilt runtime.
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

print('PASS: CLEAN10 = runtime-proven CLEAN09 final + exact classes4-only TV-show navigation delta')
