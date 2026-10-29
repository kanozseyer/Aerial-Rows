#!/usr/bin/env python3
from pathlib import Path

p=Path('.github/clean07/RedLightDiscoveryProvider.java')
s=p.read_text()
old='''                if (applyClean13CDateClock(a)) return;\n\n                long now = SystemClock.elapsedRealtime();\n'''
new='''                if (applyClean13CDateClock(a)) return;\n                StatusBarCustomizationBridge.applyToHome(a);\n\n                long now = SystemClock.elapsedRealtime();\n'''
if s.count(old)!=1:
    raise SystemExit(f'CLEAN13D resume anchor mismatch: {s.count(old)}')
s=s.replace(old,new,1)
p.write_text(s)
for token in ['StatusBarCustomizationBridge.applyToHome(a);','CLEAN13C native clock APPLIED before=','CLEAN12 refresh throttled ageMs=']:
    if token not in s: raise SystemExit('missing '+token)
print('PASS: CLEAN13D Home styling wired after CLEAN13C migration')
