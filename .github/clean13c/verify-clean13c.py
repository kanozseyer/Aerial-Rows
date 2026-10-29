#!/usr/bin/env python3
import sys, zipfile
from pathlib import Path

if len(sys.argv) != 3:
    raise SystemExit('usage: verify-clean13c.py BASE.apk CANDIDATE.apk')
base, cand = map(Path, sys.argv[1:])
with zipfile.ZipFile(base) as zb, zipfile.ZipFile(cand) as zc:
    bn={i.filename for i in zb.infolist() if not i.filename.startswith('META-INF/')}
    cn={i.filename for i in zc.infolist() if not i.filename.startswith('META-INF/')}
    if bn != cn:
        raise SystemExit('archive member set changed')
    changed=[name for name in sorted(bn) if zb.read(name) != zc.read(name)]
    if changed != ['classes4.dex']:
        raise SystemExit(f'CLEAN13C scope violation changed={changed}')
    dex=zc.read('classes4.dex')
    for token in [
        b'CLEAN13C native clock APPLIED before=',
        b'CLEAN13C requesting safe Home recreate',
        b'CLEAN13C native clock FAILED',
        b'native_date_next_to_clock_v1',
        b'EEE d MMM',
        b'CLEAN12.1 SUCCESS channels=',
        b'CLEAN12 refresh throttled ageMs=',
        b'CLEAN10.3 browse clean uri=',
        b'CLEAN09 forwarding kind=',
    ]:
        if token not in dex:
            raise SystemExit('missing CLEAN13C/frozen marker: '+repr(token))
    for forbidden in [
        b'CLEAN13B date clock APPLIED',
        b'CLEAN13 date clock APPLIED',
        b'CLEAN07 PROGRAM CLEANUP channelId=',
    ]:
        if forbidden in dex:
            raise SystemExit('forbidden marker present: '+repr(forbidden))
print('PASS: CLEAN13C changes classes4.dex only and retains frozen CLEAN12.1B stack')
