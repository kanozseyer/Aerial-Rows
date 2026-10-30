#!/usr/bin/env bash
set -euo pipefail

I_APK=/tmp/Projectivy-CLEAN-13-1I-Weather-Order-Only.apk
UNSIGNED=/tmp/unsigned-clean13-1n.apk
ALIGNED=/tmp/aligned-clean13-1n.apk
FINAL=/tmp/Projectivy-CLEAN-13-1N-Weather-Native-Checkbox-Factory.apk
ART=/tmp/clean13-1n-artifact

# Rebuild the approved I baseline. This bypasses H/J/K/L/M entirely.
bash .github/clean13_1i/build-clean13-1i.sh
test -s "$I_APK"

python3 .github/clean13_1n/fix-clean13-1n.py
BRIDGE=.github/clean13_1/WeatherStatusBarBridge.java

grep -Fq 'CLEAN13.1N native checkbox built builder=' "$BRIDGE"
grep -Fq 'CLEAN13.1N native checkbox failed; using G checked-row fallback' "$BRIDGE"
grep -Fq 'CLEAN13.1N show weather checkbox=' "$BRIDGE"
grep -Fq 'findMethod(fragment.getClass(), "q0", preferenceClass)' "$BRIDGE"
grep -Fq 'CLEAN13.1I status order watcher attached' "$BRIDGE"
grep -Fq 'CLEAN13.1G Weather parent uses native Projectivy cloud icon' "$BRIDGE"
! grep -Fq 'getDeclaredConstructor(Context.class)' "$BRIDGE"
! grep -Fq 'Class.forName("hv1")' "$BRIDGE"
! grep -Fq 'CLEAN13.1H Weather ordered between status icons and date/clock' "$BRIDGE"
! grep -Fq 'CLEAN13.1J Show weather selector=' "$BRIDGE"
! grep -Fq 'CLEAN13.1K show weather choice=' "$BRIDGE"
! grep -Fq 'CLEAN13.1L route action id=' "$BRIDGE"

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"

rm -rf /tmp/embedded131n-classes /tmp/embedded131n-dex
mkdir -p /tmp/embedded131n-classes /tmp/embedded131n-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded131n-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java \
  .github/clean13d/StatusBarCustomizationBridge.java \
  "$BRIDGE"

mapfile -t CLASSFILES < <(find /tmp/embedded131n-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded131n-dex "${CLASSFILES[@]}"
cp /tmp/embedded131n-dex/classes.dex /tmp/classes4-clean13-1n.dex
test -s /tmp/classes4-clean13-1n.dex

strings /tmp/classes4-clean13-1n.dex > /tmp/classes4-clean13-1n.strings
for marker in \
  'CLEAN13.1N native checkbox built builder=' \
  'CLEAN13.1N native checkbox failed; using G checked-row fallback' \
  'CLEAN13.1N Show weather action=' \
  'CLEAN13.1N show weather checkbox=' \
  'CLEAN13.1I status order watcher attached' \
  'CLEAN13.1I status geometry phase=' \
  'CLEAN13.1G Weather parent uses native Projectivy cloud icon' \
  'CLEAN13D2 native clock/date actions added; unified default' \
  'api.postcodes.io/postcodes/' \
  'api.open-meteo.com/v1/forecast'; do
  grep -Fq "$marker" /tmp/classes4-clean13-1n.strings || {
    echo "MISSING CLEAN13.1N DEX MARKER: $marker"
    exit 1
  }
  echo "PASS DEX MARKER: $marker"
done

# Repackage directly from I; only classes4 may differ before signing.
python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-13-1I-Weather-Order-Only.apk'
out='/tmp/unsigned-clean13-1n.apk'
replacement=open('/tmp/classes4-clean13-1n.dex','rb').read()
with zipfile.ZipFile(base) as zin, zipfile.ZipFile(out,'w') as zout:
    for i in zin.infolist():
        if i.filename.startswith('META-INF/'):
            continue
        data=replacement if i.filename=='classes4.dex' else zin.read(i.filename)
        n=zipfile.ZipInfo(i.filename,i.date_time)
        n.compress_type=i.compress_type; n.comment=i.comment; n.extra=i.extra
        n.internal_attr=i.internal_attr; n.external_attr=i.external_attr
        n.create_system=i.create_system; n.create_version=i.create_version
        n.extract_version=i.extract_version; n.flag_bits=i.flag_bits
        zout.writestr(n,data)
PY
unzip -t "$UNSIGNED" >/dev/null

python3 - "$I_APK" "$UNSIGNED" <<'PY'
import sys,zipfile,hashlib
base,cand=sys.argv[1:]
def members(path):
    with zipfile.ZipFile(path) as z:
        return {i.filename:z.read(i.filename) for i in z.infolist() if not i.filename.startswith('META-INF/')}
b=members(base); c=members(cand)
assert set(b)==set(c), 'archive member set changed'
changed=sorted(k for k in b if b[k]!=c[k])
assert changed==['classes4.dex'], changed
for token in [
    b'CLEAN13.1N native checkbox built builder=',
    b'CLEAN13.1N native checkbox failed; using G checked-row fallback',
    b'CLEAN13.1N show weather checkbox=',
    b'CLEAN13.1I status order watcher attached',
    b'CLEAN13.1G Weather parent uses native Projectivy cloud icon',
]:
    assert token in c['classes4.dex'], token
for forbidden in [
    b'CLEAN13.1H Weather ordered between status icons and date/clock',
    b'CLEAN13.1J Show weather selector=',
    b'CLEAN13.1K show weather choice=',
    b'CLEAN13.1L route action id=',
    b'getDeclaredConstructor(Context.class)',
]:
    assert forbidden not in c['classes4.dex'], forbidden
print('PASS: CLEAN13.1N scope exact: classes4.dex only vs I')
print('classes4 sha256', hashlib.sha256(c['classes4.dex']).hexdigest())
PY

"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" "$ALIGNED"
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" "$ALIGNED"

"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean13-1n.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean13-1n.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean13-1n.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean13-1n.txt

rm -rf "$ART" && mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean13-1n.txt /tmp/apksigner-clean13-1n.txt /tmp/zipalign-clean13-1n.txt /tmp/package-clean13-1n.txt "$ART/"
printf '%s\n' \
  'CLEAN13.1N Projectivy native checkbox factory' \
  '- Built directly from approved CLEAN13.1I; H/J/K/L/M are not inherited.' \
  '- G native Weather category/cloud icon/background and I status order are preserved.' \
  '- Uses Projectivy cb3.q0(yh3) to create the native Boolean hv1 builder internally.' \
  '- No reflection attempt to find hv1(Context); Projectivy invokes lu1(Context) in its own DEX.' \
  '- Sets Weather action ID, label and saved checked state after native builder creation.' \
  '- If native factory invocation fails, falls back to the known-visible G checked row so Weather settings never go blank.' \
  '- Only classes4.dex differs from I before signing.' \
  > "$ART/CLEAN13-1N-NOTES.txt"

echo "FINAL_APK=$FINAL"
