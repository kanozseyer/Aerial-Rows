#!/usr/bin/env python3
import hashlib
import os
import sys
import zipfile

if len(sys.argv) != 3:
    raise SystemExit('usage: scope-clean05.py OFFICIAL FINAL')
a=zipfile.ZipFile(sys.argv[1]); b=zipfile.ZipFile(sys.argv[2])
ign=lambda n:n.startswith('META-INF/')
aa={i.filename for i in a.infolist() if not ign(i.filename)}
bb={i.filename for i in b.infolist() if not ign(i.filename)}
missing=sorted(aa-bb); extra=sorted(bb-aa)
assert len(missing)==9 and all(n.startswith(('lib/armeabi-v7a/','lib/x86/','lib/x86_64/')) for n in missing),missing
assert extra==['assets/aerials.json','assets/launcher_default_profile.plbackup','classes4.dex'],extra
changed=[n for n in sorted(aa & bb) if hashlib.sha256(a.read(n)).digest()!=hashlib.sha256(b.read(n)).digest()]
assert changed==['AndroidManifest.xml','classes.dex','classes2.dex'],changed
assert a.read('classes3.dex')==b.read('classes3.dex')
assert a.read('resources.arsc')==b.read('resources.arsc')
print('missing:',missing)
print('extra:',extra)
print('changed:',changed)
print('official APK bytes:',os.path.getsize(sys.argv[1]))
print('CLEAN05 APK bytes:',os.path.getsize(sys.argv[2]))
print('classes4.dex bytes:',len(b.read('classes4.dex')))
print('profile bytes:',len(b.read('assets/launcher_default_profile.plbackup')))
