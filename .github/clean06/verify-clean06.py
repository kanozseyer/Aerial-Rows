#!/usr/bin/env python3
from pathlib import Path
import hashlib,json,re,sys,zipfile
if len(sys.argv)!=2: raise SystemExit('usage: verify-clean06.py UNSIGNED_APK')
apk=Path(sys.argv[1]); verify=Path('/tmp/verify-clean06')
def body(path,sig):
    s=Path(path).read_text(); m=re.search(sig+r'\n(.*?)\.end method',s,re.S); assert m,sig; return m.group(1)
def begins_return(path,sig):
    lines=[x.strip() for x in body(path,sig).splitlines() if x.strip() and not x.strip().startswith('.line')]; assert 'return-void' in lines[:4],(path,sig,lines[:8])
begins_return(verify/'smali/it1.smali',r'\.method public final g\(\)V')
begins_return(verify/'smali_classes2/rc1.smali',r'\.method public final h\(\)V')
begins_return(verify/'smali/com/spocky/projengmenu/utils/PTUtils.smali',r'\.method public static e\(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;\)V')
begins_return(verify/'smali/com/spocky/projengmenu/PTApplication.smali',r'\.method public final e\(\)V')
s71=list(verify.rglob('s71.smali')); assert len(s71)==1
b=body(s71[0],r'\.method public static b\(\[B\)\[B'); assert 'ProjectivyAccessibilityService;->s0:B' not in b; assert 'const/16 v0, 0x10' in b
bs=(verify/'smali/bs4.smali').read_text(); assert '8c00feaf-c7be-43c2-80f9-d49853aaa99a' in bs
m=(verify/'AndroidManifest.xml').read_text()
for x in ['AerialService','AerialSettingsActivity','ProfileRestoreProvider','RedLightDiscoveryProvider','com.spocky.projengmenu.clean06.redlight','org.xbmc.kodi.file','FirebaseInitProvider','RemoteConfigRegistrar','FirebaseInstallationsRegistrar']: assert x in m,x
for x in ['RedLightLaunchActivity','EmbeddedRuntime$BootReceiver','EmbeddedRuntime$SyncJob','CrashlyticsRegistrar','FirebaseSessionsRegistrar','AnalyticsConnectorRegistrar','TransportRegistrar','AppMeasurementService','SessionLifecycleService','JobInfoSchedulerService']: assert x not in m,x
with zipfile.ZipFile(apk) as z:
    names=z.namelist(); libs=[n for n in names if n.startswith('lib/') and n.endswith('.so')]
    assert len(libs)==3 and all(n.startswith('lib/arm64-v8a/') for n in libs),libs
    assert 'classes4.dex' in names and 'assets/aerials.json' in names and 'assets/launcher_default_profile.plbackup' in names
    profile=z.read('assets/launcher_default_profile.plbackup'); assert len(profile)==13760; assert hashlib.sha256(profile).hexdigest()=='cda71dbae3d2eca7bd298a198aca4068187bde34f25727df9e9ddd18e8aac2b7'
    json.loads(z.read('assets/aerials.json'))
print('PASS: CLEAN 06 = CLEAN 05 + foreground one-row Red Light Trending Movies discovery only')
