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

# Keep CLEAN07F's stock Projectivy Leanback multi-choice presentation.
ie2 = ie2s[0]
s = ie2.read_text()
anchor = '''    :cond_0\n    instance-of v1, p1, Landroidx/preference/ListPreference;\n'''
if anchor not in s:
    raise SystemExit('ie2 ListPreference branch anchor missing')
insert = '''    :cond_0\n    invoke-static {p1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->isRedLightGroupedPreference(Ljava/lang/Object;)Z\n    move-result v1\n    if-eqz v1, :clean07g_stock_list\n\n    const/4 v1, 0x1\n    iput-boolean v1, p0, Lie2;->v0:Z\n    check-cast p1, Landroidx/preference/ListPreference;\n    iget-object v0, p1, Landroidx/preference/ListPreference;->o0:[Ljava/lang/CharSequence;\n    iput-object v0, p0, Lie2;->w0:[Ljava/lang/CharSequence;\n    iget-object v0, p1, Landroidx/preference/ListPreference;->p0:[Ljava/lang/CharSequence;\n    iput-object v0, p0, Lie2;->x0:[Ljava/lang/CharSequence;\n    invoke-static {p1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->getSelectedValues(Ljava/lang/Object;)Ljava/util/Set;\n    move-result-object p1\n    iput-object p1, p0, Lie2;->A0:Ljava/util/Set;\n    return-void\n\n    :clean07g_stock_list\n    instance-of v1, p1, Landroidx/preference/ListPreference;\n'''
s = s.replace(anchor, insert, 1)
ie2.write_text(s)

# CLEAN07G correction:
# Projectivy's grouped ListPreference callback is navigation into the selected
# channel's customization page; it is NOT itself the Visible toggle. CLEAN07F
# allowed the temporary HashSet mutation made by the native multi-choice row to
# become the displayed state after that navigation callback. If visibility was
# left unchanged, the row therefore looked unchecked until the whole preference
# screen was rebuilt.
#
# After forwarding the click through Projectivy's original callback, immediately
# discard that temporary selection Set and rebuild both ie2.A0 and ge2.g from the
# authoritative i5.n Visible flags exposed by ChannelVisualBridge.getSelectedValues.
# Thus the selector never lies about visibility while entering/leaving the detail
# page, and stale local Set drift cannot accumulate into changed=2/changed=N calls.
he2 = he2s[0]
s = he2.read_text()
anchor = '''    invoke-virtual {v0}, Lje2;->X()Landroidx/preference/DialogPreference;\n\n    .line 92\n    .line 93\n    .line 94\n    move-result-object v1\n\n    .line 95\n    check-cast v1, Landroidx/preference/MultiSelectListPreference;\n'''
if anchor not in s:
    anchor = '''    invoke-virtual {v0}, Lje2;->X()Landroidx/preference/DialogPreference;\n\n    move-result-object v1\n\n    check-cast v1, Landroidx/preference/MultiSelectListPreference;\n'''
if anchor not in s:
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
    special = '''\n    invoke-static {v1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->isRedLightGroupedPreference(Ljava/lang/Object;)Z\n    move-result v3\n    if-eqz v3, :clean07g_native_multi\n\n    new-instance v3, Ljava/util/HashSet;\n    invoke-direct {v3, v2}, Ljava/util/HashSet;-><init>(Ljava/util/Collection;)V\n    invoke-static {v1, v3}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->applySelectedValues(Ljava/lang/Object;Ljava/util/Set;)Z\n    move-result v3\n    if-eqz v3, :cond_4\n\n    invoke-static {v1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->getSelectedValues(Ljava/lang/Object;)Ljava/util/Set;\n    move-result-object v3\n    new-instance p0, Ljava/util/HashSet;\n    invoke-direct {p0, v3}, Ljava/util/HashSet;-><init>(Ljava/util/Collection;)V\n    iput-object p0, p1, Lge2;->g:Ljava/lang/Object;\n    iput-object p0, v0, Lie2;->A0:Ljava/util/Set;\n    goto :goto_1\n\n    :clean07g_native_multi\n'''
    s = s[:insert_at] + special + s[insert_at:]
else:
    prefix, suffix = anchor.rsplit('    check-cast v1, Landroidx/preference/MultiSelectListPreference;\n', 1)
    replacement = prefix + '''    invoke-static {v1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->isRedLightGroupedPreference(Ljava/lang/Object;)Z\n    move-result v3\n    if-eqz v3, :clean07g_native_multi\n\n    new-instance v3, Ljava/util/HashSet;\n    invoke-direct {v3, v2}, Ljava/util/HashSet;-><init>(Ljava/util/Collection;)V\n    invoke-static {v1, v3}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->applySelectedValues(Ljava/lang/Object;Ljava/util/Set;)Z\n    move-result v3\n    if-eqz v3, :cond_4\n\n    invoke-static {v1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->getSelectedValues(Ljava/lang/Object;)Ljava/util/Set;\n    move-result-object v3\n    new-instance p0, Ljava/util/HashSet;\n    invoke-direct {p0, v3}, Ljava/util/HashSet;-><init>(Ljava/util/Collection;)V\n    iput-object p0, p1, Lge2;->g:Ljava/lang/Object;\n    iput-object p0, v0, Lie2;->A0:Ljava/util/Set;\n    goto :goto_1\n\n    :clean07g_native_multi\n    check-cast v1, Landroidx/preference/MultiSelectListPreference;\n''' + suffix
    s = s.replace(anchor, replacement, 1)
he2.write_text(s)

its = ie2.read_text(); hts = he2.read_text()
for token in [
    'ChannelVisualBridge;->isRedLightGroupedPreference(Ljava/lang/Object;)Z',
    'ChannelVisualBridge;->getSelectedValues(Ljava/lang/Object;)Ljava/util/Set;',
    ':clean07g_stock_list',
]:
    if token not in its: raise SystemExit('missing ie2 CLEAN07G token: ' + token)
for token in [
    'ChannelVisualBridge;->applySelectedValues(Ljava/lang/Object;Ljava/util/Set;)Z',
    'ChannelVisualBridge;->getSelectedValues(Ljava/lang/Object;)Ljava/util/Set;',
    'iput-object p0, p1, Lge2;->g:Ljava/lang/Object;',
    'iput-object p0, v0, Lie2;->A0:Ljava/util/Set;',
    ':clean07g_native_multi',
]:
    if token not in hts: raise SystemExit('missing he2 CLEAN07G token: ' + token)
print('PASS: CLEAN 07G native Red Light selector restores its displayed Set from authoritative Projectivy visibility after channel navigation')
