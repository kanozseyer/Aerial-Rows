#!/usr/bin/env python3
from pathlib import Path
import re
import sys

AERIAL_ID = '8c00feaf-c7be-43c2-80f9-d49853aaa99a'

if len(sys.argv) != 2:
    raise SystemExit('usage: patch-decoded.py DECODED_ROOT')
root = Path(sys.argv[1])

# CLEAN 02 provider trust registration. bs4.<clinit> already has .locals 5;
# v4 is no longer live by this point, so the fifth string does not require a
# register-count change.
bs4 = root / 'smali/bs4.smali'
s = bs4.read_text()
old = '''    const-string v0, "e77610c3-d495-4090-b387-fae3faafa718"\n\n    .line 120\n    .line 121\n    const-string v1, "eea7a685-d68f-4fb5-b46e-b4897af5413f"\n\n    .line 122\n    .line 123\n    const-string v2, "773b5948-b4d1-49c5-b994-999210bc8877"\n\n    .line 124\n    .line 125\n    const-string v3, "326618d8-3242-4322-a1b1-9e60e5d3dfc3"\n\n    .line 126\n    .line 127\n    filled-new-array {v2, v3, v0, v1}, [Ljava/lang/String;'''
new = f'''    const-string v0, "e77610c3-d495-4090-b387-fae3faafa718"\n\n    .line 120\n    .line 121\n    const-string v1, "eea7a685-d68f-4fb5-b46e-b4897af5413f"\n\n    .line 122\n    .line 123\n    const-string v2, "773b5948-b4d1-49c5-b994-999210bc8877"\n\n    .line 124\n    .line 125\n    const-string v3, "326618d8-3242-4322-a1b1-9e60e5d3dfc3"\n\n    const-string v4, "{AERIAL_ID}"\n\n    .line 126\n    .line 127\n    filled-new-array {{v4, v2, v3, v0, v1}}, [Ljava/lang/String;'''
assert s.count(old) == 1, 'wallpaper trust registry anchor mismatch'
s = s.replace(old, new, 1)
bs4.write_text(s)

# Reproduce the frozen CLEAN 01L manifest policy, then add only the Aerial
# wallpaper-provider service. No Configure Activity or other runtime component
# is added in CLEAN 02.
manifest = root / 'AndroidManifest.xml'
m = manifest.read_text()
patterns = [
    r'\s*<uses-permission android:name="com\.android\.vending\.BILLING"\s*/>',
    r'\s*<activity[^>]*android:name="com\.android\.billingclient\.api\.ProxyBillingActivity(?:V2)?"[^>]*/>',
    r'\s*<meta-data[^>]*android:name="com\.google\.android\.play\.billingclient\.version"[^>]*/>',
    r'\s*<intent>\s*<action android:name="com\.android\.vending\.billing\.InAppBillingService\.BIND"\s*/>\s*</intent>',
    r'\s*<intent>\s*<action android:name="com\.google\.android\.apps\.play\.billingtestcompanion\.BillingOverrideService\.BIND"\s*/>\s*</intent>',
    r'\s*<activity[^>]*android:name="com\.spocky\.projengmenu\.ui\.guidedActions\.activities\.about\.PremiumActivity"[^>]*/>',
    r'\s*<activity[^>]*android:name="com\.spocky\.projengmenu\.ui\.guidedActions\.activities\.about\.PremiumCardActivity"[^>]*/>',
    r'\s*<meta-data[^>]*android:name="com\.google\.firebase\.components:com\.google\.firebase\.perf\.FirebasePerfKtxRegistrar"[^>]*/>',
    r'\s*<meta-data[^>]*android:name="com\.google\.firebase\.components:com\.google\.firebase\.perf\.FirebasePerfRegistrar"[^>]*/>',
    r'\s*<meta-data[^>]*android:name="com\.google\.firebase\.components:com\.google\.firebase\.crashlytics\.FirebaseCrashlyticsKtxRegistrar"[^>]*/>',
    r'\s*<meta-data[^>]*android:name="com\.google\.firebase\.components:com\.google\.firebase\.crashlytics\.CrashlyticsRegistrar"[^>]*/>',
    r'\s*<meta-data[^>]*android:name="com\.google\.firebase\.components:com\.google\.firebase\.analytics\.connector\.internal\.AnalyticsConnectorRegistrar"[^>]*/>',
    r'\s*<meta-data[^>]*android:name="com\.google\.firebase\.components:com\.google\.firebase\.sessions\.FirebaseSessionsRegistrar"[^>]*/>',
    r'\s*<meta-data[^>]*android:name="com\.google\.firebase\.components:com\.google\.firebase\.datatransport\.TransportRegistrar"[^>]*/>',
    r'\s*<receiver[^>]*android:name="com\.google\.android\.gms\.measurement\.AppMeasurementReceiver"[^>]*/>',
    r'\s*<service[^>]*android:name="com\.google\.android\.gms\.measurement\.AppMeasurementService"[^>]*/>',
    r'\s*<service[^>]*android:name="com\.google\.android\.gms\.measurement\.AppMeasurementJobService"[^>]*/>',
    r'\s*<service[^>]*android:name="com\.google\.firebase\.sessions\.SessionLifecycleService"[^>]*/>',
    r'\s*<service[^>]*android:name="com\.google\.android\.datatransport\.runtime\.backends\.TransportBackendDiscovery"[^>]*>\s*<meta-data[^>]*/>\s*</service>',
    r'\s*<service[^>]*android:name="com\.google\.android\.datatransport\.runtime\.scheduling\.jobscheduling\.JobInfoSchedulerService"[^>]*/>',
    r'\s*<receiver[^>]*android:name="com\.google\.android\.datatransport\.runtime\.scheduling\.jobscheduling\.AlarmManagerSchedulerBroadcastReceiver"[^>]*/>',
]
for pattern in patterns:
    m = re.sub(pattern, '', m)

required = [
    'com.google.firebase.provider.FirebaseInitProvider',
    'com.google.firebase.components.ComponentDiscoveryService',
    'com.google.firebase.remoteconfig.FirebaseRemoteConfigKtxRegistrar',
    'com.google.firebase.remoteconfig.RemoteConfigRegistrar',
    'com.google.firebase.installations.FirebaseInstallationsKtxRegistrar',
    'com.google.firebase.installations.FirebaseInstallationsRegistrar',
    'com.google.firebase.FirebaseCommonKtxRegistrar',
    'com.google.firebase.abt.component.AbtRegistrar',
]
for value in required:
    assert value in m, value

forbidden = [
    'FirebasePerfRegistrar', 'CrashlyticsRegistrar', 'AnalyticsConnectorRegistrar',
    'FirebaseSessionsRegistrar', 'TransportRegistrar', 'AppMeasurementReceiver',
    'AppMeasurementService', 'AppMeasurementJobService', 'SessionLifecycleService',
    'TransportBackendDiscovery', 'JobInfoSchedulerService',
    'AlarmManagerSchedulerBroadcastReceiver',
]
for value in forbidden:
    assert value not in m, value

assert 'android.permission.INTERNET' in m, 'Projectivy INTERNET permission missing'
assert 'com.spocky.projengmenu.embedded.AerialService' not in m
service = f'''        <service android:exported="true" android:label="Aerial Rows" android:name="com.spocky.projengmenu.embedded.AerialService">\n            <intent-filter>\n                <action android:name="tv.projectivy.plugin.WALLPAPER_PROVIDER"/>\n            </intent-filter>\n            <meta-data android:name="apiVersion" android:value="1"/>\n            <meta-data android:name="uuid" android:value="{AERIAL_ID}"/>\n            <meta-data android:name="name" android:value="Aerial Rows"/>\n            <meta-data android:name="itemsCacheDurationMillis" android:value="21600000"/>\n            <meta-data android:name="updateMode" android:value="1"/>\n        </service>\n'''
assert m.count('    </application>') == 1
m = m.replace('    </application>', service + '    </application>', 1)
assert 'settingsActivity' not in service
manifest.write_text(m)

print('PASS: CLEAN 02 decoded tree patched: trusted Aerial provider + service only')
