#!/usr/bin/env bash
set -euo pipefail

bash .github/clean13d/build-clean13d.sh
APK=/tmp/Projectivy-CLEAN-13D2-Date-Background-Fix.apk
APKTOOL=/tmp/apktool.jar
if [ ! -f "$APKTOOL" ]; then
  curl -fL --retry 3 'https://github.com/iBotPeaches/Apktool/releases/download/v2.11.1/apktool_2.11.1.jar' -o "$APKTOOL"
fi
rm -rf /tmp/pjtrace-native-ui
java -jar "$APKTOOL" d -r -f "$APK" -o /tmp/pjtrace-native-ui >/tmp/trace-native-ui-decode.log 2>&1
QG="$(find /tmp/pjtrace-native-ui -path '*/qg.smali' -print -quit)"
PG="$(find /tmp/pjtrace-native-ui -path '*/pg.smali' -print -quit)"
RG="$(find /tmp/pjtrace-native-ui -path '*/rg.smali' -print -quit)"
HV1="$(find /tmp/pjtrace-native-ui -path '*/hv1.smali' -print -quit)"
CB3="$(find /tmp/pjtrace-native-ui -path '*/cb3.smali' -print -quit)"

echo '===== qg.e0 STATUS BAR ROOT ====='
sed -n '/^\.method public final e0(Ljava\/util\/List;Landroid\/os\/Bundle;)V$/,/^\.end method$/p' "$QG" | head -1200

echo '===== qg ctor / resource references ====='
sed -n '/^\.method public constructor <init>/,/^\.end method$/p' "$QG" | head -600

echo '===== rg.e0 STATUS ICONS PAGE ====='
sed -n '/^\.method public final e0(Ljava\/util\/List;Landroid\/os\/Bundle;)V$/,/^\.end method$/p' "$RG" | head -1600

echo '===== pg.e0 CLOCK PAGE ====='
sed -n '/^\.method public final e0(Ljava\/util\/List;Landroid\/os\/Bundle;)V$/,/^\.end method$/p' "$PG" | head -1200

echo '===== hv1 constructors ====='
grep -n -A80 -B10 '^\.method.*constructor <init>' "$HV1" | head -1000

echo '===== cb3 checkbox/action builders ====='
grep -n -A100 -B20 '^\.method.* r0(' "$CB3" | head -1200 || true
grep -n -A100 -B20 '^\.method.* t0(' "$CB3" | head -1200 || true

echo '===== resource IDs referenced by qg/rg/pg ====='
for f in "$QG" "$RG" "$PG"; do
  echo "--- $f ---"
  grep -oE '0x7f[0-9a-f]{6}' "$f" | sort -u
 done

echo '===== AAPT resources likely settings/statusbar/icon ====='
"$ANDROID_HOME/build-tools/$(ls "$ANDROID_HOME/build-tools" | sort -V | tail -1)/aapt" dump resources "$APK" \
 | grep -Ei -A3 -B2 'statusbar|status_bar|clock|notification|settings|icon.*background|background.*icon|menu_settings|appearance' \
 | head -3000 || true
