#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: patch-manifest.py DECODED_ROOT')
root = Path(sys.argv[1])
manifest = root / 'AndroidManifest.xml'
s = manifest.read_text()

name = 'com.spocky.projengmenu.embedded.RedLightLaunchActivity'
if name in s:
    raise SystemExit('CLEAN09 launch activity already present')
if s.count('    </application>') != 1:
    raise SystemExit('CLEAN09 application close anchor mismatch')

activity = '''        <activity android:excludeFromRecents="true" android:exported="true" android:launchMode="singleTop" android:name="com.spocky.projengmenu.embedded.RedLightLaunchActivity" android:noHistory="true" android:theme="@android:style/Theme.Translucent.NoTitleBar"/>\n'''
s = s.replace('    </application>', activity + '    </application>', 1)
manifest.write_text(s)

# Same apktool compatibility stubs used by the proven CLEAN07 decode/rebuild
# pipeline. They only satisfy Material resource linking in the temporary tree.
attrs = root / 'res/values/attrs.xml'
a = attrs.read_text()
marker = '</resources>'
extra = '''    <attr name="state_liftable" format="boolean" />\n    <attr name="state_lifted" format="boolean" />\n    <attr name="state_swiped" format="boolean" />\n'''
if 'name="state_liftable"' not in a:
    if marker not in a:
        raise SystemExit('CLEAN09 attrs resource close anchor missing')
    attrs.write_text(a.replace(marker, extra + marker, 1))

print('PASS: CLEAN09 RedLightLaunchActivity registered + apktool attrs restored')
