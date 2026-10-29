#!/usr/bin/env bash
set -euo pipefail

bash .github/clean13d/build-clean13d.sh
APK=/tmp/Projectivy-CLEAN-13D2-Date-Background-Fix.apk
APKTOOL=/tmp/apktool.jar
if [ ! -f "$APKTOOL" ]; then
  curl -fL --retry 3 'https://github.com/iBotPeaches/Apktool/releases/download/v2.11.1/apktool_2.11.1.jar' -o "$APKTOOL"
fi
rm -rf /tmp/pjtrace131g
java -jar "$APKTOOL" d -r -f "$APK" -o /tmp/pjtrace131g >/tmp/trace-decode.log 2>&1

QG="$(find /tmp/pjtrace131g -path '*/qg.smali' -print -quit)"
PG="$(find /tmp/pjtrace131g -path '*/pg.smali' -print -quit)"
RG="$(find /tmp/pjtrace131g -path '*/rg.smali' -print -quit)"
MG1="$(find /tmp/pjtrace131g -path '*/mg1.smali' -print -quit)"
S21="$(find /tmp/pjtrace131g -path '*/s21.smali' -print -quit)"
TZ="$(find /tmp/pjtrace131g -path '*/tz.smali' -print -quit)"
test -n "$QG" && test -n "$PG"

echo '===== EXACT qg.e0 STATUS BAR PARENT POPULATION ====='
sed -n '/^\.method public final e0(Ljava\/util\/List;Landroid\/os\/Bundle;)V$/,/^\.end method$/p' "$QG"

echo '===== EXACT qg.i0 STATUS BAR PARENT CLICK HANDLER ====='
sed -n '/^\.method public final i0(Lmu1;)V$/,/^\.end method$/p' "$QG"

echo '===== EXACT pg.e0 WEATHER/CLOCK PAGE POPULATION ====='
sed -n '/^\.method public final e0(Ljava\/util\/List;Landroid\/os\/Bundle;)V$/,/^\.end method$/p' "$PG"

echo '===== qg -> pg REFERENCES ====='
grep -n -B30 -A60 'new-instance.*Lpg;' "$QG" || true

echo '===== mg1 COMPLETE CLASS ====='
[ -n "$MG1" ] && cat "$MG1" || true

echo '===== rg ICON/CATEGORY METHODS ====='
[ -n "$RG" ] && grep -n -A120 -B20 -E '^\.method|setIcon|ImageView|icon|Ltz;->a|Ll40;->e' "$RG" | head -1800 || true

echo '===== s21 CHECKBOX CLASS KEY METHODS ====='
[ -n "$S21" ] && grep -n -A100 -B20 -E '^\.method public constructor|^\.method.*B1\(|^\.method.*v0\(|setChecked|CheckBox|Switch' "$S21" | head -1600 || true

echo '===== tz native view styling ====='
[ -n "$TZ" ] && sed -n '/^\.method public static a(Landroid\/view\/View;)V$/,/^\.end method$/p' "$TZ" || true
