#!/usr/bin/env python3
from pathlib import Path
import hashlib,json,re,sys,zipfile
if len(sys.argv)!=2: raise SystemExit('usage: verify-clean07j.py UNSIGNED_APK')
apk=Path(sys.argv[1]); verify=Path('/tmp/verify-clean07j')
def body(path,sig):
    s=Path(path).read_text(); m=re.search(sig+r'\n(.*?)\.end method',s,re.S); assert m,sig; return m.group(1)
def begins_return(path,sig):
    lines=[x.strip() for x in body(path,sig).splitlines() if x.strip() and not x.strip().startswith('.line')]
    assert 'return-void' in lines[:4],(path,sig,lines[:8])

begins_return(verify/'smali/it1.smali',r'\.method public final g\(\)V')
begins_return(verify/'smali_classes2/rc1.smali',r'\.method public final h\(\)V')
begins_return(verify/'smali/com/spocky/projengmenu/utils/PTUtils.smali',r'\.method public static e\(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;\)V')
begins_return(verify/'smali/com/spocky/projengmenu/PTApplication.smali',r'\.method public final e\(\)V')

s71=list(verify.rglob('s71.smali')); assert len(s71)==1
b=body(s71[0],r'\.method public static b\(\[B\)\[B'); assert 'ProjectivyAccessibilityService;->s0:B' not in b; assert 'const/16 v0, 0x10' in b

g70=list(verify.rglob('g70.smali')); assert len(g70)==1
assert 'ChannelVisualBridge;->decorateGroupedPreference(Ljava/lang/Object;Ljava/util/List;)V' in body(g70[0],r'\.method public final p0\(\)V')

ie2=list(verify.rglob('ie2.smali')); he2=list(verify.rglob('he2.smali')); ft=list(verify.rglob('ft.smali'))
assert len(ie2)==len(he2)==len(ft)==1
its=ie2[0].read_text(); hts=he2[0].read_text(); fts=ft[0].read_text()
assert 'registerSelector(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V' in its
bmethod=body(ie2[0],r'\.method public final B\(Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Landroid/os/Bundle;\)Landroid/view/View;')
assert 'RecyclerView;->setAdapter(Lfr3;)V' in bmethod
assert 'registerSelector(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V' in bmethod
assert bmethod.index('RecyclerView;->setAdapter(Lfr3;)V') < bmethod.index('registerSelector(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V')
hb=body(ie2[0],r'\.method public H\(\)V')
assert 'invoke-super {p0}, Lje2;->H()V' in hb
assert 'onSelectorResumed(Ljava/lang/Object;Ljava/lang/Object;)V' in hb
assert 'openChannel(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)Z' in hts
assert 'applySelectedValues(Ljava/lang/Object;Ljava/util/Set;)Z' not in hts
assert 'onCategoryVisibilityChanged(Ljava/lang/Object;)V' in fts

m=(verify/'AndroidManifest.xml').read_text()
for x in ['AerialService','AerialSettingsActivity','ProfileRestoreProvider','RedLightDiscoveryProvider','com.spocky.projengmenu.clean07.redlight','org.xbmc.kodi.file','FirebaseInitProvider','RemoteConfigRegistrar','FirebaseInstallationsRegistrar']: assert x in m,x
for x in ['RedLightLaunchActivity','EmbeddedRuntime$BootReceiver','EmbeddedRuntime$SyncJob','com.spocky.projengmenu.clean06.redlight','CrashlyticsRegistrar','FirebaseSessionsRegistrar','AnalyticsConnectorRegistrar','TransportRegistrar','AppMeasurementService','SessionLifecycleService','JobInfoSchedulerService']: assert x not in m,x

with zipfile.ZipFile(apk) as z:
    names=z.namelist(); libs=[n for n in names if n.startswith('lib/') and n.endswith('.so')]
    assert len(libs)==3 and all(n.startswith('lib/arm64-v8a/') for n in libs),libs
    assert 'classes4.dex' in names and 'assets/aerials.json' in names and 'assets/launcher_default_profile.plbackup' in names
    profile=z.read('assets/launcher_default_profile.plbackup'); assert len(profile)==13760
    assert hashlib.sha256(profile).hexdigest()=='cda71dbae3d2eca7bd298a198aca4068187bde34f25727df9e9ddd18e8aac2b7'
    json.loads(z.read('assets/aerials.json'))
    c4=z.read('classes4.dex')
    for token in [b'CLEAN07 SUCCESS',b'projectivy-clean07',b'redlight:shelf:',b'CLEAN07H open-channel',b'CLEAN07I resume-sync',b'CLEAN07J live-adapter registered']:
        assert token in c4,token
    for token in [b'CLEAN07D dialog shown',b'CLEAN07E stock dialog upgraded',b'Red Light channels']:
        assert token not in c4,token

src=Path('.github/clean07j/ChannelVisualBridge.java').read_text()
assert 'registerSelector' in src and 'onSelectorResumed' in src
assert 'applySelectedValues' not in src
assert '\\uFFFC' in src
print('PASS: CLEAN 07J = CLEAN 07I plus live attached adapter registration on every selector view creation')
