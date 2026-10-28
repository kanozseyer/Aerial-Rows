#!/usr/bin/env bash
set -euo pipefail

OFFICIAL=/tmp/official.apk
UNSIGNED=/tmp/unsigned-clean05.apk
FINAL=/tmp/Projectivy-CLEAN-05-Auto-Profile-Restore.apk
ART=/tmp/clean05-artifact
APKTOOL=/tmp/apktool.jar

cat .github/clean05/profile-*.b64 | tr -d '\r\n' | base64 -d > /tmp/launcher_default_profile.plbackup
test "$(stat -c%s /tmp/launcher_default_profile.plbackup)" = "13760"
echo 'cda71dbae3d2eca7bd298a198aca4068187bde34f25727df9e9ddd18e8aac2b7  /tmp/launcher_default_profile.plbackup' | sha256sum -c -
sha256sum /tmp/launcher_default_profile.plbackup | tee /tmp/profile-SHA256.txt

curl -fL --retry 3 'https://github.com/spocky/miproja1/releases/download/4.72/ProjectivyLauncher-4.72-c97-xda-release.apk' -o "$OFFICIAL"
echo 'a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae  /tmp/official.apk' | sha256sum -c -
unzip -p "$OFFICIAL" classes.dex > /tmp/classes.dex
unzip -p "$OFFICIAL" classes2.dex > /tmp/classes2.dex

python3 .github/patch-clean-01k-projectivy-telemetry.py /tmp/classes.dex /tmp/classes2.dex /tmp/classes-lean.dex /tmp/classes2-lean.dex | tee /tmp/dex-base-patch.txt
[ "$(stat -c%s /tmp/classes.dex)" = "$(stat -c%s /tmp/classes-lean.dex)" ]
[ "$(stat -c%s /tmp/classes2.dex)" = "$(stat -c%s /tmp/classes2-lean.dex)" ]

python3 - <<'PY'
import zipfile
repl={'classes.dex':open('/tmp/classes-lean.dex','rb').read(),'classes2.dex':open('/tmp/classes2-lean.dex','rb').read()}
with zipfile.ZipFile('/tmp/official.apk') as zin, zipfile.ZipFile('/tmp/decode-input.apk','w') as zout:
    for i in zin.infolist():
        if i.filename.startswith('META-INF/'): continue
        data=repl.get(i.filename,zin.read(i.filename))
        n=zipfile.ZipInfo(i.filename,i.date_time)
        n.compress_type=i.compress_type; n.comment=i.comment; n.extra=i.extra
        n.internal_attr=i.internal_attr; n.external_attr=i.external_attr
        n.create_system=i.create_system; n.create_version=i.create_version
        n.extract_version=i.extract_version; n.flag_bits=i.flag_bits
        zout.writestr(n,data)
PY

curl -fL --retry 3 'https://github.com/iBotPeaches/Apktool/releases/download/v2.11.1/apktool_2.11.1.jar' -o "$APKTOOL"
java -jar "$APKTOOL" d -f /tmp/decode-input.apk -o /tmp/pj >/dev/null
python3 .github/clean03/patch-decoded.py /tmp/pj
python3 .github/clean05/patch-manifest.py /tmp/pj
java -jar "$APKTOOL" b /tmp/pj -o /tmp/rebuilt.apk >/dev/null
unzip -p /tmp/rebuilt.apk classes.dex > /tmp/classes-clean05.dex
unzip -p /tmp/rebuilt.apk AndroidManifest.xml > /tmp/AndroidManifest.xml

python3 .github/clean04/patch-backup-decrypt-dex.py /tmp/classes-clean05.dex /tmp/classes2-lean.dex /tmp/classes-clean05-fixed.dex /tmp/classes2-clean05-fixed.dex | tee /tmp/backup-fix.txt
mv /tmp/classes-clean05-fixed.dex /tmp/classes-clean05.dex
mv /tmp/classes2-clean05-fixed.dex /tmp/classes2-clean05.dex

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
mkdir -p /tmp/embedded-classes /tmp/embedded-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded-classes .github/clean02/AerialService.java .github/clean03/AerialSettingsActivity.java .github/clean05/ProfileRestoreProvider.java
mapfile -t CLASSFILES < <(find /tmp/embedded-classes -name '*.class' -type f | sort)
[ "${#CLASSFILES[@]}" -ge 5 ]
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded-dex "${CLASSFILES[@]}"
cp /tmp/embedded-dex/classes.dex /tmp/classes4.dex
for s in 'ProfileRestoreProvider' 'CLEAN05 restore SUCCESS' 'launcher_default_profile.plbackup' 'cda71dbae3d2eca7bd298a198aca4068187bde34f25727df9e9ddd18e8aac2b7' 'action_categories_list'; do strings /tmp/classes4.dex | grep -q "$s"; done

curl -fL --retry 3 'https://raw.githubusercontent.com/kanozseyer/Aerial-Rows/fbb317593c02c9997f380bbe5cf6a2ceb2bb9f81/projectivy-aerial-rows-provider/src/main/assets/aerials.json' -o /tmp/aerials.json
python3 -m json.tool /tmp/aerials.json >/dev/null
sha256sum /tmp/aerials.json | tee /tmp/aerials-SHA256.txt

python3 - <<'PY'
import zipfile
repl={'classes.dex':open('/tmp/classes-clean05.dex','rb').read(),'classes2.dex':open('/tmp/classes2-clean05.dex','rb').read(),'AndroidManifest.xml':open('/tmp/AndroidManifest.xml','rb').read()}
removed=[]
with zipfile.ZipFile('/tmp/official.apk') as zin, zipfile.ZipFile('/tmp/unsigned-clean05.apk','w') as zout:
    for i in zin.infolist():
        if i.filename.startswith('META-INF/'): continue
        if i.filename.startswith('lib/') and not i.filename.startswith('lib/arm64-v8a/'):
            removed.append(i.filename); continue
        data=repl.get(i.filename,zin.read(i.filename))
        n=zipfile.ZipInfo(i.filename,i.date_time)
        n.compress_type=i.compress_type; n.comment=i.comment; n.extra=i.extra
        n.internal_attr=i.internal_attr; n.external_attr=i.external_attr
        n.create_system=i.create_system; n.create_version=i.create_version
        n.extract_version=i.extract_version; n.flag_bits=i.flag_bits
        zout.writestr(n,data)
    assert len(removed)==9, removed
    zout.writestr('classes4.dex',open('/tmp/classes4.dex','rb').read(),compress_type=zipfile.ZIP_DEFLATED)
    zout.writestr('assets/aerials.json',open('/tmp/aerials.json','rb').read(),compress_type=zipfile.ZIP_DEFLATED)
    zout.writestr('assets/launcher_default_profile.plbackup',open('/tmp/launcher_default_profile.plbackup','rb').read(),compress_type=zipfile.ZIP_DEFLATED)
PY
unzip -t "$UNSIGNED" >/dev/null

rm -rf /tmp/verify-clean05
java -jar "$APKTOOL" d -f "$UNSIGNED" -o /tmp/verify-clean05 >/tmp/final-decode.log 2>&1
python3 .github/clean05/verify-clean05.py "$UNSIGNED"

curl -fL --retry 3 'https://android.googlesource.com/platform/build/+/refs/heads/master/target/product/security/testkey.pk8?format=TEXT' | base64 -d > /tmp/testkey.pk8
curl -fL --retry 3 'https://android.googlesource.com/platform/build/+/refs/heads/master/target/product/security/testkey.x509.pem?format=TEXT' | base64 -d > /tmp/testkey.x509.pem
"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" /tmp/aligned.apk
"$BT/apksigner" sign --key /tmp/testkey.pk8 --cert /tmp/testkey.x509.pem --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --out "$FINAL" /tmp/aligned.apk
"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package.txt
"$BT/aapt" dump xmltree "$FINAL" AndroidManifest.xml > /tmp/manifest.txt
grep -q "package: name='com.spocky.projengmenu'" /tmp/package.txt
grep -q "versionName='4.72'" /tmp/package.txt
grep -q 'ProfileRestoreProvider' /tmp/manifest.txt

python3 .github/clean05/scope-clean05.py "$OFFICIAL" "$FINAL" | tee /tmp/scope.txt
sha256sum "$FINAL" | tee /tmp/SHA256.txt
mkdir -p "$ART"
cp "$FINAL" "$ART/"
cp /tmp/SHA256.txt /tmp/scope.txt /tmp/dex-base-patch.txt /tmp/backup-fix.txt /tmp/apksigner.txt /tmp/zipalign.txt /tmp/package.txt /tmp/manifest.txt /tmp/final-decode.log /tmp/aerials-SHA256.txt /tmp/profile-SHA256.txt "$ART/"
