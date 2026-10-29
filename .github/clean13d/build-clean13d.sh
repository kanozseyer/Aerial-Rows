#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-13C-Native-Clock-Date.apk
UNSIGNED=/tmp/unsigned-clean13d2.apk
FINAL=/tmp/Projectivy-CLEAN-13D2-Date-Background-Fix.apk
ART=/tmp/clean13d-artifact
APKTOOL=/tmp/apktool.jar

# Build the exact runtime-proven CLEAN13C base first. This also leaves the
# prepared embedded Java sources in the CI workspace for CLEAN13D to extend.
bash .github/clean13c/build-clean13c.sh
test -f "$BASE"
python3 .github/clean13d/prepare-clean13d.py
python3 .github/clean13d/fix-bridge-compile.py
python3 .github/clean13d/fix-runtime-feedback.py

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"

# Recompile embedded classes4 with the corrected CLEAN13D2 bridge.
rm -rf /tmp/embedded13d-classes /tmp/embedded13d-dex
mkdir -p /tmp/embedded13d-classes /tmp/embedded13d-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded13d-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java \
  .github/clean13d/StatusBarCustomizationBridge.java
mapfile -t CLASSFILES < <(find /tmp/embedded13d-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded13d-dex "${CLASSFILES[@]}"
cp /tmp/embedded13d-dex/classes.dex /tmp/classes4-clean13d.dex

for s in \
  'CLEAN13D2 native clock/date actions added; unified default' \
  'Show date' \
  'Date format' \
  'Clock & date background' \
  'Combined with status icons' \
  'CLEAN13C native clock APPLIED before=' \
  'CLEAN12.1 SUCCESS channels=' \
  'CLEAN10.3 browse clean uri='; do
  strings /tmp/classes4-clean13d.dex | grep -Fq "$s"
done

# Patch only pg.smali in classes2. Decode with -r/--no-res so Projectivy's
# resources.arsc remains opaque and apktool never attempts to relink Material3
# resources we did not touch.
if [ ! -f "$APKTOOL" ]; then
  curl -fL --retry 3 'https://github.com/iBotPeaches/Apktool/releases/download/v2.11.1/apktool_2.11.1.jar' -o "$APKTOOL"
fi
rm -rf /tmp/pj13d /tmp/rebuilt13d.apk
java -jar "$APKTOOL" d -r -f "$BASE" -o /tmp/pj13d >/tmp/decode-clean13d.log 2>&1
PG="$(find /tmp/pj13d -path '*/pg.smali' -print -quit)"
test -n "$PG"
cp "$PG" /tmp/pg-before-clean13d.smali
python3 .github/clean13d/patch-pg-smali.py /tmp/pj13d
cmp -s "$PG" /tmp/pg-before-clean13d.smali && { echo 'pg.smali did not change'; exit 1; }

grep -Fq 'addClockDateActions(Ljava/lang/Object;Ljava/util/List;)V' "$PG"
grep -Fq 'handleClockDateAction(Ljava/lang/Object;Ljava/lang/Object;)Z' "$PG"

if ! java -jar "$APKTOOL" b /tmp/pj13d -o /tmp/rebuilt13d.apk >/tmp/rebuild-clean13d.log 2>&1; then
  echo '===== CLEAN13D2 APKTOOL REBUILD FAILURE ====='
  cat /tmp/rebuild-clean13d.log
  echo '===== PATCHED pg.smali METHODS ====='
  grep -n -A35 -B8 'StatusBarCustomizationBridge' "$PG" || true
  exit 1
fi
unzip -p /tmp/rebuilt13d.apk classes2.dex > /tmp/classes2-clean13d.dex
test -s /tmp/classes2-clean13d.dex

# Repackage strictly from CLEAN13C, replacing only classes2 + classes4.
python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-13C-Native-Clock-Date.apk'
out='/tmp/unsigned-clean13d2.apk'
repl={
 'classes2.dex':open('/tmp/classes2-clean13d.dex','rb').read(),
 'classes4.dex':open('/tmp/classes4-clean13d.dex','rb').read(),
}
seen=set()
with zipfile.ZipFile(base) as zin, zipfile.ZipFile(out,'w') as zout:
    for i in zin.infolist():
        if i.filename.startswith('META-INF/'):
            continue
        data=repl.get(i.filename,zin.read(i.filename))
        n=zipfile.ZipInfo(i.filename,i.date_time)
        n.compress_type=i.compress_type; n.comment=i.comment; n.extra=i.extra
        n.internal_attr=i.internal_attr; n.external_attr=i.external_attr
        n.create_system=i.create_system; n.create_version=i.create_version
        n.extract_version=i.extract_version; n.flag_bits=i.flag_bits
        zout.writestr(n,data)
        seen.add(i.filename)
assert set(repl).issubset(seen)
PY
unzip -t "$UNSIGNED" >/dev/null
python3 .github/clean13d/verify-clean13d.py "$BASE" "$UNSIGNED"

"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" /tmp/aligned-clean13d2.apk
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" /tmp/aligned-clean13d2.apk
"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean13d.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean13d.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean13d.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean13d.txt

rm -rf "$ART" && mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean13d.txt /tmp/apksigner-clean13d.txt /tmp/zipalign-clean13d.txt /tmp/package-clean13d.txt "$ART/"
printf '%s\n' \
  'CLEAN13D2: runtime feedback correction for native Clock/Date UI' \
  '- Based directly on runtime-proven CLEAN13C.' \
  '- Fixes menu construction stopping after Clock format.' \
  '- Show date is now a native On/Off dropdown using the already-proven dropdown path.' \
  '- Date format dropdown is reachable.' \
  '- Clock & date background dropdown is reachable.' \
  '- Default visual mode is Combined with status icons: one continuous Projectivy-style pill.' \
  '- Background colour continues to inherit the native status-icon background/tint.' \
  '- Other visual modes remain selectable: None / Separate / Combined.' \
  '- Existing raw Line 1 / Line 2 advanced controls remain untouched.' \
  '- Only classes2.dex and classes4.dex differ from CLEAN13C before signing.' \
  > "$ART/CLEAN13D2-NOTES.txt"
