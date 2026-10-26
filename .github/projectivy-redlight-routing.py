from pathlib import Path

manifest = Path('/tmp/projectivy-decoded/AndroidManifest.xml')
text = manifest.read_text()

old = '        <activity android:excludeFromRecents="true" android:exported="false" android:name="com.spocky.projengmenu.embedded.RedLightLaunchActivity" android:noHistory="true" android:theme="@android:style/Theme.Translucent.NoTitleBar"/>'
new = '''        <activity android:excludeFromRecents="true" android:exported="false" android:name="com.spocky.projengmenu.embedded.RedLightLaunchActivity" android:noHistory="true" android:theme="@android:style/Theme.Translucent.NoTitleBar">
            <intent-filter>
                <action android:name="android.intent.action.VIEW"/>
                <category android:name="android.intent.category.DEFAULT"/>
                <data android:scheme="plugin" android:host="plugin.video.redlight"/>
            </intent-filter>
        </activity>'''

if 'android:host="plugin.video.redlight"' not in text:
    if text.count(old) != 1:
        raise SystemExit(f'Red Light activity manifest anchor mismatch: {text.count(old)}')
    text = text.replace(old, new, 1)

# The Shield logcat proves Projectivy ultimately emits ACTION_VIEW plugin://...
# with pkg=com.spocky.projengmenu.  This filter deliberately catches that
# package-pinned implicit intent inside Projectivy, after which the invisible
# activity forwards the untouched URI to Kodi.
checks = [
    'com.spocky.projengmenu.embedded.RedLightLaunchActivity',
    'android:scheme="plugin"',
    'android:host="plugin.video.redlight"',
]
for needle in checks:
    if needle not in text:
        raise SystemExit(f'missing Red Light routing manifest assertion: {needle}')

manifest.write_text(text)
print('patched Red Light package-pinned ACTION_VIEW routing')
