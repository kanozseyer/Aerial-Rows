#!/usr/bin/env python3
from pathlib import Path
import os
import runpy
import shutil
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean07k.py UNSIGNED_APK')

# Reuse every CLEAN 07H invariant against the CLEAN 07K decode tree.
verify_k = Path('/tmp/verify-clean07k')
verify_h = Path('/tmp/verify-clean07h')
if verify_h.is_symlink() or verify_h.exists():
    if verify_h.is_dir() and not verify_h.is_symlink():
        shutil.rmtree(verify_h)
    else:
        verify_h.unlink()
os.symlink(verify_k, verify_h, target_is_directory=True)
runpy.run_path('.github/clean07h/verify-clean07h.py', run_name='__main__')

# 07K-specific invariant: the real CategoryPreferencesFragment category_visible
# handler must notify the bridge immediately after Projectivy writes i5.n.
frags = list(verify_k.rglob('com/spocky/projengmenu/ui/sidePanels/settings/preferenceFragment/CategoryPreferencesFragment.smali'))
assert len(frags) == 1, frags
text = frags[0].read_text()
start = text.find('.method public final b0(Landroidx/preference/Preference;)Z')
end = text.find('.end method', start)
assert start >= 0 and end >= 0
body = text[start:end]
write = 'iput-boolean v1, v0, Li5;->n:Z'
call = 'ChannelVisualBridge;->onCategoryVisibilityChanged(Ljava/lang/Object;)V'
assert write in body
assert call in body
assert body.find(write) < body.find(call)

# Explicitly reject the two failed experiments from 07I/07J.
ie2 = list(verify_k.rglob('ie2.smali'))
assert len(ie2) == 1
its = ie2[0].read_text()
assert 'onSelectorResumed(Ljava/lang/Object;Ljava/lang/Object;)V' not in its
assert 'registerLiveSelector(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V' not in its

print('PASS: CLEAN 07K retains CLEAN 07H and hooks only the real category_visible i5.n write; no 07I/07J code')
