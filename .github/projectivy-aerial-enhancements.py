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


# Keep Projectivy's native video dynamic-colour sampling path. On NVIDIA Shield
# Projectivy already samples SurfaceView video via PixelCopy and feeds the
# extracted palette into its dynamic-colour engine. No card/dock glass is added.

# Put wallpaper Tint and Tint amount first in Wallpaper adjustments.
te = root / 'smali_classes2/te.smali'
text = te.read_text()
start = text.index('.method public final e0(Ljava/util/List;Landroid/os/Bundle;)V')
end = text.index('.method public final i0(Lmu1;)V', start)
method = text[start:end]


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

# Keep readability overlay but reduce it for Aerial so video is not washed out.
z62 = root / 'smali/z62.smali'
replace_once(
    z62,
    '''    invoke-virtual {v0}, Ljava/lang/Number;->floatValue()F\n\n    .line 113\n    .line 114\n    .line 115\n    move-result v0\n\n    .line 116\n    const/high16 v1, 0x437f0000    # 255.0f''',
    f'''    invoke-virtual {{v0}}, Ljava/lang/Number;->floatValue()F\n    move-result v0\n\n    sget-object v6, Lhj3;->o0:Lyh3;\n    invoke-virtual {{v6}}, Lyh3;->a()Ljava/lang/Object;\n    move-result-object v6\n    const-string v7, "{aerial_id}"\n    invoke-virtual {{v7, v6}}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z\n    move-result v6\n    if-eqz v6, :aerial_overlay_strength_done\n    const v6, 0x3eb33333    # 0.35f\n    mul-float/2addr v0, v6\n    :aerial_overlay_strength_done\n\n    const/high16 v1, 0x437f0000    # 255.0f''',
    'Aerial overlay scaling',
)

# Date beside clock; still editable through Projectivy's existing clock format.
hj3 = root / 'smali/hj3.smali'
replace_once(
    hj3,
    '''    const-string v15, "appearance_statusbar_clock_format_l1"\n\n    const-string v16, ""''',
    '''    const-string v15, "appearance_statusbar_clock_format_l1"\n\n    const-string v16, "HH:mm   EEE d MMM"''',
    'clock date default',
)

print('Projectivy embedded Aerial enhancements applied (no liquid glass)')
