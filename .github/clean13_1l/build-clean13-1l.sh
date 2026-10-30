#!/usr/bin/env bash
set -euo pipefail
# CLEAN13.1L CI trigger after workflow registration.

J_APK=/tmp/Projectivy-CLEAN-13-1J-Weather-Selector.apk
UNSIGNED=/tmp/unsigned-clean13-1l.apk
ALIGNED=/tmp/aligned-clean13-1l.apk
FINAL=/tmp/Projectivy-CLEAN-13-1L-Weather-Routing-Fix.apk
ART=/tmp/clean13-1l-artifact

# Rebuild J from its frozen I/G ancestry. This deliberately does NOT inherit K,
# so the rejected global monochrome icon patch is absent.
bash .github/clean13_1j/build-clean13-1j.sh
test -f "$J_APK"

python3 .github/clean13_1l/fix-clean13-1l.py
BRIDGE=.github/clean13_1/WeatherStatusBarBridge.java

grep -Fq 'CLEAN13.1L route action id=' "$BRIDGE"
grep -Fq 'CLEAN13.1L show weather choice=' "$BRIDGE"
grep -Fq 'CLEAN13.1J Show weather selector=' "$BRIDGE"
grep -Fq 'CLEAN13.1I status order watcher attached' "$BRIDGE"
grep -Fq 'CLEAN13.1G Weather parent uses native Projectivy cloud icon' "$BRIDGE"
! grep -Fq 'Class.forName("hv1")' "$BRIDGE"

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"

rm -rf /tmp/embedded131l-classes /tmp/embedded131l-dex
mkdir -p /tmp/embedded131l-classes /tmp/embedded131l-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded131l-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java \
  .github/clean13d/StatusBarCustomizationBridge.java \
  "$BRIDGE"

mapfile -t CLASSFILES < <(find /tmp/embedded131l-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded131l-dex "${CLASSFILES[@]}"
cp /tmp/embedded131l-dex/classes.dex /tmp/classes4-clean13-1l.dex
test -s /tmp/classes4-clean13-1l.dex

for marker in \
  'CLEAN13.1L route action id=' \
  'CLEAN13.1L show weather choice=' \
  'CLEAN13.1J Show weather selector=' \
  'CLEAN13.1I status order watcher attached' \
  'CLEAN13.1I status geometry phase=' \
  'CLEAN13.1G Weather parent uses native Projectivy cloud icon' \
  'api.postcodes.io/postcodes/' \
  'api.open-meteo.com/v1/forecast'; do
  strings /tmp/classes4-clean13-1l.dex | grep -Fq "$marker"
done

# Repackage directly from J; only classes4 may differ before signing.
python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-13-1J-Weather-Selector.apk'
out='/tmp/unsigned-clean13-1l.apk'
replacement=open('/tmp/classes4-clean13-1l.dex','rb').read()
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

python3 - "$J_APK" "$UNSIGNED" <<'PY'
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
    b'CLEAN13.1L route action id=',
    b'CLEAN13.1L show weather choice=',
    b'CLEAN13.1J Show weather selector=',
    b'CLEAN13.1I status order watcher attached',
    b'CLEAN13.1G Weather parent uses native Projectivy cloud icon',
]:
    assert token in c['classes4.dex'], token
for forbidden in [
    b'CLEAN13.1H native checkbox Show weather=',
    b'Class.forName("hv1")',
    b'CLEAN13.1K monochrome icon patch',
]:
    assert forbidden not in c['classes4.dex'], forbidden
print('PASS: CLEAN13.1L scope exact: classes4.dex only vs J')
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

"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean13-1l.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean13-1l.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean13-1l.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean13-1l.txt

rm -rf "$ART" && mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean13-1l.txt /tmp/apksigner-clean13-1l.txt /tmp/zipalign-clean13-1l.txt /tmp/package-clean13-1l.txt "$ART/"
printf '%s\n' \
  'CLEAN13.1L Weather routing fix' \
  '- Built from J/I/G; K global monochrome icon experiment is not inherited.' \
  '- Weather category icon/background and I status order remain unchanged.' \
  '- Show weather parent only opens the native selector.' \
  '- Weather-owned dropdown child actions are routed back to ACTIVE_WEATHER_PAGE when callback fragment differs.' \
  '- Runtime route marker logs action ID, callbackMarked and usingActivePage.' \
  '- Only classes4.dex differs from rebuilt J before signing.' \
  > "$ART/CLEAN13-1L-NOTES.txt"

echo "FINAL_APK=$FINAL"
