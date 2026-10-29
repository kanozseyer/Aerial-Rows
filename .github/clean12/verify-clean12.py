#!/usr/bin/env python3
from pathlib import Path
import sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean12.py APK')
apk = Path(sys.argv[1])
assert apk.is_file(), apk

provider = Path('.github/clean07/RedLightDiscoveryProvider.java').read_text()
engine = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
launch = Path('.github/clean09/RedLightLaunchActivity.java').read_text()

# CLEAN12 is deliberately a lifecycle-only delta on top of proven CLEAN11.1.
for token in [
    'CLEAN12 discovery provider armed with cold gate + retention lease + 5m refresh throttle',
    'private static final long REFRESH_INTERVAL_MS = 5L * 60L * 1000L',
    'hasSuccessfulSync',
    'lastSuccessfulSyncElapsed',
    'CLEAN12 refresh throttled ageMs=',
    'mode=' + '" + (refresh ? "refresh" : "initial")',
    'CLEAN12 Kodi native not ready - cached rows retained; no provider lease',
    'CLEAN12 Kodi provider lease ACQUIRED',
    'CLEAN12 Kodi provider retention lease=',
    'acquireContentProviderClient(KODI_FILE_PROVIDER)',
    'new InetSocketAddress("127.0.0.1", 9090)',
    'RedLightDiscoveryEngine.syncOnce(app)',
]:
    assert token in provider, token

# CLEAN12 must stay registered after a successful sync and must never foreground Kodi.
for forbidden in [
    'unregisterActivityLifecycleCallbacks',
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
        b'CLEAN12 discovery provider armed with cold gate + retention lease + 5m refresh throttle',
        b'CLEAN12 refresh throttled ageMs=',
        b'CLEAN12 Kodi native not ready - cached rows retained; no provider lease',
        b'CLEAN12 Kodi provider lease ACQUIRED',
        b'CLEAN12 Kodi native ready - starting Red Light ',
        b'CLEAN10.3 browse clean uri=',
        b'GUI.ActivateWindow',
        b'CLEAN09 forwarding kind=',
        b'build_season_list',
    ]:
        assert token in c4, token

print('PASS: CLEAN12 throttled repeat refresh present; CLEAN11.1 retention and CLEAN10.3 routing retained')
