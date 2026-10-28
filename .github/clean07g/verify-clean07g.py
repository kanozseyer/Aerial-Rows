#!/usr/bin/env python3
from pathlib import Path
import hashlib,json,re,sys,zipfile
if len(sys.argv)!=2: raise SystemExit('usage: verify-clean07g.py UNSIGNED_APK')
apk=Path(sys.argv[1]); verify=Path('/tmp/verify-clean07g')

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
bs=(verify/'smali/bs4.smali').read_text(); assert '8c00feaf-c7be-43c2-80f9-d49853aaa99a' in bs

g70=list(verify.rglob('g70.smali')); assert len(g70)==1,g70
g70_body=body(g70[0],r'\.method public final p0\(\)V')
assert 'ChannelVisualBridge;->decorateGroupedPreference(Ljava/lang/Object;Ljava/util/List;)V' in g70_body

ie2=list(verify.rglob('ie2.smali')); he2=list(verify.rglob('he2.smali'))
assert len(ie2)==1 and len(he2)==1,(ie2,he2)
its=ie2[0].read_text(); hts=he2[0].read_text()
for token in [
    'ChannelVisualBridge;->isRedLightGroupedPreference(Ljava/lang/Object;)Z',
    'ChannelVisualBridge;->getSelectedValues(Ljava/lang/Object;)Ljava/util/Set;',
]: assert token in its,token
for token in [
    'ChannelVisualBridge;->isRedLightGroupedPreference(Ljava/lang/Object;)Z',
    'ChannelVisualBridge;->applySelectedValues(Ljava/lang/Object;Ljava/util/Set;)Z',
    'ChannelVisualBridge;->getSelectedValues(Ljava/lang/Object;)Ljava/util/Set;',
    'Lge2;->g:Ljava/lang/Object;',
    'Lie2;->A0:Ljava/util/Set;',
]: assert token in hts,token
# CLEAN07G must read the authoritative visibility set again in he2 after the
# Projectivy navigation callback, not merely seed it when the dialog first opens.
assert hts.count('ChannelVisualBridge;->getSelectedValues(Ljava/lang/Object;)Ljava/util/Set;') >= 1
apply_pos=hts.index('ChannelVisualBridge;->applySelectedValues(Ljava/lang/Object;Ljava/util/Set;)Z')
resync_pos=hts.index('ChannelVisualBridge;->getSelectedValues(Ljava/lang/Object;)Ljava/util/Set;', apply_pos)
assert resync_pos > apply_pos
assert hts.index('Lge2;->g:Ljava/lang/Object;', resync_pos) > resync_pos
assert hts.index('Lie2;->A0:Ljava/util/Set;', resync_pos) > resync_pos

m=(verify/'AndroidManifest.xml').read_text()
for x in ['AerialService','AerialSettingsActivity','ProfileRestoreProvider','RedLightDiscoveryProvider','com.spocky.projengmenu.clean07.redlight','org.xbmc.kodi.file','FirebaseInitProvider','RemoteConfigRegistrar','FirebaseInstallationsRegistrar']: assert x in m,x
for x in ['RedLightLaunchActivity','EmbeddedRuntime$BootReceiver','EmbeddedRuntime$SyncJob','com.spocky.projengmenu.clean06.redlight','CrashlyticsRegistrar','FirebaseSessionsRegistrar','AnalyticsConnectorRegistrar','TransportRegistrar','AppMeasurementService','SessionLifecycleService','JobInfoSchedulerService']: assert x not in m,x

with zipfile.ZipFile(apk) as z:
    names=z.namelist(); libs=[n for n in names if n.startswith('lib/') and n.endswith('.so')]
    assert len(libs)==3 and all(n.startswith('lib/arm64-v8a/') for n in libs),libs
    assert 'classes4.dex' in names and 'assets/aerials.json' in names and 'assets/launcher_default_profile.plbackup' in names
    profile=z.read('assets/launcher_default_profile.plbackup')
    assert len(profile)==13760
    assert hashlib.sha256(profile).hexdigest()=='cda71dbae3d2eca7bd298a198aca4068187bde34f25727df9e9ddd18e8aac2b7'
    json.loads(z.read('assets/aerials.json'))
    c4=z.read('classes4.dex')
    for token in [b'CLEAN07 SUCCESS',b'projectivy-clean07',b'redlight:shelf:',b'ChannelVisualBridge',b'CLEAN07F grouped preference bound',b'CLEAN07F toggle-set']:
        assert token in c4,token
    for token in [b'projectivy-clean06',b'RedLightLaunchActivity',b'CLEAN07C grouped channels decorated',b'CLEAN07D dialog shown',b'CLEAN07E stock dialog upgraded',b'Red Light channels']:
        assert token not in c4,token

src=Path('.github/clean07f/ChannelVisualBridge.java').read_text()
assert 'new AlertDialog.Builder' not in src
assert 'setMultiChoiceItems' not in src
assert '\\uFFFC' in src
assert 'applySelectedValues' in src

print('PASS: CLEAN 07G = CLEAN 07F stock native multi-select + authoritative Visible-state resync after Projectivy channel navigation')
