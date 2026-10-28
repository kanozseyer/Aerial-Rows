#!/usr/bin/env python3
from pathlib import Path
import os, runpy, shutil, sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean09.py UNSIGNED_APK')
apk = Path(sys.argv[1])
verify09 = Path('/tmp/verify-clean09')
verify085 = Path('/tmp/verify-clean085')

# Reuse every frozen CLEAN08.5 / CLEAN08 / CLEAN07L invariant.
if verify085.is_symlink() or verify085.exists():
    if verify085.is_dir() and not verify085.is_symlink():
        shutil.rmtree(verify085)
    else:
        verify085.unlink()
os.symlink(verify09, verify085, target_is_directory=True)
runpy.run_path('.github/clean085/verify-clean085.py', run_name='__main__')

engine = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
launch = Path('.github/clean09/RedLightLaunchActivity.java').read_text()
manifest = (verify09 / 'AndroidManifest.xml').read_text()

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

# CLEAN10 is deliberately not included: TV-show/navigation cards still fall back
# to the inert identity route because only program types 0 and 2 are bridged.
assert 'type == 1' not in engine.split('private static Intent programIntent', 1)[1].split('private static Intent programIdentityIntent', 1)[0]

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

with zipfile.ZipFile(apk) as z:
    c4 = z.read('classes4.dex')
    for token in [
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

print('PASS: CLEAN09 = frozen CLEAN08.5 + movie/episode Red Light playback bridge only')
