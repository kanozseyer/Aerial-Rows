#!/usr/bin/env python3
import sys, zipfile
from pathlib import Path

if len(sys.argv) != 3:
    raise SystemExit('usage: verify-clean13-rescue.py BASE.apk CANDIDATE.apk')
base, cand = map(Path, sys.argv[1:])
with zipfile.ZipFile(base) as zb, zipfile.ZipFile(cand) as zc:
    bn = {i.filename for i in zb.infolist() if not i.filename.startswith('META-INF/')}
    cn = {i.filename for i in zc.infolist() if not i.filename.startswith('META-INF/')}
    if bn != cn:
        raise SystemExit('archive member set changed')
    changed=[]
    for name in sorted(bn):
        if zb.read(name) != zc.read(name):
            changed.append(name)
    if changed != ['classes4.dex']:
        raise SystemExit(f'rescue scope violation changed={changed}')
    dex = zc.read('classes4.dex')
    for token in [
        b'CLEAN13 RESCUE removed clock format override',
        b'appearance_statusbar_clock_format_l1',
        b'CLEAN12.1 SUCCESS channels=',
        b'CLEAN12 discovery COMPLETE attempt=',
        b'CLEAN10.3 browse clean uri=',
        b'CLEAN12 Kodi provider retention lease=',
    ]:
        if token not in dex:
            raise SystemExit('missing rescue/frozen marker: '+repr(token))
print('PASS: CLEAN13 rescue changes classes4.dex only and retains frozen stack')
