#!/usr/bin/env python3
from pathlib import Path
import sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean111.py APK')
apk = Path(sys.argv[1])
assert apk.is_file(), apk

provider = Path('.github/clean07/RedLightDiscoveryProvider.java').read_text()
engine = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
launch = Path('.github/clean09/RedLightLaunchActivity.java').read_text()

# CLEAN11.1: frozen CLEAN11 safety gate + stable provider retention lease.
for token in [
    'CLEAN11.1 Kodi native not ready - cached rows retained; no provider lease',
    'CLEAN11.1 Kodi provider lease ACQUIRED',
    'CLEAN11.1 Kodi provider retention lease=',
    'CLEAN11.1 Kodi native ready - starting Red Light sync',
    'ContentProviderClient',
    'acquireContentProviderClient(KODI_FILE_PROVIDER)',
    'content://org.xbmc.kodi.file',
    'new InetSocketAddress("127.0.0.1", 9090)',
    'RedLightDiscoveryEngine.syncOnce(app)',
]:
    assert token in provider, token

# Provider must never launch or foreground Kodi.
for forbidden in [
    'startActivity(',
    'getLaunchIntentForPackage(',
    'getLeanbackLaunchIntentForPackage(',
    'org.xbmc.kodi/.Main',
    'org.xbmc.kodi/.Splash',
]:
    assert forbidden not in provider, forbidden

# Frozen CLEAN10.3 routing/playback invariants stay present.
for token in [
    'type == 0 || type == 1 || type == 2',
    'source.getData()',
    'browseTarget.indexOf("#Intent;")',
    '"build_season_list".equals(mode)',
    'CLEAN10.3 browse clean uri=',
    'GUI.ActivateWindow',
    'CLEAN09 forwarding kind=',
    'redlight:trakt:watchlist:movies',
    'redlight:trakt:watchlist:shows',
]:
    assert token in engine + launch, token

with zipfile.ZipFile(apk) as z:
    c4 = z.read('classes4.dex')
    for token in [
        b'CLEAN11.1 Kodi native not ready - cached rows retained; no provider lease',
        b'CLEAN11.1 Kodi provider lease ACQUIRED',
        b'CLEAN11.1 Kodi native ready - starting Red Light sync',
        b'CLEAN10.3 browse clean uri=',
        b'GUI.ActivateWindow',
        b'CLEAN09 forwarding kind=',
        b'build_season_list',
    ]:
        assert token in c4, token

print('PASS: CLEAN11.1 retention lease present; CLEAN11 cold safety and CLEAN10.3 routing retained')
