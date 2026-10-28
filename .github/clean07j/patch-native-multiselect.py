#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: patch-native-multiselect.py DECODED_ROOT')
root = Path(sys.argv[1])
ie2s=list(root.rglob('ie2.smali')); he2s=list(root.rglob('he2.smali')); fts=list(root.rglob('ft.smali'))
if len(ie2s)!=1 or len(he2s)!=1 or len(fts)!=1:
    raise SystemExit(f'expected one ie2/he2/ft, got {len(ie2s)}/{len(he2s)}/{len(fts)}')

ie2=ie2s[0]; s=ie2.read_text()
anchor='''    :cond_0\n    instance-of v1, p1, Landroidx/preference/ListPreference;\n'''
if anchor not in s: raise SystemExit('ie2 ListPreference anchor missing')
insert='''    :cond_0\n    invoke-static {p1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->isRedLightGroupedPreference(Ljava/lang/Object;)Z\n    move-result v1\n    if-eqz v1, :clean07j_stock_list\n\n    const/4 v1, 0x1\n    iput-boolean v1, p0, Lie2;->v0:Z\n    check-cast p1, Landroidx/preference/ListPreference;\n    iget-object v0, p1, Landroidx/preference/ListPreference;->o0:[Ljava/lang/CharSequence;\n    iput-object v0, p0, Lie2;->w0:[Ljava/lang/CharSequence;\n    iget-object v0, p1, Landroidx/preference/ListPreference;->p0:[Ljava/lang/CharSequence;\n    iput-object v0, p0, Lie2;->x0:[Ljava/lang/CharSequence;\n    invoke-static {p1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->getSelectedValues(Ljava/lang/Object;)Ljava/util/Set;\n    move-result-object p1\n    iput-object p1, p0, Lie2;->A0:Ljava/util/Set;\n    return-void\n\n    :clean07j_stock_list\n    instance-of v1, p1, Landroidx/preference/ListPreference;\n'''
s=s.replace(anchor,insert,1)

# CLEAN07J: register the adapter instance that is actually attached to the
# current VerticalGridView. Navigation can recreate ie2's view/adapter, so a
# reference captured by an earlier row click may point at a detached adapter.
view_anchor='''    :goto_0\n    invoke-virtual {p2, p3}, Landroidx/recyclerview/widget/RecyclerView;->setAdapter(Lfr3;)V\n'''
if view_anchor not in s:
    raise SystemExit('ie2 B() setAdapter anchor missing')
view_hook=view_anchor+'''\n    invoke-virtual {p0}, Lje2;->X()Landroidx/preference/DialogPreference;\n    move-result-object v0\n    invoke-static {v0}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->isRedLightGroupedPreference(Ljava/lang/Object;)Z\n    move-result v1\n    if-eqz v1, :clean07j_after_register\n    invoke-static {v0, p0, p3}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->registerSelector(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V\n    :clean07j_after_register\n'''
s=s.replace(view_anchor,view_hook,1)

# Retain CLEAN07I's real Fragment.onResume resync.
if '.method public H()V' in s:
    raise SystemExit('ie2 already overrides H/onResume')
resume='''\n.method public H()V\n    .locals 1\n\n    invoke-super {p0}, Lje2;->H()V\n\n    invoke-virtual {p0}, Lje2;->X()Landroidx/preference/DialogPreference;\n    move-result-object v0\n\n    invoke-static {v0, p0}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->onSelectorResumed(Ljava/lang/Object;Ljava/lang/Object;)V\n\n    return-void\n.end method\n'''
s=s.rstrip()+resume+'\n'
ie2.write_text(s)

# Red Light row click: one click opens exactly one stock channel detail page.
he2=he2s[0]; s=he2.read_text()
needle='''    move-result-object p0\n\n    .line 78\n    invoke-virtual {v2, p0}, Ljava/util/HashSet;->contains(Ljava/lang/Object;)Z\n'''
if needle not in s:
    needle='''    move-result-object p0\n\n    invoke-virtual {v2, p0}, Ljava/util/HashSet;->contains(Ljava/lang/Object;)Z\n'''
if needle not in s: raise SystemExit('he2 multi row string/contains anchor missing')
replacement=needle.split('    invoke-virtual {v2, p0}',1)[0]+'''    invoke-virtual {v0}, Lje2;->X()Landroidx/preference/DialogPreference;\n    move-result-object v1\n    invoke-static {v1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->isRedLightGroupedPreference(Ljava/lang/Object;)Z\n    move-result v3\n    if-eqz v3, :clean07j_native_multi\n\n    invoke-static {v1, p0, v0, p1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->openChannel(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)Z\n    invoke-virtual {p1}, Lfr3;->d()V\n    goto :goto_2\n\n    :clean07j_native_multi\n    invoke-virtual {v2, p0}, Ljava/util/HashSet;->contains(Ljava/lang/Object;)Z\n'''
s=s.replace(needle,replacement,1); he2.write_text(s)

# Retain 07H best-effort visibility hook.
ft=fts[0]; s=ft.read_text()
anchor='''    invoke-virtual {p0}, Lcom/spocky/projengmenu/ui/sidePanels/settings/preferenceFragment/CategoryAppsPreferencesFragment;->q0()V\n\n    .line 130\n    .line 131\n    .line 132\n    return v2\n'''
if anchor not in s:
    anchor='''    invoke-virtual {p0}, Lcom/spocky/projengmenu/ui/sidePanels/settings/preferenceFragment/CategoryAppsPreferencesFragment;->q0()V\n\n    return v2\n'''
if anchor not in s:
    call='    invoke-virtual {p0}, Lcom/spocky/projengmenu/ui/sidePanels/settings/preferenceFragment/CategoryAppsPreferencesFragment;->q0()V\n'
    pos=s.find(call)
    if pos<0: raise SystemExit('ft CategoryApps q0 anchor missing')
    insert_at=pos+len(call)
    hook='''\n    invoke-virtual {p0}, Lcom/spocky/projengmenu/ui/sidePanels/settings/preferenceFragment/CategoryAppsPreferencesFragment;->o0()Li5;\n    move-result-object p1\n    invoke-static {p1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->onCategoryVisibilityChanged(Ljava/lang/Object;)V\n'''
    s=s[:insert_at]+hook+s[insert_at:]
else:
    prefix=anchor.rsplit('    return v2\n',1)[0]
    replacement=prefix+'''    invoke-virtual {p0}, Lcom/spocky/projengmenu/ui/sidePanels/settings/preferenceFragment/CategoryAppsPreferencesFragment;->o0()Li5;\n    move-result-object p1\n    invoke-static {p1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->onCategoryVisibilityChanged(Ljava/lang/Object;)V\n    return v2\n'''
    s=s.replace(anchor,replacement,1)
ft.write_text(s)

its=ie2.read_text(); hts=he2.read_text(); fts=ft.read_text()
for t in [
    'getSelectedValues(Ljava/lang/Object;)Ljava/util/Set;',
    'registerSelector(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V',
    '.method public H()V',
    'invoke-super {p0}, Lje2;->H()V',
    'onSelectorResumed(Ljava/lang/Object;Ljava/lang/Object;)V',
]:
    if t not in its: raise SystemExit('missing ie2 CLEAN07J token '+t)
if 'openChannel(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)Z' not in hts:
    raise SystemExit('missing he2 openChannel')
if 'applySelectedValues(Ljava/lang/Object;Ljava/util/Set;)Z' in hts:
    raise SystemExit('CLEAN07J must not use whole-set applySelectedValues')
if 'onCategoryVisibilityChanged(Ljava/lang/Object;)V' not in fts:
    raise SystemExit('missing ft visibility hook')
print('PASS: CLEAN 07J registers the live attached Red Light adapter, then resume resync redraws that current adapter')
