#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-09-1-Movie-Episode-Playback-Intent-Filter.apk
UNSIGNED=/tmp/unsigned-clean103.apk
FINAL=/tmp/Projectivy-CLEAN-10-3-Sanitized-TV-Browse-URI.apk
ART=/tmp/clean103-artifact
APKTOOL=/tmp/apktool.jar

# Build and fully verify frozen CLEAN09.1 first.
bash .github/clean09/build-clean09.sh
test -f "$BASE"

python3 .github/clean103/prepare-clean103.py

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
rm -rf /tmp/embedded103-classes /tmp/embedded103-dex
mkdir -p /tmp/embedded103-classes /tmp/embedded103-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded103-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java
mapfile -t CLASSFILES < <(find /tmp/embedded103-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded103-dex "${CLASSFILES[@]}"
cp /tmp/embedded103-dex/classes.dex /tmp/classes4-clean103.dex

for s in \
  'RedLightLaunchActivity' \
  'CLEAN10.3 browse clean uri=' \
  'CLEAN10 browse show ActivateWindow sent attempt=' \
  'build_season_list' \
  'GUI.ActivateWindow' \
  'CLEAN09 forwarding kind='; do
  strings /tmp/classes4-clean103.dex | grep -q "$s"
done

python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-09-1-Movie-Episode-Playback-Intent-Filter.apk'
out='/tmp/unsigned-clean103.apk'
replacement=open('/tmp/classes4-clean103.dex','rb').read()
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
assert seen
PY
unzip -t "$UNSIGNED" >/dev/null

rm -rf /tmp/verify-clean103
java -jar "$APKTOOL" d -f "$UNSIGNED" -o /tmp/verify-clean103 >/tmp/final-clean103-decode.log 2>&1
python3 .github/clean103/verify-clean103.py "$UNSIGNED"

"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" /tmp/aligned-clean103.apk
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" /tmp/aligned-clean103.apk
"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean103.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean103.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean103.txt
"$BT/aapt" dump xmltree "$FINAL" AndroidManifest.xml > /tmp/manifest-clean103.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean103.txt

rm -rf "$ART"
mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean103.txt /tmp/apksigner-clean103.txt /tmp/zipalign-clean103.txt \
  /tmp/package-clean103.txt /tmp/manifest-clean103.txt /tmp/final-clean103-decode.log "$ART/"
