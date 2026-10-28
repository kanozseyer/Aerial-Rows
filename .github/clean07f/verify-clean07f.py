#!/usr/bin/env python3
from pathlib import Path
import hashlib, json, re, sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit('usage: verify-clean07f.py UNSIGNED_APK')
apk = Path(sys.argv[1])
verify = Path('/tmp/verify-clean07f')

def body(path, sig):
    s = Path(path).read_text()
    m = re.search(sig + r'\n(.*?)\.end method', s, re.S)
    assert m, sig
    return m.group(1)

def begins_return(path, sig):
    lines = [x.strip() for x in body(path, sig).splitlines()
             if x.strip() and not x.strip().startswith('.line')]
    assert 'return-void' in lines[:4], (path, sig, lines[:8])

# Frozen lean/CLEAN04 baseline invariants.
begins_return(verify/'smali/it1.smali', r'\.method public final g\(\)V')
begins_return(verify/'smali_classes2/rc1.smali', r'\.method public final h\(\)V')
begins_return(verify/'smali/com/spocky/projengmenu/utils/PTUtils.smali', r'\.method public static e\(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;\)V')
begins_return(verify/'smali/com/spocky/projengmenu/PTApplication.smali', r'\.method public final e\(\)V')
s71 = list(verify.rglob('s71.smali')); assert len(s71) == 1
b = body(s71[0], r'\.method public static b\(\[B\)\[B')
assert 'ProjectivyAccessibilityService;->s0:B' not in b
assert 'const/16 v0, 0x10' in b
bs = (verify/'smali/bs4.smali').read_text()
assert '8c00feaf-c7be-43c2-80f9-d49853aaa99a' in bs

# Existing grouped-channel registration hook remains the only g70 addition.
g70 = list(verify.rglob('g70.smali')); assert len(g70) == 1, g70
g70_body = body(g70[0], r'\.method public final p0\(\)V')
assert g70_body.count('ChannelVisualBridge;->decorateGroupedPreference(Ljava/lang/Object;Ljava/util/List;)V') == 1

# CLEAN07F must patch Projectivy's real native Leanback list dialog path.
ie2 = list(verify.rglob('ie2.smali')); assert len(ie2) == 1, ie2
he2 = list(verify.rglob('he2.smali')); assert len(he2) == 1, he2
ie2_body = body(ie2[0], r'\.method public final A\(Landroid/os/Bundle;\)V')
he2_body = body(he2[0], r'\.method public final onClick\(Landroid/view/View;\)V')
assert ie2_body.count('ChannelVisualBridge;->configureNativeVisibilityDialog(Ljava/lang/Object;Ljava/lang/Object;)Z') == 1
assert he2_body.count('ChannelVisualBridge;->applyNativeVisibilitySelection(Ljava/lang/Object;Ljava/util/Set;)Z') == 1
# Stock single navigation and genuine native MultiSelect behaviour remain available
# for all non-Red-Light preferences.
assert 'Landroidx/preference/ListPreference;->K(Ljava/lang/String;)V' in he2_body
assert 'Landroidx/preference/MultiSelectListPreference;->I(Ljava/util/Set;)V' in he2_body

m = (verify/'AndroidManifest.xml').read_text()
for x in ['AerialService','AerialSettingsActivity','ProfileRestoreProvider',
          'RedLightDiscoveryProvider','com.spocky.projengmenu.clean07.redlight',
          'org.xbmc.kodi.file','FirebaseInitProvider','RemoteConfigRegistrar',
          'FirebaseInstallationsRegistrar']:
    assert x in m, x
for x in ['RedLightLaunchActivity','EmbeddedRuntime$BootReceiver','EmbeddedRuntime$SyncJob',
          'com.spocky.projengmenu.clean06.redlight','CrashlyticsRegistrar',
          'FirebaseSessionsRegistrar','AnalyticsConnectorRegistrar','TransportRegistrar',
          'AppMeasurementService','SessionLifecycleService','JobInfoSchedulerService']:
    assert x not in m, x

with zipfile.ZipFile(apk) as z:
    names = z.namelist()
    libs = [n for n in names if n.startswith('lib/') and n.endswith('.so')]
    assert len(libs) == 3 and all(n.startswith('lib/arm64-v8a/') for n in libs), libs
    assert 'classes4.dex' in names
    assert 'assets/aerials.json' in names
    assert 'assets/launcher_default_profile.plbackup' in names
    profile = z.read('assets/launcher_default_profile.plbackup')
    assert len(profile) == 13760
    assert hashlib.sha256(profile).hexdigest() == 'cda71dbae3d2eca7bd298a198aca4068187bde34f25727df9e9ddd18e8aac2b7'
    json.loads(z.read('assets/aerials.json'))
    c4 = z.read('classes4.dex')
    for token in [b'CLEAN07 SUCCESS', b'projectivy-clean07', b'trakt_watchlist',
                  b'build_continue_watching', b'build_in_progress_episode',
                  b'redlight:shelf:', b'ChannelVisualBridge',
                  b'CLEAN07F grouped Red Light registered',
                  b'CLEAN07F native visibility dialog',
                  b'CLEAN07F visibility apply']:
        assert token in c4, token
    for token in [b'projectivy-clean06', b'RedLightLaunchActivity',
                  b'CLEAN07C grouped channels decorated', b'CLEAN07D dialog shown',
                  b'Red Light channels', b'CLEAN07E stock dialog upgraded',
                  b'CLEAN07E toggle']:
        assert token not in c4, token

src = Path('.github/clean07f/ChannelVisualBridge.java').read_text()
assert 'AlertDialog' not in src
assert 'ListView' not in src
assert 'setMultiChoiceItems' not in src
assert 'configureNativeVisibilityDialog' in src
assert 'applyNativeVisibilitySelection' in src
assert 'setBooleanField(category, "n", desired)' in src
assert '\\uFFFC' in src

print('PASS: CLEAN 07F = CLEAN 07 dynamic channels + one native Projectivy Red Light panel whose circles directly control real category visibility')
