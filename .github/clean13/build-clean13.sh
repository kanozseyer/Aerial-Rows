#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-12-1B-Description-Normalization-Fix.apk
UNSIGNED=/tmp/unsigned-clean13.apk
FINAL=/tmp/Projectivy-CLEAN-13-Date-Next-To-Clock.apk
ART=/tmp/clean13-artifact

# Build the exact frozen CLEAN12.1B stack first. CLEAN13 then changes only the
# embedded ProfileRestoreProvider clock preference migration and recompiles classes4.dex.
bash .github/clean121b/build-clean121b.sh
test -f "$BASE"
python3 .github/clean13/prepare-clean13.py

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
rm -rf /tmp/embedded13-classes /tmp/embedded13-dex
mkdir -p /tmp/embedded13-classes /tmp/embedded13-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded13-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java
mapfile -t CLASSFILES < <(find /tmp/embedded13-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded13-dex "${CLASSFILES[@]}"
cp /tmp/embedded13-dex/classes.dex /tmp/classes4-clean13.dex

python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-12-1B-Description-Normalization-Fix.apk'
out='/tmp/unsigned-clean13.apk'
replacement=open('/tmp/classes4-clean13.dex','rb').read()
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
python3 .github/clean13/verify-clean13.py "$BASE" "$UNSIGNED"

"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" /tmp/aligned-clean13.apk
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" /tmp/aligned-clean13.apk
"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean13.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean13.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean13.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean13.txt

rm -rf "$ART"
mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean13.txt "$ART/"
cp -f /tmp/apksigner-clean13.txt "$ART/"
cp -f /tmp/zipalign-clean13.txt "$ART/"
cp -f /tmp/package-clean13.txt "$ART/"

printf '%s\n' \
  'CLEAN13: date next to native Projectivy clock' \
  '- Built from frozen projectivy-clean12-1-final / CLEAN12.1B stack.' \
  '- Uses Projectivy native ClockView formatter; no overlay and no extra timer.' \
  '- Existing Line 1 time format is preserved and "   EEE d MMM" appended once.' \
  '- If Line 1 already contains date tokens, it is left unchanged.' \
  '- Fresh installs apply after canonical profile restore; existing installs apply on provider startup.' \
  '- One-time completion marker means later user clock edits remain authoritative.' \
  '- Only classes4.dex differs from frozen CLEAN12.1B before signing.' \
  > "$ART/CLEAN13-NOTES.txt"
