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

findclass() { grep -RIl --include='*.smali' "\.class.*L$1;" /tmp/pjtrace131g | head -1 || true; }
QG="$(findclass qg)"
PG="$(findclass pg)"
CB3="$(findclass cb3)"
H22="$(findclass h22)"
TA3="$(findclass ta3)"
S02="$(findclass s02)"
TZ="$(findclass tz)"
test -n "$QG" && test -n "$PG" && test -n "$CB3"

echo '===== qg.e0 STATUS BAR PARENT ====='
sed -n '/^\.method public final e0(/,/^\.end method$/p' "$QG"
echo '===== qg.i0 CLICK HANDLER ====='
sed -n '/^\.method public final i0(/,/^\.end method$/p' "$QG"
echo '===== pg.e0 CLOCK PAGE ====='
sed -n '/^\.method public final e0(/,/^\.end method$/p' "$PG"
echo '===== cb3.s0 CATEGORY FACTORY ====='
sed -n '/^\.method public static s0(/,/^\.end method$/p' "$CB3"
echo '===== cb3.t0 ACTION FACTORY ====='
sed -n '/^\.method public static t0(/,/^\.end method$/p' "$CB3"
echo '===== cb3.J0 ICON FACTORY ====='
sed -n '/^\.method public static J0(/,/^\.end method$/p' "$CB3"
echo '===== h22 FULL ====='
[ -n "$H22" ] && cat "$H22" || true
echo '===== ta3 relevant enum/static definitions ====='
[ -n "$TA3" ] && grep -n -A14 -B5 -E '\.field public static final (E|pe|ra):|\.method.*<init>|0x7f08' "$TA3" | head -1000 || true
echo '===== s02 interface/base ====='
[ -n "$S02" ] && head -240 "$S02" || true
echo '===== tz.a native styling ====='
[ -n "$TZ" ] && sed -n '/^\.method public static a(Landroid\/view\/View;)V$/,/^\.end method$/p' "$TZ" || true
