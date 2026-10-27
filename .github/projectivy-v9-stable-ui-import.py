from pathlib import Path
import re

root = Path('/tmp/projectivy-decoded')

# 1) VISUAL-ONLY grouped-channel selection fix. Projectivy keeps its stock
# ListPreference and, more importantly, its stock ft/f70 visibility callback.
# We only decorate every live i5.n==true row with a check and clear the misleading
# single radio value.
g70s = list(root.rglob('g70.smali'))
if len(g70s) != 1:
    raise SystemExit(f'expected one g70.smali, found {len(g70s)}')
g70 = g70s[0]
text = g70.read_text()
start = text.index('.method public final p0()V')
end = text.index('.end method', start)
method = text[start:end]
call = 'Lk00;->e(Lvu;Landroidx/preference/PreferenceGroup;Ljava/lang/String;Ljava/util/List;Z)Landroidx/preference/Preference;'
pos = method.find(call)
if pos < 0:
    raise SystemExit('grouped-channel k00.e call not found')
move = '    move-result-object v2\n'
move_pos = method.find(move, pos)
if move_pos < 0:
    raise SystemExit('grouped-channel result register not found')
move_end = move_pos + len(move)
insert = '''\n    # Embedded stable v9: keep Projectivy toggle logic, fix only checked-state rendering.\n    invoke-static {v2, v4}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->decorateGroupedPreference(Ljava/lang/Object;Ljava/util/List;)V\n'''
if 'ChannelVisualBridge;->decorateGroupedPreference' not in method:
    method = method[:move_end] + insert + method[move_end:]
text = text[:start] + method + text[end:]
g70.write_text(text)

# 2) Projectivy's import activity unnecessarily requires ACTION_VIEW even when a
# valid content URI/EXTRA_STREAM is present. Some Shield file pickers return the
# chosen .plbackup with a different action. Relax only the action check; a URI is
# still mandatory and stock decryption/import remains untouched.
imports = list(root.rglob('ImportSettingsActivity.smali'))
if len(imports) != 1:
    raise SystemExit(f'expected one ImportSettingsActivity.smali, found {len(imports)}')
imp = imports[0]
text = imp.read_text()
start = text.index('.method public v()V') if '.method public v()V' in text else text.index('.method public final v()V')
end = text.index('.end method', start)
method = text[start:end]
view_pos = method.find('android.intent.action.VIEW')
if view_pos < 0:
    raise SystemExit('ACTION_VIEW guard not found in ImportSettingsActivity.v')
post = method[view_pos:]
m = re.search(r'\n\s*move-result\s+(v\d+)\s*\n(?:\s*\.line[^\n]*\n)*\s*if-eqz\s+\1,\s*(:cond_[0-9a-f]+)', post)
if not m:
    # Apktool line directives can vary; search the first matching result/branch pair.
    m = re.search(r'move-result\s+(v\d+)[\s\S]{0,180}?if-eqz\s+\1,\s*(:cond_[0-9a-f]+)', post)
if not m:
    raise SystemExit('ACTION_VIEW result branch not found')
branch_text = re.search(r'if-eqz\s+' + re.escape(m.group(1)) + r',\s*' + re.escape(m.group(2)), m.group(0)).group(0)
post = post.replace(branch_text, 'nop    # embedded: accept any action carrying a valid backup URI', 1)
method = method[:view_pos] + post
text = text[:start] + method + text[end:]
imp.write_text(text)

# 3) A successful settings import must disable the bundled first-run profile from
# ever re-applying later. Store this marker outside Projectivy prefs so it is not
# itself exported in future .plbackup files.
dj3s = list(root.rglob('dj3.smali'))
if len(dj3s) != 1:
    raise SystemExit(f'expected one dj3.smali, found {len(dj3s)}')
dj3 = dj3s[0]
text = dj3.read_text()
start = text.index('.method public final i(Ljava/lang/String;Lhi3;Lzi0;)Ljava/lang/Object;')
end = text.index('.end method', start)
method = text[start:end]
anchor = '''    invoke-static {p0, p1}, Ldj3;->h(Landroid/content/SharedPreferences;Ljava/lang/String;)V\n'''
if method.count(anchor) != 1:
    raise SystemExit(f'dj3 settings import merge anchor mismatch: {method.count(anchor)}')
insert = anchor + '''\n    sget-object p0, Lhi3;->u:Lhi3;\n    if-ne p2, p0, :embedded_manual_import_marked\n    invoke-static {}, Lq32;->q()Lcom/spocky/projengmenu/PTApplication;\n    move-result-object p0\n    invoke-static {p0}, Lcom/spocky/projengmenu/embedded/StableProfileBridge;->onManualImportSucceeded(Landroid/content/Context;)V\n    :embedded_manual_import_marked\n'''
method = method.replace(anchor, insert, 1)
text = text[:start] + method + text[end:]
dj3.write_text(text)

# Verification
for path, needle in [
    (g70, 'ChannelVisualBridge;->decorateGroupedPreference'),
    (imp, 'accept any action carrying a valid backup URI'),
    (dj3, 'StableProfileBridge;->onManualImportSucceeded'),
]:
    if needle not in path.read_text():
        raise SystemExit(f'v9 stable UI/import patch verification failed: {needle}')
print('patched grouped-channel checked-state rendering and robust settings import')
