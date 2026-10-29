#!/usr/bin/env python3
import sys, zipfile
from pathlib import Path

if len(sys.argv) != 3:
    raise SystemExit('usage: verify-clean13b.py BASE.apk CANDIDATE.apk')
base, cand = map(Path, sys.argv[1:])
if not base.is_file() or not cand.is_file():
    raise SystemExit('CLEAN13B verifier input missing')

with zipfile.ZipFile(base) as zb, zipfile.ZipFile(cand) as zc:
    bn = {i.filename for i in zb.infolist() if not i.filename.startswith('META-INF/')}
    cn = {i.filename for i in zc.infolist() if not i.filename.startswith('META-INF/')}
    if bn != cn:
        raise SystemExit(f'CLEAN13B archive member set changed: missing={sorted(bn-cn)} extra={sorted(cn-bn)}')
    changed=[]
    for name in sorted(bn):
        if zb.read(name) != zc.read(name):
            changed.append(name)
    if changed != ['classes4.dex']:
        raise SystemExit(f'CLEAN13B scope violation; changed={changed}')

    dex = zc.read('classes4.dex')
    required = [
        b'CLEAN13B date clock APPLIED before=',
        b'CLEAN13B requesting safe Home recreate',
        b'appearance_statusbar_clock_format_l1',
        b'EEE d MMM',
        b'CLEAN12.1 SUCCESS channels=',
        b'CLEAN12 discovery COMPLETE attempt=',
        b'CLEAN12 refresh throttled ageMs=',
        b'CLEAN10.3 browse clean uri=',
        b'GUI.ActivateWindow',
    ]
    missing=[x.decode('utf-8','replace') for x in required if x not in dex]
    if missing:
        raise SystemExit('CLEAN13B DEX markers missing: '+repr(missing))
    forbidden=[b'CLEAN13 date clock APPLIED before=', b'CLEAN13 RESCUE']
    bad=[x.decode('utf-8','replace') for x in forbidden if x in dex]
    if bad:
        raise SystemExit('CLEAN13B old/broken markers survived: '+repr(bad))

print('PASS: CLEAN13B changes classes4.dex only; safe date lifecycle + frozen stack retained')
