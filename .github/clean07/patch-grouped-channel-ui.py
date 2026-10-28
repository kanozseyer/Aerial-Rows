#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: patch-grouped-channel-ui.py DECODED_ROOT')
root = Path(sys.argv[1])

g70s = list(root.rglob('g70.smali'))
if len(g70s) != 1:
    raise SystemExit(f'expected one g70.smali, found {len(g70s)}')
g70 = g70s[0]
text = g70.read_text()
start = text.index('.method public final p0()V')
end = text.index('.end method', start)
method = text[start:end]

hook = 'Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->decorateGroupedPreference(Ljava/lang/Object;Ljava/util/List;)V'
if hook in method:
    raise SystemExit('grouped-channel UI hook already present')

call = 'Lk00;->e(Lvu;Landroidx/preference/PreferenceGroup;Ljava/lang/String;Ljava/util/List;Z)Landroidx/preference/Preference;'
pos = method.find(call)
if pos < 0:
    raise SystemExit('Projectivy grouped-channel k00.e call missing')
move = '    move-result-object v2\n'
move_pos = method.find(move, pos)
if move_pos < 0:
    raise SystemExit('Projectivy grouped-channel preference result register missing')
insert_at = move_pos + len(move)
insert = '\n    invoke-static {v2, v4}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->decorateGroupedPreference(Ljava/lang/Object;Ljava/util/List;)V\n'
method = method[:insert_at] + insert + method[insert_at:]
text = text[:start] + method + text[end:]
g70.write_text(text)

# Strict local verification.
patched = g70.read_text()
if patched.count(hook) != 1:
    raise SystemExit('expected exactly one grouped-channel UI hook')
print('PASS: CLEAN 07B hooked stock Projectivy grouped-channel preference to visual bridge')
