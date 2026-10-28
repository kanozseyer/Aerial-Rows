#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv)!=2: raise SystemExit('usage: patch-manifest.py DECODED_ROOT')
root=Path(sys.argv[1]); manifest=root/'AndroidManifest.xml'; s=manifest.read_text()

# Preserve CLEAN 05 one-time profile provider.
profile='''        <provider android:authorities="com.spocky.projengmenu.clean05.profile" android:exported="false" android:initOrder="100" android:name="com.spocky.projengmenu.embedded.ProfileRestoreProvider"/>\n'''
assert 'com.spocky.projengmenu.embedded.ProfileRestoreProvider' not in s
assert s.count('    </application>')==1

# Foreground-only Red Light discovery provider. No boot receiver, job or service.
red='''        <provider android:authorities="com.spocky.projengmenu.clean06.redlight" android:exported="false" android:initOrder="90" android:name="com.spocky.projengmenu.embedded.RedLightDiscoveryProvider"/>\n'''
assert 'com.spocky.projengmenu.embedded.RedLightDiscoveryProvider' not in s
s=s.replace('    </application>', profile+red+'    </application>',1)

# Package/provider visibility needed to talk to Kodi's existing file provider.
if 'android:name="org.xbmc.kodi"' not in s:
    anchor='        <package android:name="com.android.vending"/>'
    assert anchor in s
    s=s.replace(anchor,'        <package android:name="org.xbmc.kodi"/>\n        <provider android:authorities="org.xbmc.kodi.file"/>\n'+anchor,1)

manifest.write_text(s)

p=root/'res/values/attrs.xml'; a=p.read_text(); marker='</resources>'
extra='''    <attr name="state_liftable" format="boolean" />\n    <attr name="state_lifted" format="boolean" />\n    <attr name="state_swiped" format="boolean" />\n'''
if 'name="state_liftable"' not in a: p.write_text(a.replace(marker,extra+marker))
print('PASS: CLEAN 06 profile+foreground Red Light discovery providers registered; Kodi visibility added; no boot/job component')
