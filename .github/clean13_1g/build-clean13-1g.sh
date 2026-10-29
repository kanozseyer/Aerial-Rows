#!/usr/bin/env bash
set -euo pipefail

LEGACY=75b8da5399a6dbd211764c9a1d6a531b71379964
BASE=/tmp/Projectivy-CLEAN-13D2-Date-Background-Fix.apk
UNSIGNED=/tmp/unsigned-clean13-1g.apk
ALIGNED=/tmp/aligned-clean13-1g.apk
FINAL=/tmp/Projectivy-CLEAN-13-1G-Weather-Native-UI.apk
ART=/tmp/clean13-1g-artifact
APKTOOL=/tmp/apktool.jar

# Always start from the frozen/runtime-proven CLEAN13D2 base.
bash .github/clean13d/build-clean13d.sh
test -f "$BASE"
BASE_SHA="$(sha256sum "$BASE" | awk '{print $1}')"
echo "CLEAN13 stable base sha256=$BASE_SHA"

# Recover only the last runtime-proven CLEAN13.1D Weather implementation from an
# immutable commit. G then applies exactly two corrections on top: post-init page
# population and native Projectivy icon/check rendering.
git fetch --no-tags --depth=1 origin "$LEGACY"
mkdir -p .github/clean13_1
for f in WeatherStatusBarBridge.java prepare-clean13-1.py fix-weather-structure.py fix-weather-controls.py; do
  git show "$LEGACY:.github/clean13_1/$f" > ".github/clean13_1/$f"
done

python3 .github/clean13_1/prepare-clean13-1.py
python3 .github/clean13_1/fix-weather-structure.py
python3 .github/clean13_1/fix-weather-controls.py
python3 .github/clean13_1g/fix-clean13-1g.py

BRIDGE=.github/clean13_1/WeatherStatusBarBridge.java
grep -Fq 'finishPopulateWeatherPage' "$BRIDGE"
grep -Fq 'getDeclaredField("m4")' "$BRIDGE"
grep -Fq 't0.invoke(null, fragment, id, title, nativeCloud, 16)' "$BRIDGE"
grep -Fq 'setChecked.invoke(action, checked)' "$BRIDGE"
! grep -Fq 'new WeatherMenuDrawable(context), 28' "$BRIDGE"
! grep -Fq '0xff424242' "$BRIDGE"
! grep -Fq 'hv1' "$BRIDGE"

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
"$BT/aapt" dump permissions "$BASE" | grep -Fq 'android.permission.INTERNET'

# classes4: frozen embedded stack + corrected Weather bridge.
rm -rf /tmp/embedded131g-classes /tmp/embedded131g-dex
mkdir -p /tmp/embedded131g-classes /tmp/embedded131g-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded131g-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java \
  .github/clean13d/StatusBarCustomizationBridge.java \
  "$BRIDGE"

mapfile -t CLASSFILES < <(find /tmp/embedded131g-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded131g-dex "${CLASSFILES[@]}"
cp /tmp/embedded131g-dex/classes.dex /tmp/classes4-clean13-1g.dex
test -s /tmp/classes4-clean13-1g.dex

for marker in \
  'CLEAN13.1G Weather page populated after native init' \
  'CLEAN13.1G Weather parent uses native Projectivy cloud icon' \
  'CLEAN13.1G native checked Show weather=' \
  'CLEAN13.1D show weather=' \
  'CLEAN13.1D geocoded via' \
  'api.postcodes.io/postcodes/' \
  'api.open-meteo.com/v1/forecast' \
  'CLEAN13D2 native clock/date actions added; unified default' \
  'CLEAN12.1 SUCCESS channels='; do
  strings /tmp/classes4-clean13-1g.dex | grep -Fq "$marker"
done

# classes2: qg Status Bar parent + pg shell only. Crucially, pg.e0 gets the
# Weather replacement hook at its final return, after Projectivy native init.
if [ ! -f "$APKTOOL" ]; then
  curl -fL --retry 3 'https://github.com/iBotPeaches/Apktool/releases/download/v2.11.1/apktool_2.11.1.jar' -o "$APKTOOL"
fi
rm -rf /tmp/pj131g /tmp/rebuilt131g.apk
java -jar "$APKTOOL" d -r -f "$BASE" -o /tmp/pj131g >/tmp/decode-clean13-1g.log 2>&1
python3 .github/clean13_1g/patch-weather-menu-native.py /tmp/pj131g

QG="$(find /tmp/pj131g -path '*/qg.smali' -print -quit)"
PG="$(find /tmp/pj131g -path '*/pg.smali' -print -quit)"
test -n "$QG" && test -n "$PG"
grep -Fq 'addWeatherParentAction(Ljava/lang/Object;Ljava/util/List;)V' "$QG"
grep -Fq 'handleWeatherParentAction(Ljava/lang/Object;Ljava/lang/Object;)Z' "$QG"
grep -Fq 'finishPopulateWeatherPage(Ljava/lang/Object;Ljava/util/List;)V' "$PG"
grep -Fq 'handleWeatherPageAction(Ljava/lang/Object;Ljava/lang/Object;)Z' "$PG"
! grep -Fq '->populateWeatherPage(Ljava/lang/Object;Ljava/util/List;)Z' "$PG"

if ! java -jar "$APKTOOL" b /tmp/pj131g -o /tmp/rebuilt131g.apk >/tmp/rebuild-clean13-1g.log 2>&1; then
  echo '===== CLEAN13.1G APKTOOL REBUILD FAILURE ====='
  cat /tmp/rebuild-clean13-1g.log
  echo '===== qg Weather hooks ====='
  grep -n -A35 -B12 'WeatherStatusBarBridge' "$QG" || true
  echo '===== pg Weather hooks ====='
  grep -n -A35 -B12 'WeatherStatusBarBridge' "$PG" || true
  exit 1
fi
unzip -p /tmp/rebuilt131g.apk classes2.dex > /tmp/classes2-clean13-1g.dex
test -s /tmp/classes2-clean13-1g.dex

# Repackage strictly from CLEAN13D2; only classes2 and classes4 may differ.
python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-13D2-Date-Background-Fix.apk'
out='/tmp/unsigned-clean13-1g.apk'
repl={
    'classes2.dex':open('/tmp/classes2-clean13-1g.dex','rb').read(),
    'classes4.dex':open('/tmp/classes4-clean13-1g.dex','rb').read(),
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

python3 - "$BASE" "$UNSIGNED" <<'PY'
import sys, zipfile, hashlib
base,cand=sys.argv[1:]
def members(path):
    with zipfile.ZipFile(path) as z:
        return {i.filename:z.read(i.filename) for i in z.infolist() if not i.filename.startswith('META-INF/')}
b=members(base); c=members(cand)
assert set(b)==set(c), 'archive member set changed'
changed=sorted(k for k in b if b[k]!=c[k])
assert changed==['classes2.dex','classes4.dex'], changed
for token in [b'WeatherStatusBarBridge',b'finishPopulateWeatherPage',b'handleWeatherPageAction',b'addWeatherParentAction']:
    assert token in c['classes2.dex'], token
for token in [b'CLEAN13.1G Weather page populated after native init',b'CLEAN13.1G Weather parent uses native Projectivy cloud icon',b'CLEAN13.1G native checked Show weather=',b'api.postcodes.io/postcodes/',b'api.open-meteo.com/v1/forecast']:
    assert token in c['classes4.dex'], token
for forbidden in [b'ACCESS_FINE_LOCATION',b'ACCESS_COARSE_LOCATION',b'LocationManager',b'FusedLocationProvider']:
    assert forbidden not in c['classes4.dex'], forbidden
print('PASS: CLEAN13.1G scope exact: classes2.dex + classes4.dex only')
print('classes2 sha256', hashlib.sha256(c['classes2.dex']).hexdigest())
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

"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean13-1g.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean13-1g.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean13-1g.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean13-1g.txt

rm -rf "$ART" && mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean13-1g.txt /tmp/apksigner-clean13-1g.txt /tmp/zipalign-clean13-1g.txt /tmp/package-clean13-1g.txt "$ART/"
printf '%s\n' \
  'CLEAN13.1G Weather native UI rebuild' \
  '- Built directly from frozen runtime-proven CLEAN13D2.' \
  '- Reuses the runtime-proven CLEAN13.1D Weather fetch/cache/Home behavior.' \
  '- Weather pg shell fully completes Projectivy native initialization before Weather rows replace Clock rows.' \
  '- Show weather uses Projectivy lu1.h(boolean) checked-state rendering.' \
  '- Weather parent resolves Projectivy native gmo_cloud (ta3.m4) through J0/h22 and keeps the supplied icon with mask 16.' \
  '- No guessed hv1 constructor and no hard-coded icon tile/background.' \
  '- Only classes2.dex and classes4.dex differ from CLEAN13D2 before signing.' \
  > "$ART/CLEAN13-1G-NOTES.txt"

echo "FINAL_APK=$FINAL"
