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

stock = '''    invoke-static {p0, v0, v2, v4, v5}, Lk00;->e(Lvu;Landroidx/preference/PreferenceGroup;Ljava/lang/String;Ljava/util/List;Z)Landroidx/preference/Preference;\n\n    .line 196\n    .line 197\n    .line 198\n    move-result-object v2\n'''
replacement = '''    # Embedded: providers exposing >1 channel use a real multi-select from the\n    # beginning. Do not construct Projectivy's radio ListPreference first.\n    invoke-interface {v4}, Ljava/util/Collection;->size()I\n    move-result v6\n    const/4 v7, 0x1\n    if-le v6, v7, :embedded_stock_group_picker\n\n    invoke-static {v0, v2, v4}, Lcom/spocky/projengmenu/embedded/ChannelSelectionBridge;->buildGroupedPreference(Landroidx/preference/PreferenceGroup;Ljava/lang/String;Ljava/util/List;)Landroidx/preference/Preference;\n    move-result-object v2\n    goto :embedded_group_picker_ready\n\n    :embedded_stock_group_picker\n    invoke-static {p0, v0, v2, v4, v5}, Lk00;->e(Lvu;Landroidx/preference/PreferenceGroup;Ljava/lang/String;Ljava/util/List;Z)Landroidx/preference/Preference;\n\n    .line 196\n    .line 197\n    .line 198\n    move-result-object v2\n\n    :embedded_group_picker_ready\n'''
if method.count(stock) != 1:
    raise SystemExit(f'grouped picker creation anchor mismatch: {method.count(stock)}')
method = method.replace(stock, replacement, 1)

n0 = '    invoke-virtual {p0, v2, v4}, Lb40;->n0(Landroidx/preference/Preference;Li5;)V\n'
if n0 not in method:
    raise SystemExit('representative channel n0 binding not found')
replacement_n0 = '''    instance-of v6, v2, Landroidx/preference/MultiSelectListPreference;\n    if-nez v6, :embedded_multiselect_group_done\n\n''' + n0 + '''\n    :embedded_multiselect_group_done\n'''
method = method.replace(n0, replacement_n0, 1)

text = text[:start] + method + text[end:]
path.write_text(text)
check = path.read_text()
for needle in (
    'ChannelSelectionBridge;->buildGroupedPreference',
    ':embedded_stock_group_picker',
    ':embedded_group_picker_ready',
    'Landroidx/preference/MultiSelectListPreference;',
    ':embedded_multiselect_group_done',
):
    if needle not in check:
        raise SystemExit(f'channel multi-select patch verification failed: {needle}')
if 'upgradeGroupedPreference' in check:
    raise SystemExit('obsolete replacement-based multi-select bridge still present')
print('patched grouped channel providers to direct multi-select creation')
