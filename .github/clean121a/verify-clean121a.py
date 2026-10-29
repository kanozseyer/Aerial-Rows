#!/usr/bin/env python3
from pathlib import Path
import sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean121a.py APK')
apk = Path(sys.argv[1])
assert apk.is_file(), apk

engine = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
provider = Path('.github/clean07/RedLightDiscoveryProvider.java').read_text()
launch = Path('.github/clean09/RedLightLaunchActivity.java').read_text()

for token in [
    'CLEAN12.1A MISMATCH id=',
    'clean121aMismatchLogs < 8',
    'CLEAN12.1 SUCCESS channels=',
    'CLEAN12.1 CHANNEL key=',
    'loadExistingPrograms(c, channel)',
    'sameProgram(old, v)',
    'CLEAN12 refresh throttled ageMs=',
    'CLEAN12 Kodi provider retention lease=',
    'CLEAN10.3 browse clean uri=',
    'GUI.ActivateWindow',
    'CLEAN09 forwarding kind=',
]:
    assert token in engine + provider + launch, token

assert 'deleteProgramsForChannel(c, channel)' not in engine
assert 'CLEAN07 PROGRAM CLEANUP channelId=' not in engine

with zipfile.ZipFile(apk) as z:
    c4 = z.read('classes4.dex')
    for token in [
        b'CLEAN12.1A MISMATCH id=',
        b'CLEAN12.1 SUCCESS channels=',
        b'CLEAN12 refresh throttled ageMs=',
        b'CLEAN10.3 browse clean uri=',
        b'GUI.ActivateWindow',
        b'CLEAN09 forwarding kind=',
    ]:
        assert token in c4, token
    assert b'CLEAN07 PROGRAM CLEANUP channelId=' not in c4

print('PASS: CLEAN12.1a diagnostics added; differential sync/lifecycle/routing retained')
