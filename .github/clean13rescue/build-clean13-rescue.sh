#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-12-1B-Description-Normalization-Fix.apk
UNSIGNED=/tmp/unsigned-clean13-rescue.apk
FINAL=/tmp/Projectivy-CLEAN-13-RESCUE-Clock-Reset.apk
ART=/tmp/clean13-rescue-artifact

bash .github/clean121b/build-clean121b.sh
test -f "$BASE"
python3 .github/clean13rescue/prepare-clean13-rescue.py

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
rm -rf /tmp/embedded13rescue-classes /tmp/embedded13rescue-dex
mkdir -p /tmp/embedded13rescue-classes /tmp/embedded13rescue-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded13rescue-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java
mapfile -t CLASSFILES < <(find /tmp/embedded13rescue-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded13rescue-dex "${CLASSFILES[@]}"
cp /tmp/embedded13rescue-dex/classes.dex /tmp/classes4-clean13-rescue.dex

python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-12-1B-Description-Normalization-Fix.apk'
out='/tmp/unsigned-clean13-rescue.apk'
replacement=open('/tmp/classes4-clean13-rescue.dex','rb').read()
seen=False
with zipfile.ZipFile(base) as zin, zipfile.ZipFile(out,'w') as zout:
    for i in zin.infolist():
        if i.filename.startswith('META-INF/'):
            continue
        data=zin.read(i.filename)
        if i.filename == 'classes4.dex':
            data=replacement; seen=True
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
python3 .github/clean13rescue/verify-clean13-rescue.py "$BASE" "$UNSIGNED"
"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" /tmp/aligned-clean13-rescue.apk
"$BT/apksigner" sign --key /tmp/testkey.pk8 --cert /tmp/testkey.x509.pem --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --out "$FINAL" /tmp/aligned-clean13-rescue.apk
"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean13-rescue.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean13-rescue.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean13-rescue.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean13-rescue.txt
rm -rf "$ART"; mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean13-rescue.txt "$ART/"
cp -f /tmp/apksigner-clean13-rescue.txt "$ART/"
cp -f /tmp/package-clean13-rescue.txt "$ART/"
printf '%s\n' \
 'CLEAN13 RESCUE — frozen CLEAN12.1B plus one-time removal of appearance_statusbar_clock_format_l1.' \
 'Purpose: recover from failed CLEAN13 without clearing Projectivy app data.' \
 > "$ART/CLEAN13-RESCUE-NOTES.txt"
