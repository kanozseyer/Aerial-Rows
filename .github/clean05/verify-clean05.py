#!/usr/bin/env python3
from pathlib import Path
import hashlib
import json
import re
import sys
import zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean05.py UNSIGNED_APK')
apk = Path(sys.argv[1])
verify = Path('/tmp/verify-clean05')

def body(path, sig):
    s = Path(path).read_text()
    m = re.search(sig + r'\n(.*?)\.end method', s, re.S)
    assert m, sig
    return m.group(1)

def begins_return(path, sig):
    b = body(path, sig)
    lines = [x.strip() for x in b.splitlines() if x.strip() and not x.strip().startswith('.line')]
    assert 'return-void' in lines[:4], (path, sig, lines[:8])

begins_return(verify/'smali/it1.smali', r'\.method public final g\(\)V')
begins_return(verify/'smali_classes2/rc1.smali', r'\.method public final h\(\)V')
begins_return(verify/'smali/com/spocky/projengmenu/utils/PTUtils.smali', r'\.method public static e\(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;\)V')
begins_return(verify/'smali/com/spocky/projengmenu/PTApplication.smali', r'\.method public final e\(\)V')

s71 = list(verify.rglob('s71.smali'))
assert len(s71) == 1, s71
b = body(s71[0], r'\.method public static b\(\[B\)\[B')
assert 'ProjectivyAccessibilityService;->s0:B' not in b
assert 'const/16 v0, 0x10' in b

bs = (verify/'smali/bs4.smali').read_text()
assert '8c00feaf-c7be-43c2-80f9-d49853aaa99a' in bs
assert 'filled-new-array {v4, v2, v3, v0, v1}, [Ljava/lang/String;' in bs

m = (verify/'AndroidManifest.xml').read_text()
for x in [
    'AerialService','AerialSettingsActivity','ProfileRestoreProvider',
    'com.spocky.projengmenu.clean05.profile','FirebaseInitProvider',
    'RemoteConfigRegistrar','FirebaseInstallationsRegistrar'
]:
    assert x in m, x
for x in [
    'EmbeddedRuntime$BootReceiver','EmbeddedRuntime$SyncJob','RedLight',
    'CrashlyticsRegistrar','FirebaseSessionsRegistrar','AnalyticsConnectorRegistrar',
    'TransportRegistrar','AppMeasurementService','SessionLifecycleService','JobInfoSchedulerService'
]:
    assert x not in m, x

with zipfile.ZipFile(apk) as z:
    names = z.namelist()
    libs = [n for n in names if n.startswith('lib/') and n.endswith('.so')]
    assert len(libs) == 3 and all(n.startswith('lib/arm64-v8a/') for n in libs), libs
    assert 'classes4.dex' in names
    assert 'assets/aerials.json' in names
    assert 'assets/launcher_default_profile.plbackup' in names
    profile = z.read('assets/launcher_default_profile.plbackup')
    assert len(profile) == 13760, len(profile)
    assert hashlib.sha256(profile).hexdigest() == 'cda71dbae3d2eca7bd298a198aca4068187bde34f25727df9e9ddd18e8aac2b7'
    json.loads(z.read('assets/aerials.json'))

print('PASS: CLEAN 05 = CLEAN 04 + one-time canonical encrypted profile restore provider only')
