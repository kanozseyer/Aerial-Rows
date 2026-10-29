#!/usr/bin/env python3
import sys, zipfile
from pathlib import Path

if len(sys.argv) != 3:
    raise SystemExit('usage: verify-clean13.py BASE.apk CANDIDATE.apk')
base, cand = map(Path, sys.argv[1:])
if not base.is_file() or not cand.is_file():
    raise SystemExit('CLEAN13 verifier input missing')

with zipfile.ZipFile(base) as zb, zipfile.ZipFile(cand) as zc:
    bn = {i.filename for i in zb.infolist() if not i.filename.startswith('META-INF/')}
    cn = {i.filename for i in zc.infolist() if not i.filename.startswith('META-INF/')}
    if bn != cn:
        raise SystemExit(f'CLEAN13 archive member set changed: missing={sorted(bn-cn)} extra={sorted(cn-bn)}')

    changed = []
    for name in sorted(bn):
        b = zb.read(name)
        c = zc.read(name)
        if b != c:
            changed.append(name)
    if changed != ['classes4.dex']:
        raise SystemExit(f'CLEAN13 scope violation; changed={changed}')

    dex = zc.read('classes4.dex')
    required = [
        b'CLEAN13 date clock APPLIED before=',
        b'CLEAN13 date clock already applied',
        b'appearance_statusbar_clock_format_l1',
        b'EEE d MMM',
        b'CLEAN12.1 SUCCESS channels=',
        b'CLEAN12 discovery COMPLETE attempt=',
        b'CLEAN12 Kodi provider retention lease=',
        b'CLEAN10.3 browse clean uri=',
    ]
    missing = [x.decode('utf-8', 'replace') for x in required if x not in dex]
    if missing:
        raise SystemExit('CLEAN13 DEX markers missing: ' + repr(missing))

print('PASS: CLEAN13 changes classes4.dex only; native clock/date + frozen stack markers retained')
