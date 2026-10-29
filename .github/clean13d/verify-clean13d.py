#!/usr/bin/env python3
import sys, zipfile, hashlib

if len(sys.argv)!=3:
    raise SystemExit('usage: verify-clean13d.py BASE CANDIDATE')
base,cand=sys.argv[1:]

def members(path):
    with zipfile.ZipFile(path) as z:
        return {i.filename:z.read(i.filename) for i in z.infolist() if not i.filename.startswith('META-INF/')}

b=members(base); c=members(cand)
if set(b)!=set(c):
    raise SystemExit('archive member set changed')
changed=[k for k in b if b[k]!=c[k]]
if sorted(changed)!=['classes2.dex','classes4.dex']:
    raise SystemExit('unexpected changed members: '+repr(changed))

# classes4 must contain the corrected CLEAN13D2 bridge while retaining the proven stack.
d4=c['classes4.dex']
for token in [
    b'ProjectivyStatusBar',
    b'CLEAN13D2 native clock/date actions added; unified default',
    b'Show date',
    b'Date format',
    b'Clock & date background',
    b'Combined with status icons',
    b'CLEAN13D visual mode=',
    b'CLEAN13C native clock APPLIED before=',
    b'CLEAN12.1 SUCCESS channels=',
    b'CLEAN12 discovery COMPLETE attempt=',
    b'CLEAN12 Kodi provider retention lease=',
    b'CLEAN10.3 browse clean uri=',
]:
    if token not in d4:
        raise SystemExit('classes4 marker missing: '+token.decode(errors='replace'))

# classes2 must call the bridge from the native pg Status Bar settings page.
d2=c['classes2.dex']
for token in [b'addClockDateActions', b'handleClockDateAction', b'StatusBarCustomizationBridge']:
    if token not in d2:
        raise SystemExit('classes2 bridge marker missing: '+token.decode())

print('PASS: CLEAN13D2 scope exact: only classes2.dex + classes4.dex changed')
print('classes2 sha256',hashlib.sha256(d2).hexdigest())
print('classes4 sha256',hashlib.sha256(d4).hexdigest())
