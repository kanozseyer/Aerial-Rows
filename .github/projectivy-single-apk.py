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

# Components live in the injected runtime DEX but belong to Projectivy itself.
# settingsActivity is deliberately relative: Projectivy concatenates the owning
# package name with this metadata value when its Configure action is clicked.
if 'com.spocky.projengmenu.embedded.AerialService' not in text:
    app_end = '    </application>'
    components = '''        <provider android:authorities="com.spocky.projengmenu.embedded.bootstrap" android:exported="false" android:initOrder="110" android:name="com.spocky.projengmenu.embedded.EmbeddedRuntime$BootstrapProvider"/>\n        <activity android:excludeFromRecents="true" android:exported="false" android:name="com.spocky.projengmenu.embedded.AerialSettingsActivity" android:screenOrientation="landscape" android:theme="@android:style/Theme.DeviceDefault"/>\n        <activity android:excludeFromRecents="true" android:exported="false" android:name="com.spocky.projengmenu.embedded.RedLightLaunchActivity" android:noHistory="true" android:theme="@android:style/Theme.Translucent.NoTitleBar"/>\n        <service android:exported="true" android:label="Aerial Rows" android:name="com.spocky.projengmenu.embedded.AerialService">\n            <intent-filter>\n                <action android:name="tv.projectivy.plugin.WALLPAPER_PROVIDER"/>\n            </intent-filter>\n            <meta-data android:name="apiVersion" android:value="1"/>\n            <meta-data android:name="uuid" android:value="8c00feaf-c7be-43c2-80f9-d49853aaa99a"/>\n            <meta-data android:name="name" android:value="Aerial Rows"/>\n            <meta-data android:name="settingsActivity" android:value=".embedded.AerialSettingsActivity"/>\n            <meta-data android:name="itemsCacheDurationMillis" android:value="21600000"/>\n            <meta-data android:name="updateMode" android:value="1"/>\n        </service>\n        <service android:exported="false" android:name="com.spocky.projengmenu.embedded.EmbeddedRuntime$SyncJob" android:permission="android.permission.BIND_JOB_SERVICE"/>\n        <receiver android:enabled="true" android:exported="true" android:name="com.spocky.projengmenu.embedded.EmbeddedRuntime$BootReceiver">\n            <intent-filter>\n                <action android:name="android.intent.action.BOOT_COMPLETED"/>\n            </intent-filter>\n        </receiver>\n'''
    if app_end not in text:
        raise SystemExit('manifest application end missing')
    text = text.replace(app_end, components + app_end, 1)

if 'android:name="com.spocky.projengmenu.embedded.AerialSettingsActivity"' not in text:
    raise SystemExit('Aerial settings activity missing after manifest patch')
if 'android:name="settingsActivity" android:value=".embedded.AerialSettingsActivity"' not in text:
    raise SystemExit('Aerial Configure metadata missing after manifest patch')
if 'android:name="com.spocky.projengmenu.embedded.RedLightLaunchActivity"' not in text:
    raise SystemExit('Red Light launch trampoline missing after manifest patch')
manifest.write_text(text)
print('patched and verified embedded runtime manifest components')

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

# Projectivy normally overwrites every Android-TV program intent with the package
# which owns that TV row. Preserve explicit components. Red Light cards now point
# to a Projectivy-owned trampoline, which then forwards the exact plugin:// URI to Kodi.
ck3 = root / 'smali/ck3.smali'
text = ck3.read_text()
start = text.index('.method public final t(Landroid/content/Context;Landroid/os/Bundle;)Landroid/content/Intent;')
end = text.index('.end method', start)
method = text[start:end]
old_block = '''    if-eqz v1, :cond_2\n\n    .line 28\n    .line 29\n    check-cast p0, Lxu;'''
new_block = '''    if-eqz v1, :cond_2\n\n    invoke-virtual {v1}, Landroid/content/Intent;->getComponent()Landroid/content/ComponentName;\n    move-result-object v2\n    if-nez v2, :embedded_explicit_component_ready\n\n    .line 28\n    .line 29\n    check-cast p0, Lxu;'''
if 'embedded_explicit_component_ready' not in method:
    if method.count(old_block) != 1:
        raise SystemExit(f'preview intent explicit-component anchor mismatch: {method.count(old_block)}')
    method = method.replace(old_block, new_block, 1)
    launch_anchor = '''    invoke-virtual {v1, p0}, Landroid/content/Intent;->setPackage(Ljava/lang/String;)Landroid/content/Intent;\n\n    .line 40\n    .line 41\n    .line 42\n    invoke-static {v1, p1, p2}, Lt4;->c(Landroid/content/Intent;Landroid/content/Context;Landroid/os/Bundle;)Landroid/content/Intent;'''
    launch_replacement = '''    invoke-virtual {v1, p0}, Landroid/content/Intent;->setPackage(Ljava/lang/String;)Landroid/content/Intent;\n\n    :embedded_explicit_component_ready\n    .line 40\n    .line 41\n    .line 42\n    invoke-static {v1, p1, p2}, Lt4;->c(Landroid/content/Intent;Landroid/content/Context;Landroid/os/Bundle;)Landroid/content/Intent;'''
    if method.count(launch_anchor) != 1:
        raise SystemExit(f'preview intent launch anchor mismatch: {method.count(launch_anchor)}')
    method = method.replace(launch_anchor, launch_replacement, 1)
    text = text[:start] + method + text[end:]
    ck3.write_text(text)
    print('patched preview program explicit components')

print('single-package Projectivy patch complete')
