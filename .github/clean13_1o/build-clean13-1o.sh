#!/usr/bin/env bash
set -euo pipefail

I_APK=/tmp/Projectivy-CLEAN-13-1I-Weather-Order-Only.apk
UNSIGNED=/tmp/unsigned-clean13-1o.apk
ALIGNED=/tmp/aligned-clean13-1o.apk
FINAL=/tmp/Projectivy-CLEAN-13-1O-Weather-Checkbox-Routing-Fix.apk
ART=/tmp/clean13-1o-artifact
BRIDGE=.github/clean13_1/WeatherStatusBarBridge.java

# Start from the exact user-approved I visual/order baseline. No L/N patch is inherited.
bash .github/clean13_1i/build-clean13-1i.sh
test -s "$I_APK"

python3 .github/clean13_1o/fix-clean13-1o.py

grep -Fq 'CLEAN13.1I status order watcher attached' "$BRIDGE"
grep -Fq 'CLEAN13.1G Weather parent uses native Projectivy cloud icon' "$BRIDGE"
grep -Fq 'CLEAN13.1O native checkbox built builder=hv1 action=iv1 checked=' "$BRIDGE"
grep -Fq 'CLEAN13.1O page callback id=' "$BRIDGE"
grep -Fq 'CLEAN13.1O parent callback id=' "$BRIDGE"
grep -Fq 'CLEAN13.1O show weather checked=' "$BRIDGE"
! grep -Fq 'getDeclaredConstructor(Context.class)' "$BRIDGE"
! grep -Fq 'Class.forName("hv1")' "$BRIDGE"
! grep -Fq 'findMethod(fragment.getClass(), "q0", preferenceClass)' "$BRIDGE"

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"

rm -rf /tmp/embedded131o-classes /tmp/embedded131o-dex
mkdir -p /tmp/embedded131o-classes /tmp/embedded131o-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded131o-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java \
  .github/clean13d/StatusBarCustomizationBridge.java \
  "$BRIDGE"

mapfile -t CLASSFILES < <(find /tmp/embedded131o-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded131o-dex "${CLASSFILES[@]}"
cp /tmp/embedded131o-dex/classes.dex /tmp/classes4-clean13-1o.dex
test -s /tmp/classes4-clean13-1o.dex

for marker in \
  'CLEAN13.1I status order watcher attached' \
  'CLEAN13.1I correcting status order after native pass' \
  'CLEAN13.1G Weather parent uses native Projectivy cloud icon' \
  'CLEAN13.1G Weather page populated after native init' \
  'CLEAN13.1O native checkbox built builder=hv1 action=iv1 checked=' \
  'CLEAN13.1O page callback id=' \
  'CLEAN13.1O parent callback id=' \
  'CLEAN13.1O show weather checked=' \
  'api.postcodes.io/postcodes/' \
  'api.open-meteo.com/v1/forecast' \
  'CLEAN13D2 native clock/date actions added; unified default' \
  'CLEAN12.1 SUCCESS channels='; do
  strings /tmp/classes4-clean13-1o.dex | grep -Fq "$marker"
done

# Repackage directly from I and replace classes4 only.
python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-13-1I-Weather-Order-Only.apk'
out='/tmp/unsigned-clean13-1o.apk'
replacement=open('/tmp/classes4-clean13-1o.dex','rb').read()
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
    b'CLEAN13.1I status order watcher attached',
    b'CLEAN13.1G Weather parent uses native Projectivy cloud icon',
    b'CLEAN13.1O native checkbox built builder=hv1 action=iv1 checked=',
    b'CLEAN13.1O page callback id=',
    b'CLEAN13.1O parent callback id=',
]:
    assert token in c['classes4.dex'], token
for forbidden in [
    b'getDeclaredConstructor(Context.class)',
    b'Class.forName("hv1")',
    b'CLEAN13.1N',
    b'CLEAN13.1L route action id=',
]:
    assert forbidden not in c['classes4.dex'], forbidden
print('PASS: CLEAN13.1O scope exact: classes4.dex only vs I')
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

"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean13-1o.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean13-1o.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean13-1o.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean13-1o.txt

rm -rf "$ART" && mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean13-1o.txt /tmp/apksigner-clean13-1o.txt /tmp/zipalign-clean13-1o.txt /tmp/package-clean13-1o.txt "$ART/"
printf '%s\n' \
  'CLEAN13.1O Weather native checkbox candidate' \
  '- Built directly from the approved CLEAN13.1I APK; no H/J/K/L/M/N patch is inherited.' \
  '- CLEAN13.1G Weather category, native cloud icon/background and CLEAN13.1I status-bar order are preserved.' \
  '- Uses the DEX-verified Projectivy path: cb3.q0(yh3) creates hv1; cb3.z0(lu1) returns iv1.' \
  '- Does not instantiate hv1 directly; hv1 has no Context constructor.' \
  '- Routes Weather action IDs through both pg and qg callbacks, without requiring the old fragment-identity check.' \
  '- Adds runtime markers for builder creation, callback entry, checked state and save result.' \
  '- Only classes4.dex differs from I before signing.' \
  '- Candidate build verification is not a substitute for Shield runtime testing.' \
  > "$ART/CLEAN13-1O-NOTES.txt"

cd /tmp
zip -qr clean13-1o-artifact.zip clean13-1o-artifact
echo "FINAL_APK=$FINAL"
echo "ARTIFACT_ZIP=/tmp/clean13-1o-artifact.zip"
