#!/usr/bin/env bash
set -euo pipefail

O_APK=/tmp/Projectivy-CLEAN-13-1O-Weather-Checkbox-Routing-Fix.apk
UNSIGNED=/tmp/unsigned-clean13-1p.apk
ALIGNED=/tmp/aligned-clean13-1p.apk
FINAL=/tmp/Projectivy-CLEAN-13-1P-Show-Date-Native-Checkbox.apk
ART=/tmp/clean13-1p-artifact
BRIDGE=.github/clean13d/StatusBarCustomizationBridge.java
WEATHER=.github/clean13_1/WeatherStatusBarBridge.java

# First produce the exact runtime-approved O build without modifying its source.
bash .github/clean13_1o/build-clean13-1o.sh
test -s "$O_APK"

# Then patch the Clock/Date bridge plus the manual Weather refresh handler; rebuild classes4.dex.
python3 .github/clean13_1p/fix-clean13-1p.py

grep -Fq 'CLEAN13.1O native checkbox built builder=hv1 action=iv1 checked=' "$WEATHER"
grep -Fq 'CLEAN13.1O show weather checked=' "$WEATHER"
grep -Fq 'CLEAN13.1P manual refresh button clicked' "$WEATHER"
grep -Fq 'CLEAN13.1P manual refresh dispatched context=' "$WEATHER"
grep -Fq 'CLEAN13.1P refresh completed success=' "$WEATHER"
grep -Fq 'startRefresh(context, fragment, a, true)' "$WEATHER"
grep -Fq 'renderLastHome();' "$WEATHER"
grep -Fq 'refreshActiveWeatherPage();' "$WEATHER"
grep -Fq 'CLEAN13.1P native checkbox built builder=hv1 action=iv1 checked=' "$BRIDGE"
grep -Fq 'CLEAN13.1P Show date native action=' "$BRIDGE"
grep -Fq 'CLEAN13.1P Show date checked=' "$BRIDGE"
! grep -Fq 'getDeclaredConstructor(Context.class)' "$BRIDGE"
! grep -Fq 'Class.forName("hv1")' "$BRIDGE"

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"

rm -rf /tmp/embedded131p-classes /tmp/embedded131p-dex
mkdir -p /tmp/embedded131p-classes /tmp/embedded131p-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded131p-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java \
  "$BRIDGE" \
  "$WEATHER"

"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded131p-dex $(find /tmp/embedded131p-classes -name '*.class' -type f | sort)
cp /tmp/embedded131p-dex/classes.dex /tmp/classes4-clean13-1p.dex
test -s /tmp/classes4-clean13-1p.dex

for marker in \
  'CLEAN13.1I status order watcher attached' \
  'CLEAN13.1G Weather parent uses native Projectivy cloud icon' \
  'CLEAN13.1O native checkbox built builder=hv1 action=iv1 checked=' \
  'CLEAN13.1O show weather checked=' \
  'CLEAN13.1P manual refresh button clicked' \
  'CLEAN13.1P refresh start force=' \
  'CLEAN13.1P refresh completed success=' \
  'refreshActiveWeatherPage' \\
  'renderLastHome' \\
  'Updating...' \
  'CLEAN13.1P native checkbox built builder=hv1 action=iv1 checked=' \
  'CLEAN13.1P Show date native action=' \
  'CLEAN13.1P Show date checked=' \
  'CLEAN13D2 native clock/date actions added; unified default'; do
  strings /tmp/classes4-clean13-1p.dex | grep -Fq "$marker"
done

# Replace only classes4.dex in the approved O APK.
python3 - "$O_APK" "$UNSIGNED" <<'PY'
import sys, zipfile
base, out = sys.argv[1:]
replacement = open('/tmp/classes4-clean13-1p.dex', 'rb').read()
with zipfile.ZipFile(base) as zin, zipfile.ZipFile(out, 'w') as zout:
    for info in zin.infolist():
        if info.filename.startswith('META-INF/'):
            continue
        data = replacement if info.filename == 'classes4.dex' else zin.read(info.filename)
        item = zipfile.ZipInfo(info.filename, info.date_time)
        item.compress_type = info.compress_type
        item.comment = info.comment
        item.extra = info.extra
        item.internal_attr = info.internal_attr
        item.external_attr = info.external_attr
        item.create_system = info.create_system
        item.create_version = info.create_version
        item.extract_version = info.extract_version
        item.flag_bits = info.flag_bits
        zout.writestr(item, data)
PY
unzip -t "$UNSIGNED" >/dev/null

python3 - "$O_APK" "$UNSIGNED" <<'PY'
import sys, zipfile
base, candidate = sys.argv[1:]
def members(path):
    with zipfile.ZipFile(path) as z:
        return {i.filename: z.read(i.filename) for i in z.infolist()
                if not i.filename.startswith('META-INF/')}
old = members(base)
new = members(candidate)
assert set(old) == set(new), 'archive member set changed'
changed = sorted(k for k in old if old[k] != new[k])
assert changed == ['classes4.dex'], changed
for token in [
    b'CLEAN13.1O native checkbox built builder=hv1 action=iv1 checked=',
    b'CLEAN13.1O show weather checked=',
    b'CLEAN13.1P manual refresh button clicked',
    b'CLEAN13.1P refresh completed success=',
    b'CLEAN13.1P native checkbox built builder=hv1 action=iv1 checked=',
    b'CLEAN13.1P Show date native action=',
    b'CLEAN13.1P Show date checked=',
]:
    assert token in new['classes4.dex'], token
for forbidden in [b'getDeclaredConstructor(Context.class)', b'Class.forName("hv1")']:
    assert forbidden not in new['classes4.dex'], forbidden
print('PASS: CLEAN13.1P scope exact: classes4.dex only vs runtime-approved O')
PY

"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" "$ALIGNED"
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" "$ALIGNED"

"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean13-1p.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean13-1p.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean13-1p.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean13-1p.txt

rm -rf "$ART" && mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean13-1p.txt /tmp/apksigner-clean13-1p.txt /tmp/zipalign-clean13-1p.txt /tmp/package-clean13-1p.txt "$ART/"
printf '%s\n' \
  'CLEAN13.1P Show Date native checkbox candidate' \
  '- Built on top of the user-approved, runtime-tested CLEAN13.1O.' \
  '- CLEAN13.1O Show Weather checkbox, Weather page, native cloud icon/background and status order are preserved.' \
  '- Show Date uses the same DEX-audited Projectivy builder path: cb3.q0(yh3) -> hv1 -> cb3.z0(lu1) -> iv1.' \
  '- The Show Date action continues to save the checked state and refresh the native clock/date format.' \
  '- Manual Weather refresh now starts from the page Context, bypasses the Activity-only gate, and updates the last-updated row on completion.' \
  '- Manual refresh is forced even when Show Weather is off; automatic Home refresh retains its visibility and 30-minute cache rules.' \
  '- Existing Home and active Weather-page refresh callbacks remain in place.' \\
  '- Existing Clock format, Date format and Clock & date background options are retained.' \
  '- Only classes4.dex differs from CLEAN13.1O before signing.' \
  '- Runtime testing on the Shield is still required before freezing P.' \
  > "$ART/CLEAN13-1P-NOTES.txt"

cd /tmp
rm -f clean13-1p-artifact.zip
zip -qr clean13-1p-artifact.zip clean13-1p-artifact
echo "FINAL_APK=$FINAL"
echo "ARTIFACT_ZIP=/tmp/clean13-1p-artifact.zip"
