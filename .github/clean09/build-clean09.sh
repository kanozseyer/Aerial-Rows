#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-08-5-Raw-Alpha-Process-Reload.apk
UNSIGNED=/tmp/unsigned-clean09.apk
FINAL=/tmp/Projectivy-CLEAN-09-Movie-Episode-Playback.apk
ART=/tmp/clean09-artifact
APKTOOL=/tmp/apktool.jar

# 1) Produce the exact frozen CLEAN08.5 output first. This also leaves the
# CLEAN07L/CLEAN08 source preparation in the CI workspace, which CLEAN09 then
# extends without touching any frozen generator script.
bash .github/clean085/build-clean085.sh

test -f "$BASE"

# 2) Patch only the prepared Red Light source for movie/episode click routing.
python3 .github/clean09/prepare-clean09.py

# 3) Recompile only the embedded classes4.dex from the same source set as the
# frozen pipeline, plus the one CLEAN09 trampoline activity.
PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
rm -rf /tmp/embedded09-classes /tmp/embedded09-dex
mkdir -p /tmp/embedded09-classes /tmp/embedded09-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded09-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java
mapfile -t CLASSFILES < <(find /tmp/embedded09-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded09-dex "${CLASSFILES[@]}"
cp /tmp/embedded09-dex/classes.dex /tmp/classes4-clean09.dex

for s in \
  'ProfileRestoreProvider' \
  'RedLightDiscoveryProvider' \
  'RedLightLaunchActivity' \
  'ProjectivyRedLightLaunch' \
  'CLEAN09 forwarding kind=' \
  'CLEAN085 killing process after raw opaque appearance commit' \
  'redlight:trakt:watchlist:movies' \
  'redlight:trakt:watchlist:shows'; do
  strings /tmp/classes4-clean09.dex | grep -q "$s"
done

# 4) Generate a binary manifest from the frozen APK with only the CLEAN09
# Activity added. All other APK entries come directly from the frozen APK.
rm -rf /tmp/pj-clean09-manifest
java -jar "$APKTOOL" d -f "$BASE" -o /tmp/pj-clean09-manifest >/dev/null
python3 .github/clean09/patch-manifest.py /tmp/pj-clean09-manifest
java -jar "$APKTOOL" b /tmp/pj-clean09-manifest -o /tmp/rebuilt-clean09-manifest.apk >/dev/null
unzip -p /tmp/rebuilt-clean09-manifest.apk AndroidManifest.xml > /tmp/AndroidManifest-clean09.xml

# 5) Repackage from frozen CLEAN08.5, replacing exactly classes4.dex and the
# manifest. Old signatures are omitted; zipalign/apksigner below recreate them.
python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-08-5-Raw-Alpha-Process-Reload.apk'
out='/tmp/unsigned-clean09.apk'
repl={
    'classes4.dex': open('/tmp/classes4-clean09.dex','rb').read(),
    'AndroidManifest.xml': open('/tmp/AndroidManifest-clean09.xml','rb').read(),
}
seen=set()
with zipfile.ZipFile(base) as zin, zipfile.ZipFile(out,'w') as zout:
    for i in zin.infolist():
        if i.filename.startswith('META-INF/'):
            continue
        data=repl.get(i.filename, zin.read(i.filename))
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
        seen.add(i.filename)
assert set(repl).issubset(seen), (set(repl)-seen)
PY
unzip -t "$UNSIGNED" >/dev/null

# 6) Decode and run every inherited CLEAN08.5 invariant plus CLEAN09 guards.
rm -rf /tmp/verify-clean09
java -jar "$APKTOOL" d -f "$UNSIGNED" -o /tmp/verify-clean09 >/tmp/final-clean09-decode.log 2>&1
python3 .github/clean09/verify-clean09.py "$UNSIGNED"

# 7) Same final alignment/signing identity as the frozen custom builds.
"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" /tmp/aligned-clean09.apk
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" /tmp/aligned-clean09.apk
"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean09.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean09.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean09.txt
"$BT/aapt" dump xmltree "$FINAL" AndroidManifest.xml > /tmp/manifest-clean09.txt

grep -q "package: name='com.spocky.projengmenu'" /tmp/package-clean09.txt
grep -q "versionName='4.72'" /tmp/package-clean09.txt
grep -q 'RedLightLaunchActivity' /tmp/manifest-clean09.txt

sha256sum "$FINAL" | tee /tmp/SHA256-clean09.txt
mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f \
  /tmp/SHA256-clean09.txt \
  /tmp/apksigner-clean09.txt \
  /tmp/zipalign-clean09.txt \
  /tmp/package-clean09.txt \
  /tmp/manifest-clean09.txt \
  /tmp/final-clean09-decode.log \
  "$ART/"
