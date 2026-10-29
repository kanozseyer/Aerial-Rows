#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-12-1-Differential-RedLight-Programs.apk
UNSIGNED=/tmp/unsigned-clean121a.apk
FINAL=/tmp/Projectivy-CLEAN-12-1A-Diagnostic-Mismatch.apk
ART=/tmp/clean121a-artifact

# Build and verify the full proven CLEAN12.1 stack first.
bash .github/clean121/build-clean121.sh
test -f "$BASE"

# Add diagnostics only after CLEAN12.1 source is fully prepared.
python3 .github/clean121a/prepare-clean121a.py

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
rm -rf /tmp/embedded121a-classes /tmp/embedded121a-dex
mkdir -p /tmp/embedded121a-classes /tmp/embedded121a-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded121a-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java
mapfile -t CLASSFILES < <(find /tmp/embedded121a-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded121a-dex "${CLASSFILES[@]}"
cp /tmp/embedded121a-dex/classes.dex /tmp/classes4-clean121a.dex

python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-12-1-Differential-RedLight-Programs.apk'
out='/tmp/unsigned-clean121a.apk'
replacement=open('/tmp/classes4-clean121a.dex','rb').read()
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
python3 .github/clean121a/verify-clean121a.py "$UNSIGNED"

"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" /tmp/aligned-clean121a.apk
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" /tmp/aligned-clean121a.apk
"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean121a.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean121a.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean121a.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean121a.txt

rm -rf "$ART"
mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean121a.txt "$ART/"
cp -f /tmp/apksigner-clean121a.txt "$ART/"
cp -f /tmp/zipalign-clean121a.txt "$ART/"
cp -f /tmp/package-clean121a.txt "$ART/"

printf '%s\n' \
  'CLEAN12.1a: diagnostic-only mismatch logging' \
  '- Functional behavior is CLEAN12.1.' \
  '- Logs only the first 8 PreviewProgram equality mismatches.' \
  '- Reports exact differing fields and old/new values.' \
  '- No destructive cleanup restored.' \
  '- CLEAN12 lifecycle, CLEAN11.1 retention, and CLEAN10.3 routing retained.' \
  > "$ART/CLEAN12.1A-NOTES.txt"
