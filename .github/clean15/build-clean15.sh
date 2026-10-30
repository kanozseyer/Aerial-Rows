#!/usr/bin/env bash
set -euo pipefail

DOWNLOAD=/tmp/clean14-approved
OUTER="$DOWNLOAD/clean14-artifact.zip"
UNPACKED=/tmp/clean14-approved-unpacked
BASE_APK="$UNPACKED/clean14-artifact/Projectivy-CLEAN-14-Universal-Carousel.apk"
EXPECTED_CLEAN14_SHA=06a5ed63040a61dd3705f7fb9e3e0b6eef4393e129d00d92eaef2b4781fb092d

APKTOOL=/tmp/apktool-clean15.jar
DECODED=/tmp/pj-clean15
REBUILT=/tmp/rebuilt-clean15.apk
UNSIGNED=/tmp/unsigned-clean15.apk
ALIGNED=/tmp/aligned-clean15.apk
FINAL=/tmp/Projectivy-CLEAN-15-Channel-Row-Refresh.apk
ART=/tmp/clean15-artifact

rm -rf "$UNPACKED" "$DECODED" "$ART"
mkdir -p "$UNPACKED" "$ART"
test -s "$OUTER" || { echo "Missing downloaded CLEAN14 artifact: $OUTER"; find "$DOWNLOAD" -maxdepth 3 -type f -printf '%p %s bytes\\n'; exit 1; }
unzip -t "$OUTER" > "$ART/BASE-CLEAN14-artifact-zip-test.txt"
unzip -q "$OUTER" -d "$UNPACKED"
test -s "$BASE_APK" || { echo "Missing approved CLEAN14 APK: $BASE_APK"; find "$UNPACKED" -maxdepth 4 -type f -printf '%p %s bytes\\n'; exit 1; }

ACTUAL_CLEAN14_SHA="$(sha256sum "$BASE_APK" | awk '{print $1}')"
if [ "$ACTUAL_CLEAN14_SHA" != "$EXPECTED_CLEAN14_SHA" ]; then
  echo "Approved CLEAN14 APK hash mismatch: expected $EXPECTED_CLEAN14_SHA got $ACTUAL_CLEAN14_SHA"
  exit 1
fi
sha256sum "$BASE_APK" | tee "$ART/BASE-CLEAN14-SHA256.txt"

if [ ! -s "$APKTOOL" ]; then
  curl -fL --retry 3 'https://github.com/iBotPeaches/Apktool/releases/download/v2.11.1/apktool_2.11.1.jar' -o "$APKTOOL"
fi

# Decode only smali; resources.arsc and every resource remain opaque/unchanged.
java -jar "$APKTOOL" d -r -f "$BASE_APK" -o "$DECODED" > "$ART/apktool-decode-clean15.txt" 2>&1
python3 .github/clean15/fix-clean15.py "$DECODED"

R12="$DECODED/smali/r12.smali"
W12="$DECODED/smali/w12.smali"
test -s "$R12" && test -s "$W12"
grep -Fq 'CLEAN15ChannelRefreshBridge;->install(Landroid/view/ViewGroup;)V' "$R12"
grep -Fq 'CLEAN15ChannelRefreshBridge;->bind(Landroid/view/ViewGroup;Ljava/lang/Object;)V' "$R12"
grep -Fq 'CLEAN15ChannelRefreshBridge;->tint(Landroid/view/ViewGroup;I)V' "$W12"
cp "$R12" "$ART/r12-smali-CLEAN15-patched.txt"
cp "$W12" "$ART/w12-smali-CLEAN15-patched.txt"

if ! java -jar "$APKTOOL" b "$DECODED" -o "$REBUILT" > "$ART/apktool-rebuild-clean15.txt" 2>&1; then
  echo "===== CLEAN15 APKTOOL REBUILD FAILURE ====="
  cat "$ART/apktool-rebuild-clean15.txt"
  echo "===== PATCHED r12 / w12 ====="
  grep -n -A10 -B8 'CLEAN15ChannelRefreshBridge' "$R12" "$W12" || true
  exit 1
fi
unzip -p "$REBUILT" classes.dex > /tmp/classes-clean15.dex
test -s /tmp/classes-clean15.dex

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"

# Add the refresh bridge as classes6.dex. The approved CLEAN14 classes4.dex
# and classes5.dex are deliberately preserved byte-for-byte.
rm -rf /tmp/clean15-helper-classes /tmp/clean15-helper-dex
mkdir -p /tmp/clean15-helper-classes /tmp/clean15-helper-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/clean15-helper-classes \
  .github/clean15/CLEAN15ChannelRefreshBridge.java

mapfile -t CLASSFILES < <(find /tmp/clean15-helper-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/clean15-helper-dex "${CLASSFILES[@]}"
cp /tmp/clean15-helper-dex/classes.dex /tmp/classes6-clean15.dex
test -s /tmp/classes6-clean15.dex

for marker in \
  'CLEAN15ChannelRefreshBridge' \
  'CLEAN15 channel refresh action installed' \
  'CLEAN15 channel row refresh enabled' \
  'CLEAN15 forced Projectivy channel refresh dispatched' \
  'CLEAN15 Red Light manual sync started' \
  'CLEAN15 Kodi is not ready; refusing to launch Kodi'; do
  strings /tmp/classes6-clean15.dex | grep -Fq "$marker"
done
strings /tmp/classes-clean15.dex | grep -Fq 'CLEAN15ChannelRefreshBridge'
strings /tmp/classes-clean15.dex | grep -Fq 'install'
strings /tmp/classes-clean15.dex | grep -Fq 'bind'
strings /tmp/classes-clean15.dex | grep -Fq 'tint'

unzip -p "$BASE_APK" classes4.dex > /tmp/classes4-approved-clean14.dex
unzip -p "$BASE_APK" classes5.dex > /tmp/classes5-approved-clean14.dex
for marker in \
  'CLEAN13.1I status order watcher attached' \
  'CLEAN13.1G Weather parent uses native Projectivy cloud icon' \
  'CLEAN13.1O native checkbox built builder=hv1 action=iv1 checked=' \
  'CLEAN13.1P manual refresh button clicked' \
  'CLEAN13.1P refresh completed success=' \
  'CLEAN13.1P Show date native action=' \
  'CLEAN13.1P Show date checked='; do
  strings /tmp/classes4-approved-clean14.dex | grep -Fq "$marker"
done
for marker in \
  'CLEAN14CarouselBridge' \
  'CLEAN14 shared carousel attached' \
  'CLEAN14 alignment mode=3 offset=' \
  'CLEAN14 end bounce distancePx='; do
  strings /tmp/classes5-approved-clean14.dex | grep -Fq "$marker"
done

# Repackage from the exact runtime-approved CLEAN14 APK: replace classes.dex
# with the two left-rail hooks and add only classes6.dex.
python3 - "$BASE_APK" "$UNSIGNED" <<'PY'
import sys, zipfile
base, out = sys.argv[1:]
replacement = open('/tmp/classes-clean15.dex', 'rb').read()
new_helper = open('/tmp/classes6-clean15.dex', 'rb').read()
with zipfile.ZipFile(base) as zin, zipfile.ZipFile(out, 'w') as zout:
    names = set(zin.namelist())
    assert 'classes6.dex' not in names, 'CLEAN14 baseline already contains classes6.dex'
    for info in zin.infolist():
        if info.filename.startswith('META-INF/'):
            continue
        data = replacement if info.filename == 'classes.dex' else zin.read(info.filename)
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
    zout.writestr('classes6.dex', new_helper, compress_type=zipfile.ZIP_DEFLATED)
PY
unzip -t "$UNSIGNED" > "$ART/unsigned-zip-test.txt"

python3 - "$BASE_APK" "$UNSIGNED" <<'PY'
import sys, zipfile, hashlib
base, candidate = sys.argv[1:]
def members(path):
    with zipfile.ZipFile(path) as z:
        return {i.filename: z.read(i.filename) for i in z.infolist()
                if not i.filename.startswith('META-INF/')}
old = members(base)
new = members(candidate)
assert set(new) == set(old) | {'classes6.dex'}, 'unexpected archive member change'
changed = sorted(k for k in old if old[k] != new[k])
assert changed == ['classes.dex'], changed
assert old['classes4.dex'] == new['classes4.dex'], 'CLEAN14 Weather/Red Light bridge DEX changed'
assert old['classes5.dex'] == new['classes5.dex'], 'CLEAN14 carousel bridge DEX changed'
assert b'CLEAN15ChannelRefreshBridge' in new['classes.dex']
for token in [
    b'CLEAN15ChannelRefreshBridge',
    b'CLEAN15 channel refresh action installed',
    b'CLEAN15 channel row refresh enabled',
    b'CLEAN15 forced Projectivy channel refresh dispatched',
    b'CLEAN15 Red Light manual sync started',
]:
    assert token in new['classes6.dex'], token
print('PASS: CLEAN15 scope exact: classes.dex changed + classes6.dex added; all existing CLEAN14 DEX preserved')
print('classes.dex sha256', hashlib.sha256(new['classes.dex']).hexdigest())
print('classes4.dex preserved sha256', hashlib.sha256(new['classes4.dex']).hexdigest())
print('classes5.dex preserved sha256', hashlib.sha256(new['classes5.dex']).hexdigest())
print('classes6.dex sha256', hashlib.sha256(new['classes6.dex']).hexdigest())
PY

# Keep the exact same signing certificate so CLEAN15 installs as an update.
curl -fL --retry 3 'https://android.googlesource.com/platform/build/+/refs/heads/master/target/product/security/testkey.pk8?format=TEXT' | base64 -d > /tmp/testkey.pk8
curl -fL --retry 3 'https://android.googlesource.com/platform/build/+/refs/heads/master/target/product/security/testkey.x509.pem?format=TEXT' | base64 -d > /tmp/testkey.x509.pem

"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" "$ALIGNED"
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" "$ALIGNED"

"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee "$ART/apksigner-clean15.txt"
grep -Fq 'certificate SHA-256 digest: a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc' "$ART/apksigner-clean15.txt"
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > "$ART/zipalign-clean15.txt"
"$BT/aapt" dump badging "$FINAL" | head -1 | tee "$ART/package-clean15.txt"
grep -Fq "package: name='com.spocky.projengmenu' versionCode='97' versionName='4.72'" "$ART/package-clean15.txt"
sha256sum "$FINAL" | tee "$ART/SHA256-clean15.txt"
cp "$FINAL" "$ART/"

printf '%s\\n' \
  'CLEAN15 Channel Row Refresh candidate' \
  '- Built directly from the exact runtime-approved CLEAN14 APK.' \
  '- Preserves CLEAN14 universal carousel, Weather, Show Date, native Weather icon/background and status order.' \
  '- Adds a focusable refresh action to the left category header for every Projectivy channel-backed row (native channel predicate i5.j()).' \
  '- The refresh action appears alongside the existing reorder/settings controls when the channel row is selected; app/category rows are unchanged.' \
  '- Native Android TV channel rows use Projectivy forced refresh w40.z(false), which invalidates its TV channel, Watch Next and Preview Program caches and rebuilds the home model.' \
  '- Red Light rows additionally run the existing Kodi discovery engine after checking that Kodi is already ready and acquiring the existing provider lease; Kodi is never launched or foregrounded.' \
  '- Projectivy exposes a global forced-refresh entrypoint, so any channel-row button refreshes the channel set as a whole; the Red Light scan likewise updates all Red Light shelves.' \
  '- Existing CLEAN14 classes4.dex and classes5.dex are preserved byte-for-byte; only classes.dex changes and classes6.dex is added.' \
  '- Runtime testing on the Shield is required before freezing CLEAN15.' \
  > "$ART/CLEAN15-NOTES.txt"

cd /tmp
rm -f clean15-artifact.zip
zip -qr clean15-artifact.zip clean15-artifact
echo "FINAL_APK=$FINAL"
echo "ARTIFACT_ZIP=/tmp/clean15-artifact.zip"
