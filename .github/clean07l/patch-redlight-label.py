#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: patch-redlight-label.py DECODED_ROOT')
root = Path(sys.argv[1])
paths = list(root.rglob('g70.smali'))
if len(paths) != 1:
    raise SystemExit(f'expected one g70.smali, found {len(paths)}: {paths}')
path = paths[0]
text = path.read_text()
start = text.index('.method public final p0()V')
end = text.index('.end method', start)
method = text[start:end]

old = '''    move-object v4, v3\n\n    .line 48\n    check-cast v4, Li5;\n\n    .line 49\n    .line 50\n    sget-object v5, Lyb3;->a:Lyb3;\n\n    .line 51\n    .line 52\n    iget-object v4, v4, Li5;->g:Lm60;\n\n    .line 53\n    .line 54\n    if-eqz v4, :cond_0\n\n    .line 55\n    .line 56\n    invoke-virtual {v4}, Lm60;->b()Ljava/lang/String;\n\n    .line 57\n    .line 58\n    .line 59\n    move-result-object v4\n\n    .line 60\n    if-nez v4, :cond_1\n\n    .line 61\n    .line 62\n    :cond_0\n    const-string v4, ""\n\n    .line 63\n    .line 64\n    :cond_1\n    invoke-static {v4}, Lyb3;->h(Ljava/lang/String;)Ljava/lang/String;\n\n    .line 65\n    .line 66\n    .line 67\n    move-result-object v4\n'''

new = '''    move-object v4, v3\n\n    .line 48\n    check-cast v4, Li5;\n\n    .line 49\n    .line 50\n    iget-object v4, v4, Li5;->g:Lm60;\n\n    .line 53\n    .line 54\n    if-eqz v4, :cond_0\n\n    # CLEAN07L: Projectivy owns the Android-TV channel package, but dynamic\n    # Red Light channels should be grouped under their own source heading.\n    iget-object v5, v4, Lm60;->a:Landroid/content/ContentValues;\n    const-string v6, "internal_provider_id"\n    invoke-virtual {v5, v6}, Landroid/content/ContentValues;->getAsString(Ljava/lang/String;)Ljava/lang/String;\n    move-result-object v5\n    if-eqz v5, :clean07l_normal_channel_label\n    const-string v6, "redlight:"\n    invoke-virtual {v5, v6}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z\n    move-result v5\n    if-eqz v5, :clean07l_normal_channel_label\n    const-string v4, "Red Light"\n    goto :clean07l_channel_label_ready\n\n    :clean07l_normal_channel_label\n    invoke-virtual {v4}, Lm60;->b()Ljava/lang/String;\n\n    .line 57\n    .line 58\n    .line 59\n    move-result-object v4\n\n    .line 60\n    if-nez v4, :cond_1\n\n    .line 61\n    .line 62\n    :cond_0\n    const-string v4, ""\n\n    .line 63\n    .line 64\n    :cond_1\n    invoke-static {v4}, Lyb3;->h(Ljava/lang/String;)Ljava/lang/String;\n\n    .line 65\n    .line 66\n    .line 67\n    move-result-object v4\n\n    :clean07l_channel_label_ready\n'''

if 'clean07l_channel_label_ready' not in method:
    if method.count(old) != 1:
        raise SystemExit(f'g70 Red Light label anchor mismatch: {method.count(old)}')
    method = method.replace(old, new, 1)
    text = text[:start] + method + text[end:]
    path.write_text(text)

check = path.read_text()
for needle in ('internal_provider_id', 'redlight:', 'const-string v4, "Red Light"', 'clean07l_channel_label_ready'):
    if needle not in check:
        raise SystemExit(f'CLEAN07L label verification failed: {needle}')
print('PASS: CLEAN 07L Edit Channels source heading = Red Light')
