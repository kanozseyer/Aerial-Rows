#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: patch-backup-decrypt.py DECODED_ROOT')
root = Path(sys.argv[1])

hits = list(root.rglob('s71.smali'))
if len(hits) != 1:
    raise SystemExit(f'expected one s71.smali, found {len(hits)}')
p = hits[0]
s = p.read_text()
start = s.index('.method public static b([B)[B')
end = s.index('.end method', start)
method = s[start:end]
old = '''    sget-byte v0, Lcom/spocky/projengmenu/services/ProjectivyAccessibilityService;->s0:B\n\n    .line 2\n    .line 3\n    const/16 v1, 0x10\n\n    .line 4\n    .line 5\n    add-int/2addr v0, v1\n'''
new = '''    # CLEAN 04: exporter writes an exact 16-byte salt; importer must read 16.\n    const/16 v0, 0x10\n\n    .line 2\n    .line 3\n    const/16 v1, 0x10\n'''
if method.count(old) != 1:
    raise SystemExit('Projectivy 4.72 dynamic 18-byte salt sequence not found exactly once')
method = method.replace(old, new, 1)
s = s[:start] + method + s[end:]
p.write_text(s)

check = p.read_text()[start:p.read_text().index('.end method', start)]
if 'sget-byte v0, Lcom/spocky/projengmenu/services/ProjectivyAccessibilityService;->s0:B' in check:
    raise SystemExit('decryptor still references ProjectivyAccessibilityService.s0')
if check.count('const/16 v0, 0x10') != 1:
    raise SystemExit('fixed 16-byte salt length not present exactly once')
print('PASS: CLEAN 04 backup decryptor fixed to exporter-compatible 16-byte salt')
