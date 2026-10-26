from pathlib import Path

root = Path('/tmp/projectivy-decoded')


def replace_once(path: Path, old: str, new: str, label: str):
    text = path.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected 1 anchor, got {count} in {path}')
    path.write_text(text.replace(old, new, 1))
    print('patched', label)


# Kodi visibility for the embedded Red Light engine.
manifest = root / 'AndroidManifest.xml'
text = manifest.read_text()
queries_anchor = '        <package android:name="com.android.vending"/>'
if 'org.xbmc.kodi.file' not in text:
    kodi_queries = '''        <package android:name="org.xbmc.kodi"/>\n        <provider android:authorities="org.xbmc.kodi.file"/>\n'''
    if queries_anchor not in text:
        raise SystemExit('manifest queries anchor missing')
    text = text.replace(queries_anchor, kodi_queries + queries_anchor, 1)

# Components live in classes4.dex but belong to the Projectivy package.
if 'com.spocky.projengmenu.embedded.AerialService' not in text:
    app_end = '    </application>'
    components = '''        <provider android:authorities="com.spocky.projengmenu.embedded.bootstrap" android:exported="false" android:initOrder="110" android:name="com.spocky.projengmenu.embedded.EmbeddedRuntime$BootstrapProvider"/>\n        <service android:exported="true" android:label="Aerial Rows" android:name="com.spocky.projengmenu.embedded.AerialService">\n            <intent-filter>\n                <action android:name="tv.projectivy.plugin.WALLPAPER_PROVIDER"/>\n            </intent-filter>\n            <meta-data android:name="apiVersion" android:value="1"/>\n            <meta-data android:name="uuid" android:value="8c00feaf-c7be-43c2-80f9-d49853aaa99a"/>\n            <meta-data android:name="name" android:value="Aerial Rows"/>\n            <meta-data android:name="settingsActivity" android:value=""/>\n            <meta-data android:name="itemsCacheDurationMillis" android:value="21600000"/>\n            <meta-data android:name="updateMode" android:value="1"/>\n        </service>\n        <service android:exported="false" android:name="com.spocky.projengmenu.embedded.EmbeddedRuntime$SyncJob" android:permission="android.permission.BIND_JOB_SERVICE"/>\n        <receiver android:enabled="true" android:exported="true" android:name="com.spocky.projengmenu.embedded.EmbeddedRuntime$BootReceiver">\n            <intent-filter>\n                <action android:name="android.intent.action.BOOT_COMPLETED"/>\n            </intent-filter>\n        </receiver>\n'''
    if app_end not in text:
        raise SystemExit('manifest application end missing')
    text = text.replace(app_end, components + app_end, 1)
manifest.write_text(text)
print('patched embedded runtime manifest components')

# Every launcher resume refreshes Aerial immediately and asks the Red Light engine
# to rescan. This fixes the previous need to open the left header before video starts.
main = root / 'smali/com/spocky/projengmenu/ui/home/MainActivity.smali'
text = main.read_text()
start = text.index('.method public final onResume()V')
end = text.index('.end method', start)
method = text[start:end]
hook = '''    invoke-super {p0}, Lss;->onResume()V\n\n    invoke-static {p0}, Lcom/spocky/projengmenu/embedded/EmbeddedRuntime;->onLauncherResume(Landroid/content/Context;)V'''
old = '    invoke-super {p0}, Lss;->onResume()V'
if 'EmbeddedRuntime;->onLauncherResume' not in method:
    if method.count(old) != 1:
        raise SystemExit(f'MainActivity onResume super anchor mismatch: {method.count(old)}')
    method = method.replace(old, hook, 1)
    text = text[:start] + method + text[end:]
    main.write_text(text)
    print('patched MainActivity embedded runtime resume hook')

print('single-package Projectivy patch complete')
