#!/usr/bin/env python3
from pathlib import Path
import re, sys

if len(sys.argv) != 2:
    raise SystemExit('usage: patch-native-visibility-multiselect.py DECODED_ROOT')
root = Path(sys.argv[1])

# CLEAN 07F does not create another dialog. It switches Projectivy's own ie2
# Leanback list into its already-existing multi-choice adapter path for our
# registered Red Light grouped preference, then intercepts he2's native multi
# click branch before its MultiSelectListPreference cast.

ie2s = list(root.rglob('ie2.smali'))
he2s = list(root.rglob('he2.smali'))
if len(ie2s) != 1:
    raise SystemExit(f'expected one ie2.smali, found {len(ie2s)}')
if len(he2s) != 1:
    raise SystemExit(f'expected one he2.smali, found {len(he2s)}')
ie2, he2 = ie2s[0], he2s[0]

# --- ie2.A(Bundle): after stock ListPreference entries/values are captured but
# before its single selected q0 is copied, let the bridge opt this exact Red Light
# group into ie2's native multi mode and initialize A0 from real category Visible.
s = ie2.read_text()
if 'configureNativeVisibilityDialog' in s:
    raise SystemExit('ie2 CLEAN07F hook already present')
needle = '    iget-object p1, p1, Landroidx/preference/ListPreference;->q0:Ljava/lang/String;\n'
pos = s.find(needle)
if pos < 0:
    raise SystemExit('ie2 ListPreference q0 anchor missing')
insert = '''    invoke-static {p0, p1}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->configureNativeVisibilityDialog(Ljava/lang/Object;Ljava/lang/Object;)Z\n\n    move-result v0\n\n    if-eqz v0, :clean07f_stock_single\n\n    return-void\n\n    :clean07f_stock_single\n'''
s = s[:pos] + insert + s[pos:]
ie2.write_text(s)

# --- he2.onClick(View): native multi mode already toggles the adapter HashSet.
# Before Projectivy assumes the underlying preference is a real
# MultiSelectListPreference, give CLEAN07F the fragment + requested Set. For our
# synthetic Red Light visibility selector it consumes the click and keeps the
# same panel open. Genuine MultiSelectListPreference users fall through unchanged.
s = he2.read_text()
if 'applyNativeVisibilitySelection' in s:
    raise SystemExit('he2 CLEAN07F hook already present')
method_start = s.find('.method public final onClick(Landroid/view/View;)V')
method_end = s.find('.end method', method_start)
if method_start < 0 or method_end < 0:
    raise SystemExit('he2 onClick method missing')
method = s[method_start:method_end]
needle = '    :goto_0\n    invoke-virtual {v0}, Lje2;->X()Landroidx/preference/DialogPreference;\n'
pos = method.find(needle)
if pos < 0:
    # tolerate apktool line directives between label and invoke
    m = re.search(r'(    :goto_0\n)(?:(?:    \.line[^\n]*\n)|\s)*?(    invoke-virtual \{v0\}, Lje2;->X\(\)Landroidx/preference/DialogPreference;\n)', method)
    if not m:
        raise SystemExit('he2 native multi pre-cast anchor missing')
    pos = m.start(2)
else:
    pos += len('    :goto_0\n')
insert = '''    invoke-static {v0, v2}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->applyNativeVisibilitySelection(Ljava/lang/Object;Ljava/util/Set;)Z\n\n    move-result v1\n\n    if-eqz v1, :clean07f_real_multiselect\n\n    iput-object v2, v0, Lie2;->A0:Ljava/util/Set;\n\n    invoke-virtual {p1}, Lfr3;->d()V\n\n    goto :goto_2\n\n    :clean07f_real_multiselect\n'''
method = method[:pos] + insert + method[pos:]
s = s[:method_start] + method + s[method_end:]
he2.write_text(s)

# Strict local proof: exactly one hook in each real Projectivy class and no
# replacement of normal single-select navigation outside the Red Light opt-in.
ie = ie2.read_text(); he = he2.read_text()
assert ie.count('configureNativeVisibilityDialog') == 1
assert he.count('applyNativeVisibilitySelection') == 1
assert 'check-cast v1, Landroidx/preference/MultiSelectListPreference;' in he
assert 'check-cast v3, Landroidx/preference/ListPreference;' in he
print('PASS: CLEAN 07F hooked Projectivy native Leanback selector to Red Light visibility multi mode')
