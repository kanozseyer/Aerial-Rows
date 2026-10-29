#!/usr/bin/env python3
from pathlib import Path
import re, sys

if len(sys.argv)!=2:
    raise SystemExit('usage: patch-pg-smali.py APKTOOL_ROOT')
root=Path(sys.argv[1])
paths=list(root.rglob('pg.smali'))
paths=[p for p in paths if 'smali_classes2' in str(p) or 'smali' in str(p)]
if not paths:
    raise SystemExit('pg.smali not found')
p=paths[0]
s=p.read_text()

CALL_ADD='    invoke-static {p0, p1}, Lcom/spocky/projengmenu/embedded/StatusBarCustomizationBridge;->addClockDateActions(Ljava/lang/Object;Ljava/util/List;)V\n'
CALL_HANDLE='''    invoke-static {p0, p1}, Lcom/spocky/projengmenu/embedded/StatusBarCustomizationBridge;->handleClockDateAction(Ljava/lang/Object;Ljava/lang/Object;)Z\n    move-result v0\n    if-eqz v0, :clean13d_native_continue\n    return-void\n\n    :clean13d_native_continue\n'''

def patch_method(text, sig, injection):
    start=text.find(sig)
    if start<0: raise SystemExit('method not found: '+sig)
    end=text.find('.end method', start)
    if end<0: raise SystemExit('method end not found: '+sig)
    block=text[start:end]
    if 'StatusBarCustomizationBridge' in block:
        raise SystemExit('method already patched: '+sig)
    m=re.search(r'(^\s*\.locals\s+)(\d+)', block, re.M)
    if not m:
        raise SystemExit('locals directive not found: '+sig)
    if int(m.group(2))<1:
        a,b=m.span(2)
        block=block[:a]+'1'+block[b:]
        m=re.search(r'(^\s*\.locals\s+\d+\s*\n)', block, re.M)
    else:
        m=re.search(r'(^\s*\.locals\s+\d+\s*\n)', block, re.M)
    pos=m.end()
    block=block[:pos]+'\n'+injection+block[pos:]
    return text[:start]+block+text[end:]

s=patch_method(s, '.method public final e0(Ljava/util/List;Landroid/os/Bundle;)V', CALL_ADD)
s=patch_method(s, '.method public final i0(Lmu1;)V', CALL_HANDLE)

if s.count('addClockDateActions')!=1 or s.count('handleClockDateAction')!=1:
    raise SystemExit('unexpected CLEAN13D bridge call count')
p.write_text(s)
print('PASS: patched',p)
