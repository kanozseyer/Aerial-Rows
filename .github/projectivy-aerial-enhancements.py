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


# Fix Projectivy's Configure action only for Aerial Rows. The provider APK uses
# applicationId com.kanozseyer.aerialrows.wallpaper while SettingsActivity lives
# in com.kanozseyer.aerialrows.projectivy.
ze = root / 'smali_classes2/ze.smali'
old_cfg = '''    move-object v1, p1\n\n    .line 172\n    check-cast v1, Ly05;\n\n    .line 173\n    .line 174\n    iget-object v1, v1, Ly05;->j:Ljava/lang/String;\n\n    .line 175\n    .line 176\n    move-object v2, p1\n\n    .line 177\n    check-cast v2, Ly05;\n\n    .line 178\n    .line 179\n    iget-object v2, v2, Ly05;->j:Ljava/lang/String;\n\n    .line 180\n    .line 181\n    check-cast p1, Ly05;\n\n    .line 182\n    .line 183\n    iget-object p1, p1, Ly05;->h:Ljava/lang/String;\n\n    .line 184\n    .line 185\n    invoke-virtual {v2, p1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;\n\n    .line 186\n    .line 187\n    .line 188\n    move-result-object p1\n\n    .line 189\n    invoke-virtual {v0, v1, p1}, Landroid/content/Intent;->setClassName(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;'''
new_cfg = f'''    move-object v1, p1\n    check-cast v1, Ly05;\n    iget-object v2, v1, Ly05;->e:Ljava/lang/String;\n    const-string v3, "{aerial_id}"\n    invoke-virtual {{v3, v2}}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z\n    move-result v2\n    if-eqz v2, :aerial_config_normal\n\n    const-string v1, "com.kanozseyer.aerialrows.wallpaper"\n    const-string p1, "com.kanozseyer.aerialrows.projectivy.SettingsActivity"\n    goto :aerial_config_set_component\n\n    :aerial_config_normal\n    move-object v1, p1\n    check-cast v1, Ly05;\n    iget-object v1, v1, Ly05;->j:Ljava/lang/String;\n    move-object v2, p1\n    check-cast v2, Ly05;\n    iget-object v2, v2, Ly05;->j:Ljava/lang/String;\n    check-cast p1, Ly05;\n    iget-object p1, p1, Ly05;->h:Ljava/lang/String;\n    invoke-virtual {{v2, p1}}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;\n    move-result-object p1\n\n    :aerial_config_set_component\n    invoke-virtual {{v0, v1, p1}}, Landroid/content/Intent;->setClassName(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;'''
replace_once(ze, old_cfg, new_cfg, 'Aerial Configure component')

# Keep Projectivy's native video dynamic-colour sampling path. On NVIDIA Shield
# Projectivy already samples SurfaceView video via PixelCopy every 5 seconds and
# feeds the extracted palette into its dynamic colour engine. Do not paint over
# cards/docks here; that lets UI elements configured for Dynamic colour use the
# live video palette naturally.

# Put wallpaper Tint and Tint amount first in Wallpaper adjustments so the video
# readability controls are easy to find. All original controls remain available.
te = root / 'smali_classes2/te.smali'
text = te.read_text()
start = text.index('.method public final e0(Ljava/util/List;Landroid/os/Bundle;)V')
end = text.index('.method public final i0(Lmu1;)V', start)
method = text[start:end]

# Extract the existing Tint (I0) and Tint amount (J0) blocks from their original
# positions and insert them immediately after the method prologue.
def extract_setting_block(src: str, field: str, next_field: str):
    a = src.index(f'    sget-object p2, Lhj3;->{field}:Lyh3;')
    b = src.index(f'    sget-object p2, Lhj3;->{next_field}:Lyh3;', a)
    return src[a:b], src[:a] + src[b:]

tint_block, method = extract_setting_block(method, 'I0', 'J0')
tint_amount_block, method = extract_setting_block(method, 'J0', 'H0')
prologue = '.method public final e0(Ljava/util/List;Landroid/os/Bundle;)V\n    .locals 0\n\n'
if not method.startswith(prologue):
    raise SystemExit('wallpaper adjustment method prologue mismatch')
method = prologue + tint_block + tint_amount_block + method[len(prologue):]
te.write_text(text[:start] + method + text[end:])
print('patched wallpaper Tint/Tint amount priority')

# Retain Projectivy's wallpaper readability controls, but scale the shadow
# overlay to 35% only while Aerial Rows is active. Tint itself remains entirely
# user-controlled through Projectivy's Wallpaper adjustments page.
z62 = root / 'smali/z62.smali'
replace_once(
    z62,
    '''    invoke-virtual {v0}, Ljava/lang/Number;->floatValue()F\n\n    .line 113\n    .line 114\n    .line 115\n    move-result v0\n\n    .line 116\n    const/high16 v1, 0x437f0000    # 255.0f''',
    f'''    invoke-virtual {{v0}}, Ljava/lang/Number;->floatValue()F\n    move-result v0\n\n    sget-object v6, Lhj3;->o0:Lyh3;\n    invoke-virtual {{v6}}, Lyh3;->a()Ljava/lang/Object;\n    move-result-object v6\n    const-string v7, "{aerial_id}"\n    invoke-virtual {{v7, v6}}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z\n    move-result v6\n    if-eqz v6, :aerial_overlay_strength_done\n    const v6, 0x3eb33333    # 0.35f\n    mul-float/2addr v0, v6\n    :aerial_overlay_strength_done\n\n    const/high16 v1, 0x437f0000    # 255.0f''',
    'Aerial overlay scaling',
)

# Date beside clock. Projectivy already exposes editable Clock format line 1/2
# settings, so this gives line 1 a useful UK-style default while retaining full
# user customization.
hj3 = root / 'smali/hj3.smali'
replace_once(
    hj3,
    '''    const-string v15, "appearance_statusbar_clock_format_l1"\n\n    const-string v16, ""''',
    '''    const-string v15, "appearance_statusbar_clock_format_l1"\n\n    const-string v16, "HH:mm   EEE d MMM"''',
    'clock date default',
)

print('Projectivy Aerial Rows enhancements applied (no liquid glass)')
