#!/usr/bin/env python3
from pathlib import Path

root = Path('/tmp/pj131k')

def one(name: str) -> Path:
    hits = list(root.rglob(name))
    assert len(hits) == 1, (name, hits)
    return hits[0]

# LEFT LANE / ROW HEADERS
# IconHeaderImageView is used by Projectivy row/left-lane header layouts. Override
# setImageDrawable so every async-loaded app/channel/source icon is recolored at
# presentation time only. Underlying drawable/assets stay untouched.
icon = one('IconHeaderImageView.smali')
s = icon.read_text()
assert 'Lcom/spocky/projengmenu/ui/home/header/IconHeaderImageView;' in s
assert 'CLEAN13.1K_ICON_HEADER_OVERRIDE' not in s
anchor = '\n.method public final hasOverlappingRendering()Z\n'
assert s.count(anchor) == 1
method = r'''
# CLEAN13.1K_ICON_HEADER_OVERRIDE
.method public setImageDrawable(Landroid/graphics/drawable/Drawable;)V
    .locals 0

    invoke-super {p0, p1}, Lbd;->setImageDrawable(Landroid/graphics/drawable/Drawable;)V

    invoke-static {p0}, Lcom/spocky/projengmenu/embedded/MenuIconBridge;->tintHeaderIcon(Landroid/view/View;)V

    return-void
.end method
'''
s = s.replace(anchor, '\n' + method + anchor, 1)
icon.write_text(s)

# PROJECTIVY SETTINGS / MENUS
# bi3 is Projectivy's PreferenceViewHolder. Its s(id) method is the common view
# lookup used by preference binders. When the requested id is android.R.id.icon
# (0x1020006), apply an ImageView color filter. ImageView retains that filter when
# the preference binder subsequently supplies its drawable, so all preference
# icons use the current Projectivy UI icon color without changing any source asset.
bi3 = one('bi3.smali')
s = bi3.read_text()
assert '.class public final Lbi3;' in s
assert 'CLEAN13.1K_PREF_ICON_TINT' not in s
old1 = r'''    if-eqz v1, :cond_0

    .line 10
    .line 11
    return-object v1
'''
new1 = r'''    if-eqz v1, :cond_0

    # CLEAN13.1K_PREF_ICON_TINT cached icon view
    const v0, 0x1020006
    if-ne p1, v0, :clean13_1k_cached_done
    invoke-static {v1}, Lcom/spocky/projengmenu/embedded/MenuIconBridge;->tintPreferenceIcon(Landroid/view/View;)V
    :clean13_1k_cached_done

    .line 10
    .line 11
    return-object v1
'''
assert s.count(old1) == 1, f'bi3 cached return count={s.count(old1)}'
s = s.replace(old1, new1, 1)
old2 = r'''    .line 21
    .line 22
    .line 23
    :cond_1
    return-object p0
.end method
'''
new2 = r'''    .line 21
    .line 22
    .line 23
    :cond_1
    # CLEAN13.1K_PREF_ICON_TINT newly found icon view
    const v0, 0x1020006
    if-ne p1, v0, :clean13_1k_found_done
    invoke-static {p0}, Lcom/spocky/projengmenu/embedded/MenuIconBridge;->tintPreferenceIcon(Landroid/view/View;)V
    :clean13_1k_found_done
    return-object p0
.end method
'''
assert s.count(old2) == 1, f'bi3 final return count={s.count(old2)}'
s = s.replace(old2, new2, 1)
bi3.write_text(s)

# Exact-scope guards.
si = icon.read_text()
sb = bi3.read_text()
assert si.count('CLEAN13.1K_ICON_HEADER_OVERRIDE') == 1
assert si.count('MenuIconBridge;->tintHeaderIcon') == 1
assert sb.count('CLEAN13.1K_PREF_ICON_TINT') == 2
assert sb.count('MenuIconBridge;->tintPreferenceIcon') == 2

print('PASS: CLEAN13.1K icon patch = IconHeaderImageView + bi3(android.R.id.icon) only')
print('IconHeaderImageView:', icon)
print('bi3:', bi3)
