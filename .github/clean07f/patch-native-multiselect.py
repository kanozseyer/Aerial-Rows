#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: patch-native-multiselect.py DECODED_ROOT')
root = Path(sys.argv[1])

ie2s = list(root.rglob('ie2.smali'))
he2s = list(root.rglob('he2.smali'))
if len(ie2s) != 1 or len(he2s) != 1:
    raise SystemExit(f'expected one ie2/he2, found ie2={len(ie2s)} he2={len(he2s)}')

# ie2 is Projectivy's Leanback list-preference dialog fragment. Keep the original
# ListPreference object, but put the bound Red Light preference into the fragment's
# already-shipped multi-choice adapter path and seed it from the real channel model.
ie2 = ie2s[0]
s = ie2.read_text()
anchor = '''    :cond_0\n    instance-of v1, p1, Landroidx/preference/ListPreference;\n'''
if anchor not in s:
    raise SystemExit('ie2 ListPreference branch anchor missing')
insert = '''    :cond_0\n    invoke-static {p1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->isRedLightGroupedPreference(Ljava/lang/Object;)Z\n    move-result v1\n    if-eqz v1, :clean07f_stock_list\n\n    const/4 v1, 0x1\n    iput-boolean v1, p0, Lie2;->v0:Z\n    check-cast p1, Landroidx/preference/ListPreference;\n    iget-object v0, p1, Landroidx/preference/ListPreference;->o0:[Ljava/lang/CharSequence;\n    iput-object v0, p0, Lie2;->w0:[Ljava/lang/CharSequence;\n    iget-object v0, p1, Landroidx/preference/ListPreference;->p0:[Ljava/lang/CharSequence;\n    iput-object v0, p0, Lie2;->x0:[Ljava/lang/CharSequence;\n    invoke-static {p1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->getSelectedValues(Ljava/lang/Object;)Ljava/util/Set;\n    move-result-object p1\n    iput-object p1, p0, Lie2;->A0:Ljava/util/Set;\n    return-void\n\n    :clean07f_stock_list\n    instance-of v1, p1, Landroidx/preference/ListPreference;\n'''
s = s.replace(anchor, insert, 1)
ie2.write_text(s)

# he2 handles row clicks. Projectivy's native multi path normally casts X() to
# MultiSelectListPreference; our Red Light preference intentionally remains the
# stock ListPreference so other providers are untouched. For only the bound Red
# Light preference, forward the desired Set<String> to the bridge, which invokes
# Projectivy's own per-channel visibility callback for just the changed IDs.
he2 = he2s[0]
s = he2.read_text()
anchor = '''    invoke-virtual {v0}, Lje2;->X()Landroidx/preference/DialogPreference;\n\n    .line 92\n    .line 93\n    .line 94\n    move-result-object v1\n\n    .line 95\n    check-cast v1, Landroidx/preference/MultiSelectListPreference;\n'''
if anchor not in s:
    # tolerate apktool line-number formatting changes while still requiring the
    # unique native multi-select cast immediately after X().
    anchor = '''    invoke-virtual {v0}, Lje2;->X()Landroidx/preference/DialogPreference;\n\n    move-result-object v1\n\n    check-cast v1, Landroidx/preference/MultiSelectListPreference;\n'''
if anchor not in s:
    # semantic fallback: locate the move-result and the following cast.
    call = '    invoke-virtual {v0}, Lje2;->X()Landroidx/preference/DialogPreference;\n'
    cast = '    check-cast v1, Landroidx/preference/MultiSelectListPreference;\n'
    p = s.find(call)
    p = s.find(call, p + 1) if p >= 0 else -1
    if p < 0:
        raise SystemExit('he2 multi-choice X() call missing')
    c = s.find(cast, p)
    if c < 0:
        raise SystemExit('he2 native MultiSelectListPreference cast missing')
    m = s.rfind('    move-result-object v1\n', p, c)
    if m < 0:
        m = s.find('    move-result-object v1\n', p, c)
    if m < 0:
        raise SystemExit('he2 multi-choice result register missing')
    insert_at = m + len('    move-result-object v1\n')
    special = '''\n    invoke-static {v1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->isRedLightGroupedPreference(Ljava/lang/Object;)Z\n    move-result v3\n    if-eqz v3, :clean07f_native_multi\n\n    new-instance v3, Ljava/util/HashSet;\n    invoke-direct {v3, v2}, Ljava/util/HashSet;-><init>(Ljava/util/Collection;)V\n    invoke-static {v1, v3}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->applySelectedValues(Ljava/lang/Object;Ljava/util/Set;)Z\n    move-result v3\n    if-eqz v3, :cond_4\n    iput-object v2, v0, Lie2;->A0:Ljava/util/Set;\n    goto :goto_1\n\n    :clean07f_native_multi\n'''
    s = s[:insert_at] + special + s[insert_at:]
else:
    prefix, suffix = anchor.rsplit('    check-cast v1, Landroidx/preference/MultiSelectListPreference;\n', 1)
    replacement = prefix + '''    invoke-static {v1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->isRedLightGroupedPreference(Ljava/lang/Object;)Z\n    move-result v3\n    if-eqz v3, :clean07f_native_multi\n\n    new-instance v3, Ljava/util/HashSet;\n    invoke-direct {v3, v2}, Ljava/util/HashSet;-><init>(Ljava/util/Collection;)V\n    invoke-static {v1, v3}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->applySelectedValues(Ljava/lang/Object;Ljava/util/Set;)Z\n    move-result v3\n    if-eqz v3, :cond_4\n    iput-object v2, v0, Lie2;->A0:Ljava/util/Set;\n    goto :goto_1\n\n    :clean07f_native_multi\n    check-cast v1, Landroidx/preference/MultiSelectListPreference;\n''' + suffix
    s = s.replace(anchor, replacement, 1)
he2.write_text(s)

# Strict local assertions.
its = ie2.read_text(); hts = he2.read_text()
for token in [
    'ChannelVisualBridge;->isRedLightGroupedPreference(Ljava/lang/Object;)Z',
    'ChannelVisualBridge;->getSelectedValues(Ljava/lang/Object;)Ljava/util/Set;',
    ':clean07f_stock_list',
]:
    if token not in its: raise SystemExit('missing ie2 CLEAN07F token: ' + token)
for token in [
    'ChannelVisualBridge;->isRedLightGroupedPreference(Ljava/lang/Object;)Z',
    'ChannelVisualBridge;->applySelectedValues(Ljava/lang/Object;Ljava/util/Set;)Z',
    ':clean07f_native_multi',
]:
    if token not in hts: raise SystemExit('missing he2 CLEAN07F token: ' + token)
print('PASS: CLEAN 07F patched Projectivy stock Leanback Red Light selector to native multi-choice behavior')
