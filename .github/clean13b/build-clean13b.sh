#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-12-1B-Description-Normalization-Fix.apk
UNSIGNED=/tmp/unsigned-clean13b.apk
FINAL=/tmp/Projectivy-CLEAN-13B-Date-After-Resume.apk
ART=/tmp/clean13b-artifact

# Rebuild the exact frozen CLEAN12.1B stack first.
bash .github/clean121b/build-clean121b.sh
test -f "$BASE"

# Apply only CLEAN13B's safe MainActivity-resume date migration.
python3 .github/clean13b/prepare-clean13b.py

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
rm -rf /tmp/embedded13b-classes /tmp/embedded13b-dex
mkdir -p /tmp/embedded13b-classes /tmp/embedded13b-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded13b-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java
mapfile -t CLASSFILES < <(find /tmp/embedded13b-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded13b-dex "${CLASSFILES[@]}"
cp /tmp/embedded13b-dex/classes.dex /tmp/classes4-clean13b.dex

python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-12-1B-Description-Normalization-Fix.apk'
out='/tmp/unsigned-clean13b.apk'
replacement=open('/tmp/classes4-clean13b.dex','rb').read()
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
python3 .github/clean13b/verify-clean13b.py "$BASE" "$UNSIGNED"

"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" /tmp/aligned-clean13b.apk
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" /tmp/aligned-clean13b.apk
"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean13b.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean13b.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean13b.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean13b.txt

rm -rf "$ART"
mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean13b.txt "$ART/"
cp -f /tmp/apksigner-clean13b.txt "$ART/"
cp -f /tmp/zipalign-clean13b.txt "$ART/"
cp -f /tmp/package-clean13b.txt "$ART/"
printf '%s\n' \
  'CLEAN13B: date beside native Projectivy clock, safe lifecycle revision' \
  '- Based directly on frozen CLEAN12.1B.' \
  '- Waits for canonical profile restore completion.' \
  '- Applies clock Line 1 only after MainActivity reaches RESUMED.' \
  '- Preserves stored clock expression; derives 12/24h from Android if absent.' \
  '- Appends EEE d MMM beside time.' \
  '- Uses one normal Activity.recreate() to reread settings.' \
  '- No dj3.m(), w40, q32, or startup-provider reflection.' \
  '- CLEAN12.1B differential Red Light sync and CLEAN10.3 routing retained.' \
  > "$ART/CLEAN13B-NOTES.txt"
