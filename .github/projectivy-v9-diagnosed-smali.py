from pathlib import Path

root = Path('/tmp/projectivy-decoded')

# 1) Projectivy 4.72 backup decrypt bug: exporter writes a fixed 16-byte salt, but
# importer adds ProjectivyAccessibilityService.s0 (2) and reads 18 bytes. That makes
# the ciphertext length non-block-aligned. Force decrypt to the exporter's 16 bytes.
s71s = list(root.rglob('s71.smali'))
if len(s71s) != 1:
    raise SystemExit(f'expected one s71.smali, found {len(s71s)}')
s71 = s71s[0]
text = s71.read_text()
start = text.index('.method public static b([B)[B')
end = text.index('.end method', start)
method = text[start:end]
old = '''    sget-byte v0, Lcom/spocky/projengmenu/services/ProjectivyAccessibilityService;->s0:B\n\n    .line 2\n    .line 3\n    const/16 v1, 0x10\n\n    .line 4\n    .line 5\n    add-int/2addr v0, v1\n'''
if old not in method:
    raise SystemExit('Projectivy 4.72 18-byte salt sequence not found')
new = '''    # Embedded v9 fix: exporter uses exactly 16 bytes of salt.\n    const/16 v0, 0x10\n\n    .line 2\n    .line 3\n    const/16 v1, 0x10\n'''
method = method.replace(old, new, 1)
text = text[:start] + method + text[end:]
s71.write_text(text)

# 2) Visual-only multi-channel state. Keep Projectivy's stock ListPreference and its
# stock click/navigation callback. Decorate each real visible i5.n row and clear the
# misleading one-radio last-opened value.
g70s = list(root.rglob('g70.smali'))
if len(g70s) != 1:
    raise SystemExit(f'expected one g70.smali, found {len(g70s)}')
g70 = g70s[0]
text = g70.read_text()
start = text.index('.method public final p0()V')
end = text.index('.end method', start)
method = text[start:end]
call = 'Lk00;->e(Lvu;Landroidx/preference/PreferenceGroup;Ljava/lang/String;Ljava/util/List;Z)Landroidx/preference/Preference;'
pos = method.find(call)
if pos < 0:
    raise SystemExit('grouped-channel k00.e call missing')
move = '    move-result-object v2\n'
move_pos = method.find(move, pos)
if move_pos < 0:
    raise SystemExit('grouped-channel result register missing')
insert_at = move_pos + len(move)
insert = '''\n    invoke-static {v2, v4}, Lcom/spocky/projengmenu/embedded/ChannelVisualBridge;->decorateGroupedPreference(Ljava/lang/Object;Ljava/util/List;)V\n'''
method = method[:insert_at] + insert + method[insert_at:]
text = text[:start] + method + text[end:]
g70.write_text(text)

# Strict verification before rebuild.
sm = s71.read_text()[s71.read_text().index('.method public static b([B)[B'):]
if 'sget-byte v0, Lcom/spocky/projengmenu/services/ProjectivyAccessibilityService;->s0:B' in sm.split('.end method',1)[0]:
    raise SystemExit('decryptor still references dynamic salt length')
if 'const/16 v0, 0x10' not in sm.split('.end method',1)[0]:
    raise SystemExit('fixed 16-byte salt not present')
if 'ChannelVisualBridge;->decorateGroupedPreference' not in g70.read_text():
    raise SystemExit('grouped-channel visual hook missing')
print('patched stock backup decryptor to 16-byte salt and grouped-channel visual state')
