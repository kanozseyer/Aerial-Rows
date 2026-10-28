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
print('PASS: CLEAN09 RedLightLaunchActivity registered')
