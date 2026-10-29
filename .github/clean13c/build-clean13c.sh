#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-12-1B-Description-Normalization-Fix.apk
UNSIGNED=/tmp/unsigned-clean13c.apk
FINAL=/tmp/Projectivy-CLEAN-13C-Native-Clock-Date.apk
ART=/tmp/clean13c-artifact

bash .github/clean121b/build-clean121b.sh
test -f "$BASE"
python3 .github/clean13c/prepare-clean13c.py

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
rm -rf /tmp/embedded13c-classes /tmp/embedded13c-dex
mkdir -p /tmp/embedded13c-classes /tmp/embedded13c-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded13c-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java
mapfile -t CLASSFILES < <(find /tmp/embedded13c-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded13c-dex "${CLASSFILES[@]}"
cp /tmp/embedded13c-dex/classes.dex /tmp/classes4-clean13c.dex

python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-12-1B-Description-Normalization-Fix.apk'
out='/tmp/unsigned-clean13c.apk'
replacement=open('/tmp/classes4-clean13c.dex','rb').read()
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
python3 .github/clean13c/verify-clean13c.py "$BASE" "$UNSIGNED"

"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" /tmp/aligned-clean13c.apk
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" /tmp/aligned-clean13c.apk
"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean13c.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean13c.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean13c.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean13c.txt

rm -rf "$ART"
mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean13c.txt "$ART/"
cp -f /tmp/apksigner-clean13c.txt "$ART/"
cp -f /tmp/zipalign-clean13c.txt "$ART/"
cp -f /tmp/package-clean13c.txt "$ART/"
printf '%s\n' \
  'CLEAN13C: native Projectivy date-next-to-clock setter' \
  '- Based directly on frozen CLEAN12.1B.' \
  '- Applies only after MainActivity is resumed and CLEAN05 profile is complete.' \
  '- Reads Projectivy effective clock Line 1 from native hj3.W1.a().' \
  '- Writes through Projectivy native yh3.h(Object) setter.' \
  '- Verifies native readback before marking migration complete.' \
  '- Appends EEE d MMM beside the existing effective time format.' \
  '- Uses one safe Activity.recreate(); no dj3.m() startup reset.' \
  '- CLEAN12.1B differential sync and CLEAN10.3 routing retained.' \
  > "$ART/CLEAN13C-NOTES.txt"
