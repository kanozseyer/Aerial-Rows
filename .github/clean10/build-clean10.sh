#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-09-1-Movie-Episode-Playback-Intent-Filter.apk
UNSIGNED=/tmp/unsigned-clean10.apk
FINAL=/tmp/Projectivy-CLEAN-10-TV-Show-Navigation.apk
ART=/tmp/clean10-artifact
APKTOOL=/tmp/apktool.jar

# 1) Build and fully verify the exact frozen CLEAN09 final first. This leaves
# RedLightDiscoveryEngine.java prepared with CLEAN09.1 movie/episode routing.
bash .github/clean09/build-clean09.sh

test -f "$BASE"

# 2) Extend only the media-type scope so type=1 TV-show cards use the same
# already-proven trampoline. Kodi/Red Light owns the season/episode UI.
python3 .github/clean10/prepare-clean10.py

# 3) Recompile the same embedded classes4 source set. No manifest/resource
# change is required: CLEAN09.1's narrow plugin.video.redlight VIEW filter
# already resolves these TV-show plugin URIs into RedLightLaunchActivity.
PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
rm -rf /tmp/embedded10-classes /tmp/embedded10-dex
mkdir -p /tmp/embedded10-classes /tmp/embedded10-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded10-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java
mapfile -t CLASSFILES < <(find /tmp/embedded10-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded10-dex "${CLASSFILES[@]}"
cp /tmp/embedded10-dex/classes.dex /tmp/classes4-clean10.dex

for s in \
  'ProfileRestoreProvider' \
  'RedLightDiscoveryProvider' \
  'RedLightLaunchActivity' \
  'ProjectivyRedLightLaunch' \
  'CLEAN09 forwarding kind=' \
  'show' \
  'CLEAN085 killing process after raw opaque appearance commit' \
  'redlight:trakt:watchlist:movies' \
  'redlight:trakt:watchlist:shows'; do
  strings /tmp/classes4-clean10.dex | grep -q "$s"
done

# 4) Repackage directly from the runtime-proven CLEAN09 final, replacing only
# classes4.dex. Manifest and every other payload entry remain byte-identical.
python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-09-1-Movie-Episode-Playback-Intent-Filter.apk'
out='/tmp/unsigned-clean10.apk'
replacement=open('/tmp/classes4-clean10.dex','rb').read()
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
assert seen, 'classes4.dex missing from CLEAN09 base'
PY
unzip -t "$UNSIGNED" >/dev/null

# 5) Decode and prove CLEAN10 is exactly one payload-file delta over the
# runtime-proven CLEAN09 final while retaining all historical invariants.
rm -rf /tmp/verify-clean10
java -jar "$APKTOOL" d -f "$UNSIGNED" -o /tmp/verify-clean10 >/tmp/final-clean10-decode.log 2>&1
python3 .github/clean10/verify-clean10.py "$UNSIGNED"

# 6) Re-sign using the same identity as every frozen custom Projectivy build.
"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" /tmp/aligned-clean10.apk
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" /tmp/aligned-clean10.apk
"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean10.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean10.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean10.txt
"$BT/aapt" dump xmltree "$FINAL" AndroidManifest.xml > /tmp/manifest-clean10.txt

grep -q "package: name='com.spocky.projengmenu'" /tmp/package-clean10.txt
grep -q "versionName='4.72'" /tmp/package-clean10.txt
grep -q 'RedLightLaunchActivity' /tmp/manifest-clean10.txt
grep -q 'plugin.video.redlight' /tmp/manifest-clean10.txt

sha256sum "$FINAL" | tee /tmp/SHA256-clean10.txt
rm -rf "$ART"
mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f \
  /tmp/SHA256-clean10.txt \
  /tmp/apksigner-clean10.txt \
  /tmp/zipalign-clean10.txt \
  /tmp/package-clean10.txt \
  /tmp/manifest-clean10.txt \
  /tmp/final-clean10-decode.log \
  "$ART/"
