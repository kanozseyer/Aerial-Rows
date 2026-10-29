#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-13D2-Date-Background-Fix.apk
UNSIGNED=/tmp/unsigned-clean13-1d.apk
FINAL=/tmp/Projectivy-CLEAN-13-1D-Weather-Controls-Fix.apk
ART=/tmp/clean13-1-artifact
APKTOOL=/tmp/apktool.jar

# Rebuild the exact frozen/runtime-proven CLEAN13D2 stack first.
bash .github/clean13d/build-clean13d.sh
test -f "$BASE"

# Keep Clock/date native page frozen; add Weather, then apply the proven
# menu/order/idle correction and the CLEAN13.1D controls/postcode fixes.
python3 .github/clean13_1/prepare-clean13-1.py
python3 .github/clean13_1/fix-weather-structure.py
python3 .github/clean13_1/fix-weather-controls.py

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"

"$BT/aapt" dump permissions "$BASE" | grep -Fq 'android.permission.INTERNET'

# classes4: embedded bridge only.
rm -rf /tmp/embedded131d-classes /tmp/embedded131d-dex
mkdir -p /tmp/embedded131d-classes /tmp/embedded131d-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded131d-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java \
  .github/clean13d/StatusBarCustomizationBridge.java \
  .github/clean13_1/WeatherStatusBarBridge.java

mapfile -t CLASSFILES < <(find /tmp/embedded131d-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded131d-dex "${CLASSFILES[@]}"
cp /tmp/embedded131d-dex/classes.dex /tmp/classes4-clean13-1d.dex

test -s /tmp/classes4-clean13-1d.dex
for s in \
  'CLEAN13.1B Weather sibling added to Status Bar' \
  'CLEAN13.1D Weather native page populated' \
  'CLEAN13.1B location dialog opened' \
  'CLEAN13.1B status order=settings/status icons > weather > date > clock' \
  'CLEAN13.1B idle pill iconsCollapsed=' \
  'CLEAN13.1D show weather=' \
  'CLEAN13.1D manual refresh requested' \
  'CLEAN13.1D geocoded via' \
  'Weather location' \
  'WeatherMenuDrawable' \
  'Open-Meteo' \
  'api.postcodes.io/postcodes/' \
  'geocoding-api.open-meteo.com/v1/search' \
  'api.open-meteo.com/v1/forecast' \
  'CLEAN13D2 native clock/date actions added; unified default' \
  'CLEAN12.1 SUCCESS channels='; do
  strings /tmp/classes4-clean13-1d.dex | grep -Fq "$s"
done

# classes2: patch only native qg Status Bar parent + pg Clock shell. Use no-res
# decode so Projectivy resources remain byte-for-byte opaque.
if [ ! -f "$APKTOOL" ]; then
  curl -fL --retry 3 'https://github.com/iBotPeaches/Apktool/releases/download/v2.11.1/apktool_2.11.1.jar' -o "$APKTOOL"
fi
rm -rf /tmp/pj131d /tmp/rebuilt131d.apk
java -jar "$APKTOOL" d -r -f "$BASE" -o /tmp/pj131d >/tmp/decode-clean13-1d.log 2>&1
python3 .github/clean13_1/patch-weather-menu-smali.py /tmp/pj131d

QG="$(find /tmp/pj131d -path '*/qg.smali' -print -quit)"
PG="$(find /tmp/pj131d -path '*/pg.smali' -print -quit)"
test -n "$QG" && test -n "$PG"
grep -Fq 'addWeatherParentAction(Ljava/lang/Object;Ljava/util/List;)V' "$QG"
grep -Fq 'handleWeatherParentAction(Ljava/lang/Object;Ljava/lang/Object;)Z' "$QG"
grep -Fq 'populateWeatherPage(Ljava/lang/Object;Ljava/util/List;)Z' "$PG"
grep -Fq 'handleWeatherPageAction(Ljava/lang/Object;Ljava/lang/Object;)Z' "$PG"

if ! java -jar "$APKTOOL" b /tmp/pj131d -o /tmp/rebuilt131d.apk >/tmp/rebuild-clean13-1d.log 2>&1; then
  echo '===== CLEAN13.1D APKTOOL REBUILD FAILURE ====='
  cat /tmp/rebuild-clean13-1d.log
  echo '===== qg Weather hooks ====='
  grep -n -A30 -B10 'WeatherStatusBarBridge' "$QG" || true
  echo '===== pg Weather hooks ====='
  grep -n -A30 -B10 'WeatherStatusBarBridge' "$PG" || true
  exit 1
fi
unzip -p /tmp/rebuilt131d.apk classes2.dex > /tmp/classes2-clean13-1d.dex
test -s /tmp/classes2-clean13-1d.dex

# Repackage strictly from frozen CLEAN13D2, replacing only classes2 + classes4.
python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-13D2-Date-Background-Fix.apk'
out='/tmp/unsigned-clean13-1d.apk'
repl={
 'classes2.dex':open('/tmp/classes2-clean13-1d.dex','rb').read(),
 'classes4.dex':open('/tmp/classes4-clean13-1d.dex','rb').read(),
}
seen=set()
with zipfile.ZipFile(base) as zin, zipfile.ZipFile(out,'w') as zout:
    for i in zin.infolist():
        if i.filename.startswith('META-INF/'):
            continue
        data=repl.get(i.filename, zin.read(i.filename))
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
python3 .github/clean13_1/verify-clean13-1.py "$BASE" "$UNSIGNED"

"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" /tmp/aligned-clean13-1d.apk
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" /tmp/aligned-clean13-1d.apk

"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean13-1d.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean13-1d.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean13-1d.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean13-1d.txt

rm -rf "$ART" && mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean13-1d.txt /tmp/apksigner-clean13-1d.txt /tmp/zipalign-clean13-1d.txt /tmp/package-clean13-1d.txt "$ART/"
printf '%s\n' \
  'CLEAN13.1D: Weather controls/postcode correction' \
  '- Based directly on frozen runtime-proven CLEAN13D2.' \
  '- Retains runtime-proven Status Bar order and adaptive idle pill from CLEAN13.1C.' \
  '- Weather parent row has a monochrome weather icon and no location/postcode subtitle.' \
  '- UK postcodes resolve through Postcodes.io first; place names continue through Open-Meteo geocoding.' \
  '- Show weather is a direct On/Off action and immediately re-renders Home.' \
  '- Manual Refresh updates Home and refreshes the open Weather page after background completion.' \
  '- Manual city/postcode only; no GPS/location permission.' \
  '- Cached Open-Meteo data, 30-minute refresh, offline-safe last successful result retained.' \
  '- Only classes2.dex and classes4.dex differ from CLEAN13D2 before signing.' \
  > "$ART/CLEAN13-1D-NOTES.txt"
