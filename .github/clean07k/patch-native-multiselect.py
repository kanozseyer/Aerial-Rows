#!/usr/bin/env python3
from pathlib import Path
import runpy
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: patch-native-multiselect.py DECODED_ROOT')

# Start from the exact CLEAN 07H patch. 07K adds only one targeted hook in the
# real per-channel Visible switch callback; no 07I/07J lifecycle or adapter code.
runpy.run_path('.github/clean07h/patch-native-multiselect.py', run_name='__main__')

root = Path(sys.argv[1])
frags = list(root.rglob('com/spocky/projengmenu/ui/sidePanels/settings/preferenceFragment/CategoryPreferencesFragment.smali'))
if len(frags) != 1:
    raise SystemExit(f'expected one CategoryPreferencesFragment.smali, found {len(frags)}')

p = frags[0]
s = p.read_text()

# Projectivy 4.72's real category_visible handler in CategoryPreferencesFragment.b0:
#   switch.isChecked() -> i5.n
# Notify the already-proven CLEAN 07H Red Light bridge immediately after the
# authoritative Visible flag changes. The bridge is bound only to Red Light,
# so visibility changes for unrelated categories are ignored (selectors=0).
anchor = '''    iget-boolean v1, v1, Landroidx/preference/TwoStatePreference;->i0:Z\n\n    .line 67\n    .line 68\n    iput-boolean v1, v0, Li5;->n:Z\n'''
if anchor not in s:
    anchor = '''    iget-boolean v1, v1, Landroidx/preference/TwoStatePreference;->i0:Z\n\n    iput-boolean v1, v0, Li5;->n:Z\n'''
if anchor not in s:
    raise SystemExit('CategoryPreferencesFragment category_visible i5.n write anchor missing')

hook = anchor + '''\n    invoke-static {v0}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->onCategoryVisibilityChanged(Ljava/lang/Object;)V\n'''
s = s.replace(anchor, hook, 1)
p.write_text(s)

# Strictly verify that the hook landed inside b0() after the real n:Z write.
text = p.read_text()
start = text.find('.method public final b0(Landroidx/preference/Preference;)Z')
end = text.find('.end method', start)
if start < 0 or end < 0:
    raise SystemExit('CategoryPreferencesFragment.b0 method missing')
body = text[start:end]
write = 'iput-boolean v1, v0, Li5;->n:Z'
call = 'ChannelVisualBridge;->onCategoryVisibilityChanged(Ljava/lang/Object;)V'
if write not in body or call not in body:
    raise SystemExit('CLEAN07K exact Visible hook missing from b0')
if body.find(call) < body.find(write):
    raise SystemExit('CLEAN07K Visible hook must follow the i5.n write')

print('PASS: CLEAN 07K = CLEAN 07H plus exact CategoryPreferencesFragment category_visible callback -> Red Light selector sync')
