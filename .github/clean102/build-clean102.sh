#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-09-1-Movie-Episode-Playback-Intent-Filter.apk
UNSIGNED=/tmp/unsigned-clean102.apk
FINAL=/tmp/Projectivy-CLEAN-10-2-TV-Show-URI-Navigation.apk
ART=/tmp/clean102-artifact
APKTOOL=/tmp/apktool.jar

# 1) Build and fully verify the exact frozen CLEAN09.1 base first.
bash .github/clean09/build-clean09.sh

test -f "$BASE"

# 2) Add TV-show cards and URI-authoritative folder navigation only.
python3 .github/clean102/prepare-clean102.py

# 3) Recompile the same embedded classes4 source set.
PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
rm -rf /tmp/embedded102-classes /tmp/embedded102-dex
mkdir -p /tmp/embedded102-classes /tmp/embedded102-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded102-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java
mapfile -t CLASSFILES < <(find /tmp/embedded102-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded102-dex "${CLASSFILES[@]}"
cp /tmp/embedded102-dex/classes.dex /tmp/classes4-clean102.dex

for s in \
  'ProfileRestoreProvider' \
  'RedLightDiscoveryProvider' \
  'RedLightLaunchActivity' \
  'ProjectivyRedLightLaunch' \
  'CLEAN09 forwarding kind=' \
  'CLEAN10 browse show bootstrap uri=' \
  'CLEAN10 browse show ActivateWindow sent attempt=' \
  'build_season_list' \
  'GUI.ActivateWindow' \
  'redlight:trakt:watchlist:movies' \
  'redlight:trakt:watchlist:shows'; do
  strings /tmp/classes4-clean102.dex | grep -q "$s"
done

# 4) Repackage directly from frozen CLEAN09.1, replacing only classes4.dex.
python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-09-1-Movie-Episode-Playback-Intent-Filter.apk'
out='/tmp/unsigned-clean102.apk'
replacement=open('/tmp/classes4-clean102.dex','rb').read()
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

# 5) Decode and prove this is a one-file delta over runtime-proven CLEAN09.1.
rm -rf /tmp/verify-clean102
java -jar "$APKTOOL" d -f "$UNSIGNED" -o /tmp/verify-clean102 >/tmp/final-clean102-decode.log 2>&1
python3 .github/clean102/verify-clean102.py "$UNSIGNED"

# 6) Same alignment/signing identity as frozen custom builds.
"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" /tmp/aligned-clean102.apk
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" /tmp/aligned-clean102.apk
"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean102.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean102.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean102.txt
"$BT/aapt" dump xmltree "$FINAL" AndroidManifest.xml > /tmp/manifest-clean102.txt

grep -q "package: name='com.spocky.projengmenu'" /tmp/package-clean102.txt
grep -q "versionName='4.72'" /tmp/package-clean102.txt
grep -q 'RedLightLaunchActivity' /tmp/manifest-clean102.txt
grep -q 'plugin.video.redlight' /tmp/manifest-clean102.txt

sha256sum "$FINAL" | tee /tmp/SHA256-clean102.txt
rm -rf "$ART"
mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f \
  /tmp/SHA256-clean102.txt \
  /tmp/apksigner-clean102.txt \
  /tmp/zipalign-clean102.txt \
  /tmp/package-clean102.txt \
  /tmp/manifest-clean102.txt \
  /tmp/final-clean102-decode.log \
  "$ART/"
