#!/usr/bin/env python3
from pathlib import Path
import os
import runpy
import shutil
import sys
import zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean07l.py UNSIGNED_APK')
apk = Path(sys.argv[1])
verify_l = Path('/tmp/verify-clean07l')
verify_k = Path('/tmp/verify-clean07k')

# Reuse every frozen CLEAN 07K invariant against the CLEAN 07L decode tree.
if verify_k.is_symlink() or verify_k.exists():
    if verify_k.is_dir() and not verify_k.is_symlink():
        shutil.rmtree(verify_k)
    else:
        verify_k.unlink()
os.symlink(verify_l, verify_k, target_is_directory=True)
runpy.run_path('.github/clean07k/verify-clean07k.py', run_name='__main__')

# Presentation-only source-heading override must exist in g70 and remain scoped
# to internal_provider_id values beginning with redlight:.
g70s = list(verify_l.rglob('g70.smali'))
assert len(g70s) == 1, g70s
g70 = g70s[0].read_text()
start = g70.find('.method public final p0()V')
end = g70.find('.end method', start)
assert start >= 0 and end >= 0
body = g70[start:end]
for token in [
    'const-string v6, "internal_provider_id"',
    'const-string v6, "redlight:"',
    'const-string v4, "Red Light"',
    ':clean07l_channel_label_ready',
]:
    assert token in body, token

# The embedded runtime must contain the exact installed-addon icon path and use
# Android TV's native channel-logo URI. This covers Edit Channels + Home titles.
with zipfile.ZipFile(apk) as z:
    c4 = z.read('classes4.dex')
    for token in [
        b'special://home/addons/plugin.video.redlight/resources/media/addon_icons/icon.png',
        b'redlight_channel_icon.png',
        b'CLEAN07L channel logo applied',
    ]:
        assert token in c4, token
    assert b'CLEAN07I resume-sync' not in c4
    assert b'CLEAN07J live-adapter registered' not in c4

print('PASS: CLEAN 07L = frozen 07K behavior + Red Light source heading + native Red Light channel logo; no 07I/07J')
