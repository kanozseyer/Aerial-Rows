#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: patch-monochrome-icons.py <apktool-decoded-dir>')
root = Path(sys.argv[1])

icons = list(root.rglob('IconHeaderImageView.smali'))
pq0s = list(root.rglob('pq0.smali'))
assert len(icons) == 1, f'IconHeaderImageView.smali count={len(icons)}'
assert len(pq0s) == 1, f'pq0.smali count={len(pq0s)}'
icon = icons[0]
pq0 = pq0s[0]

# Home row headers and the left-hand header rail both use this exact Projectivy
# custom ImageView. Tint only this view class so posters/cards/artwork stay untouched.
s = icon.read_text()
assert '.class public final Lcom/spocky/projengmenu/ui/home/header/IconHeaderImageView;' in s
assert '.super Lbd;' in s
assert '.method public setImageDrawable(Landroid/graphics/drawable/Drawable;)V' not in s

method = r'''

# CLEAN13.1K: every Projectivy header/row icon uses the same native monochrome
# colour formula as the stock settings icons. Activation alpha remains owned by
# IconHeaderImageView.setActivated(), exactly as stock Projectivy does.
.method public setImageDrawable(Landroid/graphics/drawable/Drawable;)V
    .locals 3

    invoke-super {p0, p1}, Lbd;->setImageDrawable(Landroid/graphics/drawable/Drawable;)V

    if-eqz p1, :cond_clean13_1k_done

    invoke-static {}, Lpq0;->u()I
    move-result v0

    const v1, 0x3ecccccd    # 0.4f
    const/4 v2, -0x1

    invoke-static {v1, v0, v2}, Lcc0;->d(FII)I
    move-result v0

    invoke-virtual {p0, v0}, Lcom/spocky/projengmenu/ui/home/header/IconHeaderImageView;->a(I)V

    :cond_clean13_1k_done
    return-void
.end method
'''
s += method
icon.write_text(s)

# AppPreference / AppCustomSwitchPreference use pq0.H: stock Projectivy only
# resizes their icons there, leaving SmartTube/etc full-colour. Apply the exact
# setColorFilter + alpha sequence already used by pq0.J for native menu icons.
p = pq0.read_text()
sig = '.method public static final H(Landroidx/preference/Preference;Lbi3;II)V'
start = p.index(sig)
end = p.index('.end method', start) + len('.end method')
old = p[start:end]
assert 'setColorFilter' not in old, 'pq0.H is already tinted; refusing double patch'
assert 'iput p2, p1, Landroid/view/ViewGroup$LayoutParams;->width:I' in old
assert 'iput p3, p0, Landroid/view/ViewGroup$LayoutParams;->height:I' in old

new = r'''.method public static final H(Landroidx/preference/Preference;Lbi3;II)V
    .locals 2

    const p0, 0x1020006
    invoke-virtual {p1, p0}, Lbi3;->s(I)Landroid/view/View;
    move-result-object p0

    instance-of p1, p0, Landroid/widget/ImageView;
    if-eqz p1, :cond_clean13_1k_done
    check-cast p0, Landroid/widget/ImageView;

    invoke-virtual {p0}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;
    move-result-object p1
    iput p2, p1, Landroid/view/ViewGroup$LayoutParams;->width:I

    invoke-virtual {p0}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;
    move-result-object p1
    iput p3, p1, Landroid/view/ViewGroup$LayoutParams;->height:I

    # Exact stock Projectivy monochrome preference-icon treatment from pq0.J.
    invoke-static {}, Lpq0;->u()I
    move-result p1
    const v0, 0x3ecccccd    # 0.4f
    const/4 v1, -0x1
    invoke-static {v0, p1, v1}, Lcc0;->d(FII)I
    move-result p1
    invoke-virtual {p0, p1}, Landroid/widget/ImageView;->setColorFilter(I)V

    const/high16 p1, 0x3f000000    # 0.5f
    invoke-virtual {p0, p1}, Landroid/view/View;->setAlpha(F)V

    :cond_clean13_1k_done
    return-void
.end method'''
p = p[:start] + new + p[end:]
pq0.write_text(p)

# Hard scope/implementation checks.
si = icon.read_text()
sp = pq0.read_text()
for token in [
    '.method public setImageDrawable(Landroid/graphics/drawable/Drawable;)V',
    'invoke-static {}, Lpq0;->u()I',
    'invoke-static {v1, v0, v2}, Lcc0;->d(FII)I',
    'IconHeaderImageView;->a(I)V',
]:
    assert token in si, token
h = sp[sp.index(sig):sp.index('.end method', sp.index(sig)) + len('.end method')]
for token in [
    'invoke-static {}, Lpq0;->u()I',
    'Lcc0;->d(FII)I',
    'ImageView;->setColorFilter(I)V',
    'View;->setAlpha(F)V',
]:
    assert token in h, token

print('PASS: CLEAN13.1K monochrome icon patch targets only IconHeaderImageView + pq0.H')
print('ICON_SMALI=' + str(icon))
print('PREFERENCE_SMALI=' + str(pq0))
