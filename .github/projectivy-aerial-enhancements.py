from pathlib import Path

root = Path('/tmp/projectivy-decoded')
aerial_id = '8c00feaf-c7be-43c2-80f9-d49853aaa99a'

def replace_once(path: Path, old: str, new: str, label: str):
    text = path.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected 1 anchor, got {count} in {path}')
    path.write_text(text.replace(old, new, 1))
    print('patched', label)

# Fix Projectivy's Configure action only for Aerial Rows. The current provider APK
# uses applicationId com.kanozseyer.aerialrows.wallpaper while SettingsActivity
# lives in com.kanozseyer.aerialrows.projectivy, so Projectivy's normal
# package + relative-class concatenation points at a non-existent component.
ze = root / 'smali_classes2/ze.smali'
old_cfg = '''    move-object v1, p1\n\n    .line 172\n    check-cast v1, Ly05;\n\n    .line 173\n    .line 174\n    iget-object v1, v1, Ly05;->j:Ljava/lang/String;\n\n    .line 175\n    .line 176\n    move-object v2, p1\n\n    .line 177\n    check-cast v2, Ly05;\n\n    .line 178\n    .line 179\n    iget-object v2, v2, Ly05;->j:Ljava/lang/String;\n\n    .line 180\n    .line 181\n    check-cast p1, Ly05;\n\n    .line 182\n    .line 183\n    iget-object p1, p1, Ly05;->h:Ljava/lang/String;\n\n    .line 184\n    .line 185\n    invoke-virtual {v2, p1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;\n\n    .line 186\n    .line 187\n    .line 188\n    move-result-object p1\n\n    .line 189\n    invoke-virtual {v0, v1, p1}, Landroid/content/Intent;->setClassName(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;'''
new_cfg = f'''    move-object v1, p1\n    check-cast v1, Ly05;\n    iget-object v2, v1, Ly05;->e:Ljava/lang/String;\n    const-string v3, "{aerial_id}"\n    invoke-virtual {{v3, v2}}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z\n    move-result v2\n    if-eqz v2, :aerial_config_normal\n\n    const-string v1, "com.kanozseyer.aerialrows.wallpaper"\n    const-string p1, "com.kanozseyer.aerialrows.projectivy.SettingsActivity"\n    goto :aerial_config_set_component\n\n    :aerial_config_normal\n    move-object v1, p1\n    check-cast v1, Ly05;\n    iget-object v1, v1, Ly05;->j:Ljava/lang/String;\n    move-object v2, p1\n    check-cast v2, Ly05;\n    iget-object v2, v2, Ly05;->j:Ljava/lang/String;\n    check-cast p1, Ly05;\n    iget-object p1, p1, Ly05;->h:Ljava/lang/String;\n    invoke-virtual {{v2, p1}}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;\n    move-result-object p1\n\n    :aerial_config_set_component\n    invoke-virtual {{v0, v1, p1}}, Landroid/content/Intent;->setClassName(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;'''
replace_once(ze, old_cfg, new_cfg, 'Aerial Configure component')

# Projectivy multi-row/category backdrop -> translucent tint + bright glass rim.
ox3 = root / 'smali_classes2/ox3.smali'
replace_once(
    ox3,
    '''    iget p3, p0, Lox3;->a:I\n\n    .line 63\n    .line 64\n    invoke-virtual {p2, p3}, Landroid/graphics/Paint;->setColor(I)V''',
    '''    iget p3, p0, Lox3;->a:I\n    const p4, 0x00ffffff\n    and-int/2addr p3, p4\n    const p4, 0x66000000\n    or-int/2addr p3, p4\n    invoke-virtual {p2, p3}, Landroid/graphics/Paint;->setColor(I)V''',
    'glass dock fill',
)
replace_once(
    ox3,
    '''    iget p3, p0, Lox3;->f:I\n\n    .line 88\n    .line 89\n    invoke-virtual {p2, p3}, Landroid/graphics/Paint;->setColor(I)V''',
    '''    const p3, 0x70ffffff\n    invoke-virtual {p2, p3}, Landroid/graphics/Paint;->setColor(I)V''',
    'glass dock rim',
)
replace_once(
    ox3,
    '''    iget-object v0, p0, Lox3;->g:Landroid/graphics/Paint;\n\n    .line 8\n    .line 9\n    invoke-virtual {v0, p1}, Landroid/graphics/Paint;->setColor(I)V''',
    '''    iget-object v0, p0, Lox3;->g:Landroid/graphics/Paint;\n    move v1, p1\n    const p1, 0x00ffffff\n    and-int/2addr v1, p1\n    const p1, 0x66000000\n    or-int/2addr v1, p1\n    invoke-virtual {v0, v1}, Landroid/graphics/Paint;->setColor(I)V\n    iget p1, p0, Lox3;->a:I''',
    'glass dock live fill',
)
replace_once(
    ox3,
    '''    iget-object p0, p0, Lox3;->h:Landroid/graphics/Paint;\n\n    .line 43\n    .line 44\n    invoke-virtual {p0, p1}, Landroid/graphics/Paint;->setColor(I)V''',
    '''    iget-object p0, p0, Lox3;->h:Landroid/graphics/Paint;\n    const p1, 0x70ffffff\n    invoke-virtual {p0, p1}, Landroid/graphics/Paint;->setColor(I)V''',
    'glass dock live rim',
)

# Glass backing behind every card. Transparent app/icon artwork shows the glass;
# full-bleed posters naturally cover it.
lo2 = root / 'smali/lo2.smali'
replace_once(
    lo2,
    '''    invoke-virtual {p0, v2}, Landroid/view/View;->setForeground(Landroid/graphics/drawable/Drawable;)V\n\n    .line 49\n    .line 50\n    .line 51\n    sget-boolean v1, Lu9;->g:Z''',
    '''    invoke-virtual {p0, v2}, Landroid/view/View;->setForeground(Landroid/graphics/drawable/Drawable;)V\n\n    new-instance v1, Landroid/graphics/drawable/GradientDrawable;\n    invoke-direct {v1}, Landroid/graphics/drawable/GradientDrawable;-><init>()V\n    const v3, 0x4d000000\n    invoke-virtual {v1, v3}, Landroid/graphics/drawable/GradientDrawable;->setColor(I)V\n    const/4 v0, 0x1\n    invoke-static {v0}, Lh03;->e(I)I\n    move-result v0\n    const v3, 0x70ffffff\n    invoke-virtual {v1, v0, v3}, Landroid/graphics/drawable/GradientDrawable;->setStroke(II)V\n    const/high16 v0, 0x41400000    # 12.0f\n    invoke-virtual {v1, v0}, Landroid/graphics/drawable/GradientDrawable;->setCornerRadius(F)V\n    invoke-virtual {p0, v1}, Landroid/view/View;->setBackground(Landroid/graphics/drawable/Drawable;)V\n\n    sget-boolean v1, Lu9;->g:Z''',
    'card liquid glass',
)

# Retain Projectivy's wallpaper readability controls, but scale their shadow
# overlay to 35% only while Aerial Rows is active. This keeps the controls useful
# without flattening the Aerial video.
z62 = root / 'smali/z62.smali'
replace_once(
    z62,
    '''    invoke-virtual {v0}, Ljava/lang/Number;->floatValue()F\n\n    .line 113\n    .line 114\n    .line 115\n    move-result v0\n\n    .line 116\n    const/high16 v1, 0x437f0000    # 255.0f''',
    f'''    invoke-virtual {{v0}}, Ljava/lang/Number;->floatValue()F\n    move-result v0\n\n    sget-object v6, Lhj3;->o0:Lyh3;\n    invoke-virtual {{v6}}, Lyh3;->a()Ljava/lang/Object;\n    move-result-object v6\n    const-string v7, "{aerial_id}"\n    invoke-virtual {{v7, v6}}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z\n    move-result v6\n    if-eqz v6, :aerial_overlay_strength_done\n    const v6, 0x3eb33333    # 0.35f\n    mul-float/2addr v0, v6\n    :aerial_overlay_strength_done\n\n    const/high16 v1, 0x437f0000    # 255.0f''',
    'Aerial overlay scaling',
)

# Date beside clock. Projectivy already exposes editable Clock format line 1/2
# settings, so this simply gives line 1 a useful UK-style default while retaining
# full user customization in the existing status-bar settings UI.
hj3 = root / 'smali/hj3.smali'
replace_once(
    hj3,
    '''    const-string v15, "appearance_statusbar_clock_format_l1"\n\n    const-string v16, ""''',
    '''    const-string v15, "appearance_statusbar_clock_format_l1"\n\n    const-string v16, "HH:mm   EEE d MMM"''',
    'clock date default',
)

print('Projectivy Aerial Rows enhancements applied')
