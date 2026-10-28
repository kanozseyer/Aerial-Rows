#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: patch-manifest.py DECODED_ROOT')
root = Path(sys.argv[1])
manifest = root / 'AndroidManifest.xml'
s = manifest.read_text()
provider = '''        <provider android:authorities="com.spocky.projengmenu.clean05.profile" android:exported="false" android:initOrder="100" android:name="com.spocky.projengmenu.embedded.ProfileRestoreProvider"/>\n'''
assert 'com.spocky.projengmenu.embedded.ProfileRestoreProvider' not in s
assert s.count('    </application>') == 1
manifest.write_text(s.replace('    </application>', provider + '    </application>', 1))

p = root / 'res/values/attrs.xml'
a = p.read_text()
marker = '</resources>'
extra = '''    <attr name="state_liftable" format="boolean" />\n    <attr name="state_lifted" format="boolean" />\n    <attr name="state_swiped" format="boolean" />\n'''
if 'name="state_liftable"' not in a:
    p.write_text(a.replace(marker, extra + marker))
print('PASS: CLEAN 05 restore provider registered; no boot/job component added')
