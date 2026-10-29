#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-12-1-Differential-RedLight-Programs.apk
UNSIGNED=/tmp/unsigned-clean121b.apk
FINAL=/tmp/Projectivy-CLEAN-12-1B-Description-Normalization-Fix.apk
ART=/tmp/clean121b-artifact

# Build the proven CLEAN12.1 stack first so all frozen source transforms run in
# their validated order. CLEAN12.1B then removes only the one field proven by
# CLEAN12.1A diagnostics not to round-trip on this Shield TV provider.
bash .github/clean121/build-clean121.sh
test -f "$BASE"
python3 .github/clean121b/prepare-clean121b.py

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
rm -rf /tmp/embedded121b-classes /tmp/embedded121b-dex
mkdir -p /tmp/embedded121b-classes /tmp/embedded121b-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded121b-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java
mapfile -t CLASSFILES < <(find /tmp/embedded121b-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded121b-dex "${CLASSFILES[@]}"
cp /tmp/embedded121b-dex/classes.dex /tmp/classes4-clean121b.dex

python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-12-1-Differential-RedLight-Programs.apk'
out='/tmp/unsigned-clean121b.apk'
replacement=open('/tmp/classes4-clean121b.dex','rb').read()
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
python3 .github/clean121b/verify-clean121b.py "$UNSIGNED"

"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" /tmp/aligned-clean121b.apk
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" /tmp/aligned-clean121b.apk
"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean121b.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean121b.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean121b.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean121b.txt

rm -rf "$ART"
mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean121b.txt "$ART/"
cp -f /tmp/apksigner-clean121b.txt "$ART/"
cp -f /tmp/zipalign-clean121b.txt "$ART/"
cp -f /tmp/package-clean121b.txt "$ART/"

printf '%s\n' \
  'CLEAN12.1B: Shield PreviewProgram description normalization fix' \
  '- CLEAN12.1 stable differential card reconciliation retained.' \
  '- CLEAN12.1A proved description is the only repeatedly mismatching field.' \
  '- Stop writing/comparing PreviewProgram.description; Shield stores it empty anyway.' \
  '- Title, type, intent URI, weight, aspect ratio and poster comparison remain unchanged.' \
  '- Frozen CLEAN12 refresh lifecycle retained.' \
  '- Frozen CLEAN11.1 Kodi retention lease retained.' \
  '- Frozen CLEAN10.3 playback and TV-show browse retained.' \
  > "$ART/CLEAN12.1B-NOTES.txt"
