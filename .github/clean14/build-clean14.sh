#!/usr/bin/env bash
set -euo pipefail

P_OUTER=/tmp/clean13-1p-approved/clean13-1p-artifact.zip
P_DIR=/tmp/clean13-1p-approved/unpacked/clean13-1p-artifact
P_APK="$P_DIR/Projectivy-CLEAN-13-1P-Show-Date-Native-Checkbox.apk"
EXPECTED_P_SHA=96936b6c03316cd067374b9060c0b4248b9d6e9e9f5106b81f05c94cc5d05c4f

APKTOOL=/tmp/apktool.jar
DECODED=/tmp/pj-clean14
REBUILT=/tmp/rebuilt-clean14.apk
UNSIGNED=/tmp/unsigned-clean14.apk
ALIGNED=/tmp/aligned-clean14.apk
FINAL=/tmp/Projectivy-CLEAN-14-Universal-Carousel.apk
ART=/tmp/clean14-artifact

test -s "$P_OUTER"
rm -rf "$DECODED" "$ART" /tmp/clean13-1p-approved/unpacked
mkdir -p "$ART" /tmp/clean13-1p-approved/unpacked
unzip -t "$P_OUTER" > "$ART/APPROVED-P-ARTIFACT-ZIP-TEST.txt"
unzip -q "$P_OUTER" -d /tmp/clean13-1p-approved/unpacked
test -s "$P_APK"

ACTUAL_P_SHA="$(sha256sum "$P_APK" | awk '{print $1}')"
if [ "$ACTUAL_P_SHA" != "$EXPECTED_P_SHA" ]; then
  echo "Approved P APK hash mismatch: expected $EXPECTED_P_SHA got $ACTUAL_P_SHA"
  exit 1
fi
sha256sum "$P_APK" | tee "$ART/BASE-CLEAN13-1P-SHA256.txt"
cp "$P_DIR/apksigner-clean13-1p.txt" "$ART/BASE-apksigner-clean13-1p.txt"
cp "$P_DIR/package-clean13-1p.txt" "$ART/BASE-package-clean13-1p.txt"

if [ ! -s "$APKTOOL" ]; then
  curl -fL --retry 3 'https://github.com/iBotPeaches/Apktool/releases/download/v2.11.1/apktool_2.11.1.jar' -o "$APKTOOL"
fi

# Decode only smali; resources.arsc remains opaque and unchanged.
java -jar "$APKTOOL" d -r -f "$P_APK" -o "$DECODED" > "$ART/apktool-decode-clean14.txt" 2>&1
python3 .github/clean14/fix-clean14.py "$DECODED"

LU="$DECODED/smali/lu.smali"
test -s "$LU"
grep -Fq 'CLEAN14CarouselBridge;->attach(Landroid/view/ViewGroup;)V' "$LU"
cp "$LU" "$ART/lu-smali-CLEAN14-patched.txt"

# Rebuild the original classes.dex with the single audited lu.h hook.
if ! java -jar "$APKTOOL" b "$DECODED" -o "$REBUILT" > "$ART/apktool-rebuild-clean14.txt" 2>&1; then
  echo "===== CLEAN14 APKTOOL REBUILD FAILURE ====="
  cat "$ART/apktool-rebuild-clean14.txt"
  echo "===== PATCHED lu.h ====="
  grep -n -A35 -B12 'CLEAN14CarouselBridge' "$LU" || true
  exit 1
fi
unzip -p "$REBUILT" classes.dex > /tmp/classes-clean14.dex
test -s /tmp/classes-clean14.dex

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"

# Recompile the full already-approved embedded bridge set plus CLEAN14.
rm -rf /tmp/embedded14-classes /tmp/embedded14-dex
mkdir -p /tmp/embedded14-classes /tmp/embedded14-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded14-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java \
  .github/clean13d/StatusBarCustomizationBridge.java \
  .github/clean13_1/WeatherStatusBarBridge.java \
  .github/clean14/CLEAN14CarouselBridge.java

mapfile -t CLASSFILES < <(find /tmp/embedded14-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded14-dex "${CLASSFILES[@]}"
cp /tmp/embedded14-dex/classes.dex /tmp/classes4-clean14.dex
test -s /tmp/classes4-clean14.dex

for marker in \
  'CLEAN14 shared carousel attached' \
  'CLEAN14 alignment mode=3 offset=' \
  'CLEAN14 end bounce distancePx=' \
  'CLEAN13.1I status order watcher attached' \
  'CLEAN13.1G Weather parent uses native Projectivy cloud icon' \
  'CLEAN13.1O native checkbox built builder=hv1 action=iv1 checked=' \
  'CLEAN13.1P manual refresh button clicked' \
  'CLEAN13.1P refresh completed success=' \
  'CLEAN13.1P native checkbox built builder=hv1 action=iv1 checked=' \
  'CLEAN13.1P Show date native action=' \
  'CLEAN13.1P Show date checked=' \
  'CLEAN13D2 native clock/date actions added; unified default' \
  'CLEAN12.1 SUCCESS channels='; do
  strings /tmp/classes4-clean14.dex | grep -Fq "$marker"
done
strings /tmp/classes-clean14.dex | grep -Fq 'CLEAN14CarouselBridge'
strings /tmp/classes-clean14.dex | grep -Fq 'attach'
strings /tmp/classes-clean14.dex | grep -Fq 'Landroid/view/ViewGroup;'

# Repackage directly from the exact runtime-approved P APK.
python3 - "$P_APK" "$UNSIGNED" <<'PY'
import sys, zipfile
base, out = sys.argv[1:]
replacements = {
    'classes.dex': open('/tmp/classes-clean14.dex', 'rb').read(),
    'classes4.dex': open('/tmp/classes4-clean14.dex', 'rb').read(),
}
seen = set()
with zipfile.ZipFile(base) as zin, zipfile.ZipFile(out, 'w') as zout:
    for info in zin.infolist():
        if info.filename.startswith('META-INF/'):
            continue
        data = replacements.get(info.filename, zin.read(info.filename))
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
        seen.add(info.filename)
assert set(replacements).issubset(seen), 'missing DEX member in P APK'
PY
unzip -t "$UNSIGNED" > "$ART/unsigned-zip-test.txt"

python3 - "$P_APK" "$UNSIGNED" <<'PY'
import sys, zipfile, hashlib
base, candidate = sys.argv[1:]
def members(path):
    with zipfile.ZipFile(path) as z:
        return {i.filename: z.read(i.filename) for i in z.infolist()
                if not i.filename.startswith('META-INF/')}
old = members(base)
new = members(candidate)
assert set(old) == set(new), 'archive member set changed'
changed = sorted(k for k in old if old[k] != new[k])
assert changed == ['classes.dex', 'classes4.dex'], changed
assert old['classes.dex'] != new['classes.dex']
assert old['classes4.dex'] != new['classes4.dex']
for token in [
    b'CLEAN14CarouselBridge',
    b'CLEAN14 shared carousel attached',
    b'CLEAN14 alignment mode=3 offset=',
    b'CLEAN14 end bounce distancePx=',
]:
    assert token in new['classes4.dex'], token
assert b'CLEAN14CarouselBridge' in new['classes.dex']
print('PASS: CLEAN14 scope exact: classes.dex + classes4.dex only vs approved P')
print('classes.dex sha256', hashlib.sha256(new['classes.dex']).hexdigest())
print('classes4.dex sha256', hashlib.sha256(new['classes4.dex']).hexdigest())
PY

# Use the same public AOSP testkey used by the approved P build. Fail if its
# certificate no longer matches P, because Shield must accept an in-place update.
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

"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee "$ART/apksigner-clean14.txt"
grep -Fq 'certificate SHA-256 digest: a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc' "$ART/apksigner-clean14.txt"
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > "$ART/zipalign-clean14.txt"
"$BT/aapt" dump badging "$FINAL" | head -1 | tee "$ART/package-clean14.txt"
grep -Fq "package: name='com.spocky.projengmenu' versionCode='97' versionName='4.72'" "$ART/package-clean14.txt"
sha256sum "$FINAL" | tee "$ART/SHA256-clean14.txt"
cp "$FINAL" "$ART/"

printf '%s\n' \
  'CLEAN14 Universal Horizontal Carousel candidate' \
  '- Built directly from the exact runtime-approved CLEAN13.1P APK.' \
  '- Preserves Weather, Show Date, native cloud icon/background, status order, and forced manual Weather refresh.' \
  '- Hooks the shared Projectivy lu HorizontalGridView presenter, covering the normal jo4 path and f23 multi-row subclass.' \
  '- Uses BOTH_EDGE alignment so the first and last cards clamp to the natural row edges.' \
  '- Middle anchor is one complete preceding card plus the actual horizontal spacing, using measured card width and the current item alignment offset.' \
  '- At the end, the row stays fixed while focus traverses the remaining visible cards; moving left resumes scrolling only after focus passes the middle anchor.' \
  '- Adds a subtle 2dp elastic end bounce when rightward focus reaches the final card.' \
  '- Existing io4/ho4 touch-scroll settling listeners are not modified.' \
  '- Only classes.dex and classes4.dex differ from approved CLEAN13.1P before signing.' \
  '- Runtime testing on the Shield is required before freezing CLEAN14.' \
  > "$ART/CLEAN14-NOTES.txt"

cd /tmp
rm -f clean14-artifact.zip
zip -qr clean14-artifact.zip clean14-artifact
echo "FINAL_APK=$FINAL"
echo "ARTIFACT_ZIP=/tmp/clean14-artifact.zip"
