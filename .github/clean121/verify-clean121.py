#!/usr/bin/env python3
from pathlib import Path
import sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean121.py APK')
apk = Path(sys.argv[1])
assert apk.is_file(), apk

provider = Path('.github/clean07/RedLightDiscoveryProvider.java').read_text()
engine = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
launch = Path('.github/clean09/RedLightLaunchActivity.java').read_text()

# CLEAN12 lifecycle and CLEAN11.1 retention invariants.
for token in [
    'CLEAN12 discovery provider armed with cold gate + retention lease + 5m refresh throttle',
    'REFRESH_INTERVAL_MS = 5L * 60L * 1000L',
    'CLEAN12 refresh throttled ageMs=',
    'CLEAN12 Kodi native not ready - cached rows retained; no provider lease',
    'acquireContentProviderClient(KODI_FILE_PROVIDER)',
    'new InetSocketAddress("127.0.0.1", 9090)',
]:
    assert token in provider, token

# CLEAN12.1 must reconcile existing PreviewPrograms rather than delete/reinsert whole rows.
for token in [
    'CLEAN12.1 SUCCESS channels=',
    'CLEAN12.1 CHANNEL key=',
    'loadExistingPrograms(c, channel)',
    'sameProgram(old, v)',
    'ExistingPrograms',
    'inserted=" + ps.inserted + " updated=" + ps.updated',
    'deleted=" + ps.deleted + " unchanged=" + ps.unchanged',
    'CLEAN12.1 existing-program query unavailable; cached row retained key=',
    '__duplicate__:',
]:
    assert token in engine, token

for forbidden in [
    'deleteProgramsForChannel(c, channel)',
    'CLEAN07 PROGRAM CLEANUP channelId=',
]:
    assert forbidden not in engine, forbidden

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
        b'CLEAN12.1 SUCCESS channels=',
        b'CLEAN12.1 CHANNEL key=',
        b'CLEAN12 refresh throttled ageMs=',
        b'CLEAN12 Kodi native not ready - cached rows retained; no provider lease',
        b'CLEAN10.3 browse clean uri=',
        b'GUI.ActivateWindow',
        b'CLEAN09 forwarding kind=',
        b'build_season_list',
    ]:
        assert token in c4, token
    assert b'CLEAN07 PROGRAM CLEANUP channelId=' not in c4

print('PASS: CLEAN12.1 differential PreviewProgram sync; CLEAN12 lifecycle and frozen routing retained')
