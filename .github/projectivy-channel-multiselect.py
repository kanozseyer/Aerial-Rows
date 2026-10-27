from pathlib import Path

root = Path('/tmp/projectivy-decoded')
paths = list(root.rglob('g70.smali'))
if len(paths) != 1:
    raise SystemExit(f'expected one g70.smali, found {len(paths)}: {paths}')
path = paths[0]
text = path.read_text()
start = text.index('.method public final p0()V')
end = text.index('.end method', start)
method = text[start:end]

# k00.e builds a ListPreference whenever one provider exposes multiple channels.
# Replace that returned preference with our MultiSelectListPreference bridge.  The
# bridge copies Projectivy's original entries/IDs and checks every category whose
# live i5.n visibility flag is true.
call = 'Lk00;->e(Lvu;Landroidx/preference/PreferenceGroup;Ljava/lang/String;Ljava/util/List;Z)Landroidx/preference/Preference;'
pos = method.find(call)
if pos < 0:
    raise SystemExit('grouped-channel k00.e call not found in g70.p0')
move_pos = method.find('    move-result-object v2\n', pos)
if move_pos < 0:
    raise SystemExit('grouped-channel result register not found')
move_end = move_pos + len('    move-result-object v2\n')
insert = '''\n    # Embedded: grouped channel providers are a true multi-select.\n    invoke-static {v0, v2, v4}, Lcom/spocky/projengmenu/embedded/ChannelSelectionBridge;->upgradeGroupedPreference(Landroidx/preference/PreferenceGroup;Landroidx/preference/Preference;Ljava/util/List;)Landroidx/preference/Preference;\n\n    move-result-object v2\n'''
if 'ChannelSelectionBridge;->upgradeGroupedPreference' not in method:
    method = method[:move_end] + insert + method[move_end:]

# Stock Projectivy then binds the provider preference to list.get(0), which is why
# one representative/last radio item appears selected. Preserve that binding for a
# singleton normal Preference, but skip it for our MultiSelect replacement.
n0 = '    invoke-virtual {p0, v2, v4}, Lb40;->n0(Landroidx/preference/Preference;Li5;)V\n'
if n0 not in method:
    raise SystemExit('representative channel n0 binding not found')
if ':embedded_multiselect_group_done' not in method:
    replacement = '''    instance-of v6, v2, Landroidx/preference/MultiSelectListPreference;\n\n    if-nez v6, :embedded_multiselect_group_done\n\n''' + n0 + '''\n    :embedded_multiselect_group_done\n'''
    method = method.replace(n0, replacement, 1)

text = text[:start] + method + text[end:]
path.write_text(text)
check = path.read_text()
for needle in (
    'ChannelSelectionBridge;->upgradeGroupedPreference',
    'Landroidx/preference/MultiSelectListPreference;',
    ':embedded_multiselect_group_done',
):
    if needle not in check:
        raise SystemExit(f'channel multi-select patch verification failed: {needle}')
print('patched Projectivy Edit Channels grouped providers to multi-select')
