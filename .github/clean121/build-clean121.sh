#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-10-3-Sanitized-TV-Browse-URI.apk
UNSIGNED=/tmp/unsigned-clean121.apk
FINAL=/tmp/Projectivy-CLEAN-12-1-Differential-RedLight-Programs.apk
ART=/tmp/clean121-artifact

# First build the entire proven upstream stack unchanged. Those historical build
# scripts patch the shared RedLightDiscoveryEngine source in a strict order.
bash .github/clean103/build-clean103.sh
test -f "$BASE"

# Only after CLEAN10.3 has completed do we apply the CLEAN12.1 programme-sync
# delta. Then recompile classes4.dex from the already-prepared source tree.
python3 .github/clean121/prepare-clean121.py

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
rm -rf /tmp/embedded121-classes /tmp/embedded121-dex
mkdir -p /tmp/embedded121-classes /tmp/embedded121-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded121-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java
mapfile -t CLASSFILES < <(find /tmp/embedded121-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded121-dex "${CLASSFILES[@]}"
cp /tmp/embedded121-dex/classes.dex /tmp/classes4-clean121.dex

for s in \
  'CLEAN12.1 SUCCESS channels=' \
  'CLEAN12.1 CHANNEL key=' \
  'CLEAN12 refresh throttled ageMs=' \
  'CLEAN12 Kodi native not ready - cached rows retained; no provider lease' \
  'CLEAN10.3 browse clean uri=' \
  'GUI.ActivateWindow' \
  'CLEAN09 forwarding kind='; do
  strings /tmp/classes4-clean121.dex | grep -q "$s"
done
if strings /tmp/classes4-clean121.dex | grep -q 'CLEAN07 PROGRAM CLEANUP channelId='; then
  echo 'ERROR: destructive CLEAN07 programme cleanup marker survived CLEAN12.1' >&2
  exit 1
fi

python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-10-3-Sanitized-TV-Browse-URI.apk'
out='/tmp/unsigned-clean121.apk'
replacement=open('/tmp/classes4-clean121.dex','rb').read()
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
python3 .github/clean121/verify-clean121.py "$UNSIGNED"

"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" /tmp/aligned-clean121.apk
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" /tmp/aligned-clean121.apk
"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean121.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean121.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean121.txt
"$BT/aapt" dump xmltree "$FINAL" AndroidManifest.xml > /tmp/manifest-clean121.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean121.txt

rm -rf "$ART"
mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean121.txt "$ART/"
cp -f /tmp/apksigner-clean121.txt "$ART/"
cp -f /tmp/zipalign-clean121.txt "$ART/"
cp -f /tmp/package-clean121.txt "$ART/"
cp -f /tmp/manifest-clean121.txt "$ART/"

printf '%s\n' \
  'CLEAN12.1: differential Red Light PreviewProgram synchronization' \
  '- Frozen CLEAN12 5-minute refresh lifecycle retained.' \
  '- Frozen CLEAN11.1 Kodi retention lease retained.' \
  '- Frozen CLEAN11 cold-native safety gate retained.' \
  '- Existing cards are matched by stable internal_provider_id/plugin URI.' \
  '- Unchanged cards keep the same PreviewProgram row/ID.' \
  '- Changed cards are updated in place.' \
  '- New cards are inserted; only stale/duplicate cards are deleted.' \
  '- Existing artwork is preserved if metadata artwork lookup is temporarily unavailable.' \
  '- PreviewPrograms query failure leaves the cached row untouched.' \
  '- Frozen CLEAN10.3 movie/episode playback and TV-show browse retained.' \
  > "$ART/CLEAN12.1-NOTES.txt"
