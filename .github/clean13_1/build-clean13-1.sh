#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-13D2-Date-Background-Fix.apk
UNSIGNED=/tmp/unsigned-clean13-1.apk
FINAL=/tmp/Projectivy-CLEAN-13-1-Weather.apk
ART=/tmp/clean13-1-artifact

# Rebuild the exact frozen CLEAN13D2 source stack first.
bash .github/clean13d/build-clean13d.sh
test -f "$BASE"

# CLEAN13.1 is a classes4-only weather extension of the frozen status bar.
python3 .github/clean13_1/prepare-clean13-1.py

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"

"$BT/aapt" dump permissions "$BASE" | grep -Fq 'android.permission.INTERNET'

rm -rf /tmp/embedded131-classes /tmp/embedded131-dex
mkdir -p /tmp/embedded131-classes /tmp/embedded131-dex

javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded131-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java \
  .github/clean13d/StatusBarCustomizationBridge.java \
  .github/clean13_1/WeatherStatusBarBridge.java

mapfile -t CLASSFILES < <(find /tmp/embedded131-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded131-dex "${CLASSFILES[@]}"
cp /tmp/embedded131-dex/classes.dex /tmp/classes4-clean13-1.dex

test -s /tmp/classes4-clean13-1.dex
for s in \
  'CLEAN13.1 weather actions added' \
  'Weather location' \
  'Open-Meteo' \
  'geocoding-api.open-meteo.com/v1/search' \
  'api.open-meteo.com/v1/forecast' \
  'CLEAN13D2 native clock/date actions added; unified default' \
  'CLEAN12.1 SUCCESS channels='; do
  strings /tmp/classes4-clean13-1.dex | grep -Fq "$s"
done

python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-13D2-Date-Background-Fix.apk'
out='/tmp/unsigned-clean13-1.apk'
replacement=open('/tmp/classes4-clean13-1.dex','rb').read()
seen=False
with zipfile.ZipFile(base) as zin, zipfile.ZipFile(out,'w') as zout:
    for i in zin.infolist():
        if i.filename.startswith('META-INF/'):
            continue
        data=zin.read(i.filename)
        if i.filename == 'classes4.dex':
            data=replacement
            seen=True
        n=zipfile.ZipInfo(i.filename,i.date_time)
        n.compress_type=i.compress_type; n.comment=i.comment; n.extra=i.extra
        n.internal_attr=i.internal_attr; n.external_attr=i.external_attr
        n.create_system=i.create_system; n.create_version=i.create_version
        n.extract_version=i.extract_version; n.flag_bits=i.flag_bits
        zout.writestr(n,data)
assert seen
PY

unzip -t "$UNSIGNED" >/dev/null
python3 .github/clean13_1/verify-clean13-1.py "$BASE" "$UNSIGNED"

"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" /tmp/aligned-clean13-1.apk
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" /tmp/aligned-clean13-1.apk

"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean13-1.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean13-1.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean13-1.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean13-1.txt

rm -rf "$ART" && mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean13-1.txt /tmp/apksigner-clean13-1.txt /tmp/zipalign-clean13-1.txt /tmp/package-clean13-1.txt "$ART/"
printf '%s\n' \
  'CLEAN13.1: cached native status-bar weather' \
  '- Based directly on frozen runtime-proven CLEAN13D2.' \
  '- classes4.dex only; classes2/native Status Bar menu hook remains frozen.' \
  '- Manual city/postcode entry only; no GPS/location permission.' \
  '- Open-Meteo geocoding + current temperature/weather_code/is_day.' \
  '- 30-minute refresh throttle; networking off the main thread.' \
  '- Last successful weather remains cached/offline-safe.' \
  '- Show weather On/Off, location, Celsius/Fahrenheit and Refresh weather controls.' \
  '- Monochrome custom weather icons; no external icon assets.' \
  '- Weather data attribution visible in settings: Open-Meteo / CC BY 4.0.' \
  > "$ART/CLEAN13-1-NOTES.txt"
