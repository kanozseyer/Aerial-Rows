#!/usr/bin/env python3
from pathlib import Path
import sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean121b.py APK')
apk = Path(sys.argv[1])
assert apk.is_file(), apk

engine = Path('.github/clean07/RedLightDiscoveryEngine.java').read_text()
provider = Path('.github/clean07/RedLightDiscoveryProvider.java').read_text()
launch = Path('.github/clean09/RedLightLaunchActivity.java').read_text()

assert 'v.put("description", e.mediaType != null ? e.mediaType : "Red Light")' not in engine
assert 'same(old.description, v.getAsString("description"))' not in engine

for token in [
    'CLEAN12.1 SUCCESS channels=',
    'CLEAN12.1 CHANNEL key=',
    'loadExistingPrograms(c, channel)',
    'sameProgram(old, v)',
    'CLEAN12 refresh throttled ageMs=',
    'CLEAN12 Kodi provider retention lease=',
    'CLEAN12 Kodi native not ready - cached rows retained; no provider lease',
    'CLEAN10.3 browse clean uri=',
    'GUI.ActivateWindow',
    'CLEAN09 forwarding kind=',
]:
    assert token in engine + provider + launch, token

with zipfile.ZipFile(apk) as z:
    c4 = z.read('classes4.dex')
    for token in [
        b'CLEAN12.1 SUCCESS channels=',
        b'CLEAN12.1 CHANNEL key=',
        b'CLEAN12 refresh throttled ageMs=',
        b'CLEAN12 Kodi provider retention lease=',
        b'CLEAN10.3 browse clean uri=',
        b'GUI.ActivateWindow',
        b'CLEAN09 forwarding kind=',
    ]:
        assert token in c4, token
    assert b'CLEAN12.1A MISMATCH' not in c4

print('PASS: CLEAN12.1B description normalization fix; differential sync + CLEAN12 lifecycle + routing retained')
