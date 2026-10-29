#!/usr/bin/env python3
from pathlib import Path
import sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean11.py APK')
apk = Path(sys.argv[1])
assert apk.is_file(), apk

provider = Path('.github/clean07/RedLightDiscoveryProvider.java').read_text()
engine = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
launch = Path('.github/clean09/RedLightLaunchActivity.java').read_text()

# CLEAN11 must be a readiness-only delta: never start/foreground Kodi from the
# discovery provider. It only probes the already-proven native JSON-RPC port.
for token in [
    'CLEAN11 Kodi native not ready - cached rows retained; sync deferred',
    'CLEAN11 Kodi native ready - starting Red Light sync',
    'new InetSocketAddress("127.0.0.1", 9090)',
    'RedLightDiscoveryEngine.syncOnce(app)',
]:
    assert token in provider, token

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
        b'CLEAN11 Kodi native not ready - cached rows retained; sync deferred',
        b'CLEAN11 Kodi native ready - starting Red Light sync',
        b'CLEAN10.3 browse clean uri=',
        b'GUI.ActivateWindow',
        b'CLEAN09 forwarding kind=',
        b'build_season_list',
    ]:
        assert token in c4, token

print('PASS: CLEAN11 readiness-only gate present; frozen CLEAN10.3 routing retained')
