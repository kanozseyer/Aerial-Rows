#!/usr/bin/env bash
set -euo pipefail

J_APK=/tmp/Projectivy-CLEAN-13-1J-Weather-Selector.apk
UNSIGNED=/tmp/unsigned-clean13-1k.apk
ALIGNED=/tmp/aligned-clean13-1k.apk
FINAL=/tmp/Projectivy-CLEAN-13-1K-Weather-Choice-Monochrome-Icons.apk
ART=/tmp/clean13-1k-artifact
APKTOOL=/tmp/apktool.jar

# Rebuild the exact user-tested J candidate first. J itself rebuilds the frozen I
# baseline, so I's Weather category/icon/background and status order remain anchored.
bash .github/clean13_1j/build-clean13-1j.sh
test -f "$J_APK"

# Fix only the selector semantics: parent opens the selector; On/Off child choice
# is the only path allowed to mutate the Weather preference.
python3 .github/clean13_1k/fix-weather-choice.py
BRIDGE=.github/clean13_1/WeatherStatusBarBridge.java
grep -Fq 'CLEAN13.1K show weather choice=' "$BRIDGE"
grep -Fq 'if (id == ID_SHOW_ON || id == ID_SHOW_OFF)' "$BRIDGE"
! grep -Fq 'id == ID_SHOW_PARENT ? !p.getBoolean(KEY_SHOW, true)' "$BRIDGE"
grep -Fq 'CLEAN13.1I status order watcher attached' "$BRIDGE"
grep -Fq 'CLEAN13.1G Weather parent uses native Projectivy cloud icon' "$BRIDGE"
! grep -Fq 'Class.forName("hv1")' "$BRIDGE"

PLATFORM="$(ls -d "$ANDROID_HOME"/platforms/android-* | sort -V | tail -1)"
ANDROID_JAR="$PLATFORM/android.jar"
BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"

# Recompile only the embedded classes4 stack after the K Weather semantics delta.
rm -rf /tmp/embedded131k-classes /tmp/embedded131k-dex
mkdir -p /tmp/embedded131k-classes /tmp/embedded131k-dex
javac -source 8 -target 8 -cp "$ANDROID_JAR" -d /tmp/embedded131k-classes \
  .github/clean02/AerialService.java \
  .github/clean03/AerialSettingsActivity.java \
  .github/clean05/ProfileRestoreProvider.java \
  .github/clean07/RedLightDiscoveryEngine.java \
  .github/clean07/RedLightDiscoveryProvider.java \
  .github/clean07/ChannelVisualBridge.java \
  .github/clean09/RedLightLaunchActivity.java \
  .github/clean13d/StatusBarCustomizationBridge.java \
  "$BRIDGE"
mapfile -t CLASSFILES < <(find /tmp/embedded131k-classes -name '*.class' -type f | sort)
"$BT/d8" --lib "$ANDROID_JAR" --min-api 21 --output /tmp/embedded131k-dex "${CLASSFILES[@]}"
cp /tmp/embedded131k-dex/classes.dex /tmp/classes4-clean13-1k.dex
test -s /tmp/classes4-clean13-1k.dex
for marker in \
  'CLEAN13.1K show weather choice=' \
  'CLEAN13.1J Show weather selector=' \
  'CLEAN13.1I status order watcher attached' \
  'CLEAN13.1G Weather parent uses native Projectivy cloud icon' \
  'api.postcodes.io/postcodes/' \
  'api.open-meteo.com/v1/forecast' \
  'CLEAN12.1 SUCCESS channels='; do
  strings /tmp/classes4-clean13-1k.dex | grep -Fq "$marker"
done

# Decode the exact J APK and patch only the two Projectivy icon render paths:
#  1) IconHeaderImageView => left rail + Home row-header icons.
#  2) pq0.H => AppPreference/AppCustomSwitchPreference menu icons.
# Posters/cards (lo2 and ordinary ImageViews) are deliberately untouched.
if [ ! -f "$APKTOOL" ]; then
  curl -fL --retry 3 'https://github.com/iBotPeaches/Apktool/releases/download/v2.11.1/apktool_2.11.1.jar' -o "$APKTOOL"
fi
rm -rf /tmp/pj131k-core /tmp/rebuilt131k-core.apk
java -jar "$APKTOOL" d -r -f "$J_APK" -o /tmp/pj131k-core >/tmp/decode-clean13-1k.log 2>&1
python3 .github/clean13_1k/patch-monochrome-icons.py /tmp/pj131k-core | tee /tmp/icon-patch-clean13-1k.txt

ICON_SMALI="$(find /tmp/pj131k-core -name 'IconHeaderImageView.smali' -print -quit)"
PQ0_SMALI="$(find /tmp/pj131k-core -name 'pq0.smali' -print -quit)"
test -n "$ICON_SMALI" && test -n "$PQ0_SMALI"
grep -Fq '.method public setImageDrawable(Landroid/graphics/drawable/Drawable;)V' "$ICON_SMALI"
grep -Fq 'Lcom/spocky/projengmenu/ui/home/header/IconHeaderImageView;->a(I)V' "$ICON_SMALI"
python3 - "$PQ0_SMALI" <<'PY'
from pathlib import Path
import sys
s=Path(sys.argv[1]).read_text()
sig='.method public static final H(Landroidx/preference/Preference;Lbi3;II)V'
a=s.index(sig); b=s.index('.end method',a)
h=s[a:b]
for t in ['Lpq0;->u()I','Lcc0;->d(FII)I','ImageView;->setColorFilter(I)V','View;->setAlpha(F)V']:
    assert t in h,t
print('PASS: pq0.H carries native Projectivy monochrome filter')
PY

if ! java -jar "$APKTOOL" b /tmp/pj131k-core -o /tmp/rebuilt131k-core.apk >/tmp/rebuild-clean13-1k.log 2>&1; then
  echo '===== CLEAN13.1K APKTOOL REBUILD FAILURE ====='
  cat /tmp/rebuild-clean13-1k.log
  exit 1
fi
unzip -p /tmp/rebuilt131k-core.apk classes.dex > /tmp/classes-clean13-1k.dex
unzip -p /tmp/rebuilt131k-core.apk classes2.dex > /tmp/classes2-clean13-1k.dex
test -s /tmp/classes-clean13-1k.dex
test -s /tmp/classes2-clean13-1k.dex

# Repackage directly from rebuilt J. Only classes/classes2/classes4 are allowed to
# differ before signing; all resources, manifest, channel data and artwork stay J.
python3 - <<'PY'
import zipfile
base='/tmp/Projectivy-CLEAN-13-1J-Weather-Selector.apk'
out='/tmp/unsigned-clean13-1k.apk'
repl={
    'classes.dex':open('/tmp/classes-clean13-1k.dex','rb').read(),
    'classes2.dex':open('/tmp/classes2-clean13-1k.dex','rb').read(),
    'classes4.dex':open('/tmp/classes4-clean13-1k.dex','rb').read(),
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

python3 - "$J_APK" "$UNSIGNED" <<'PY'
import sys,zipfile,hashlib
base,cand=sys.argv[1:]
def members(path):
    with zipfile.ZipFile(path) as z:
        return {i.filename:z.read(i.filename) for i in z.infolist() if not i.filename.startswith('META-INF/')}
b=members(base); c=members(cand)
assert set(b)==set(c), 'archive member set changed'
changed=sorted(k for k in b if b[k]!=c[k])
assert changed==['classes.dex','classes2.dex','classes4.dex'], changed
for token in [
    b'CLEAN13.1K show weather choice=',
    b'CLEAN13.1J Show weather selector=',
    b'CLEAN13.1I status order watcher attached',
    b'CLEAN13.1G Weather parent uses native Projectivy cloud icon',
]:
    assert token in c['classes4.dex'], token
for forbidden in [b'CLEAN13.1H native checkbox Show weather=', b'Class.forName("hv1")']:
    assert forbidden not in c['classes4.dex'], forbidden
print('PASS: CLEAN13.1K archive scope exact: classes.dex + classes2.dex + classes4.dex only vs J')
for k in changed:
    print(k, hashlib.sha256(c[k]).hexdigest())
PY

"$BT/zipalign" -P 16 -f 4 "$UNSIGNED" "$ALIGNED"
"$BT/apksigner" sign \
  --key /tmp/testkey.pk8 \
  --cert /tmp/testkey.x509.pem \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$FINAL" "$ALIGNED"
"$BT/apksigner" verify --verbose --print-certs "$FINAL" | tee /tmp/apksigner-clean13-1k.txt
"$BT/zipalign" -P 16 -c -v 4 "$FINAL" > /tmp/zipalign-clean13-1k.txt
"$BT/aapt" dump badging "$FINAL" | head -1 | tee /tmp/package-clean13-1k.txt
sha256sum "$FINAL" | tee /tmp/SHA256-clean13-1k.txt

rm -rf "$ART" && mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean13-1k.txt /tmp/apksigner-clean13-1k.txt /tmp/zipalign-clean13-1k.txt /tmp/package-clean13-1k.txt /tmp/icon-patch-clean13-1k.txt "$ART/"
printf '%s\n' \
  'CLEAN13.1K Weather choice + native monochrome icons' \
  '- Base is rebuilt CLEAN13.1J; frozen CLEAN13.1I Weather category/icon/background/order retained.' \
  '- Show weather parent now only opens On/Off; only the chosen child changes state.' \
  '- Left-hand/header and Home row icons are tinted centrally through Projectivy IconHeaderImageView.' \
  '- Projectivy AppPreference/AppCustomSwitchPreference icons use the exact pq0.J native monochrome filter/alpha.' \
  '- Red Light/SmartTube source artwork remains intact; presentation tint is applied only in icon slots.' \
  '- Posters, programme artwork and app cards are not tinted.' \
  '- Only classes.dex, classes2.dex and classes4.dex differ from rebuilt J before signing.' \
  > "$ART/CLEAN13-1K-NOTES.txt"

echo "FINAL_APK=$FINAL"
