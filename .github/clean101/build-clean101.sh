#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-09-1-Movie-Episode-Playback-Intent-Filter.apk
UNSIGNED=/tmp/unsigned-clean101.apk
FINAL=/tmp/Projectivy-CLEAN-10-1-TV-Show-GUI-Navigation.apk
ART=/tmp/clean101-artifact
APKTOOL=/tmp/apktool.jar

# 1) Build and fully verify the runtime-proven CLEAN09.1 base first.
bash .github/clean09/build-clean09.sh
test -f "$BASE"

# 2) Add only the distinct TV-show GUI navigation path.
python3 .github/clean101/prepare-clean101.py

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"

rm -rf /tmp/embedded101-classes /tmp/embedded101-dex
mkdir -p /tmp/embedded101-classes /tmp/embedded101-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded101-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java
mapfile -t CLASSFILES < <(find /tmp/embedded101-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded101-dex "${CLASSFILES[@]}"
cp /tmp/embedded101-dex/classes.dex /tmp/classes4-clean101.dex

for s in \
  'CLEAN09 forwarding kind=' \
  'CLEAN10 browse show bootstrap' \
  'CLEAN10 browse show ActivateWindow sent attempt=' \
  'GUI.ActivateWindow' \
  '127.0.0.1' \
  'ProjectivyRedLightBrowse' \
  'CLEAN085 killing process after raw opaque appearance commit'; do
  strings /tmp/classes4-clean101.dex | grep -q "$s"
done

# 3) Repackage directly from runtime-proven CLEAN09.1, replacing classes4.dex only.
python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-09-1-Movie-Episode-Playback-Intent-Filter.apk'
out='/tmp/unsigned-clean101.apk'
replacement=open('/tmp/classes4-clean101.dex','rb').read()
seen=False
with zipfile.ZipFile(base) as zin, zipfile.ZipFile(out,'w') as zout:
    for i in zin.infolist():
        if i.filename.startswith('META-INF/'):
            continue
        data=zin.read(i.filename)
        if i.filename == 'classes4.dex':
            data=replacement
            seen=True
        n=zipfile.ZipInfo(i.filename, i.date_time)
        n.compress_type=i.compress_type
        n.comment=i.comment
        n.extra=i.extra
        n.internal_attr=i.internal_attr
        n.external_attr=i.external_attr
        n.create_system=i.create_system
        n.create_version=i.create_version
        n.extract_version=i.extract_version
        n.flag_bits=i.flag_bits
        zout.writestr(n,data)
assert seen, 'classes4.dex missing from CLEAN09.1 base'
PY
unzip -t "$UNSIGNED" >/dev/null

# 4) Decode and prove one-file delta + inherited invariants.
rm -rf /tmp/verify-clean101
java -jar "$APKTOOL" d -f "$UNSIGNED" -o /tmp/verify-clean101 >/tmp/final-clean101-decode.log 2>&1
python3 .github/clean101/verify-clean101.py "$UNSIGNED"

# 5) Same signing identity as frozen custom Projectivy builds.
"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" /tmp/aligned-clean101.apk
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" /tmp/aligned-clean101.apk
"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean101.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean101.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean101.txt
"$BT/aapt" dump xmltree "$FINAL" AndroidManifest.xml > /tmp/manifest-clean101.txt

grep -q "package: name='com.spocky.projengmenu'" /tmp/package-clean101.txt
grep -q "versionName='4.72'" /tmp/package-clean101.txt
grep -q 'android.permission.INTERNET' /tmp/manifest-clean101.txt
grep -q 'RedLightLaunchActivity' /tmp/manifest-clean101.txt
grep -q 'plugin.video.redlight' /tmp/manifest-clean101.txt

sha256sum "$FINAL" | tee /tmp/SHA256-clean101.txt
rm -rf "$ART"
mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f \
  /tmp/SHA256-clean101.txt \
  /tmp/apksigner-clean101.txt \
  /tmp/zipalign-clean101.txt \
  /tmp/package-clean101.txt \
  /tmp/manifest-clean101.txt \
  /tmp/final-clean101-decode.log \
  "$ART/"
