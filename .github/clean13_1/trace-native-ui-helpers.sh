#!/usr/bin/env bash
set -euo pipefail

bash .github/clean13d/build-clean13d.sh
APK=/tmp/Projectivy-CLEAN-13D2-Date-Background-Fix.apk
APKTOOL=/tmp/apktool.jar
if [ ! -f "$APKTOOL" ]; then
  curl -fL --retry 3 'https://github.com/iBotPeaches/Apktool/releases/download/v2.11.1/apktool_2.11.1.jar' -o "$APKTOOL"
fi
rm -rf /tmp/pjtrace-helper
java -jar "$APKTOOL" d -r -f "$APK" -o /tmp/pjtrace-helper >/tmp/trace-helper-decode.log 2>&1

findclass() { grep -RIl --include='*.smali' "\.class.*L$1;" /tmp/pjtrace-helper | head -1 || true; }
CB3="$(findclass cb3)"
LU1="$(findclass lu1)"
MU1="$(findclass mu1)"
H22="$(findclass h22)"
TA3="$(findclass ta3)"
YH3="$(findclass yh3)"

echo "CB3=$CB3"
echo "LU1=$LU1"
echo "MU1=$MU1"
echo "H22=$H22"
echo "TA3=$TA3"
echo "YH3=$YH3"

echo '===== cb3.s0 ====='
grep -n -A140 -B30 '^\.method.* s0(' "$CB3" || true
echo '===== cb3.J0 ====='
grep -n -A160 -B30 '^\.method.* J0(' "$CB3" || true
echo '===== cb3.q0 ====='
grep -n -A220 -B30 '^\.method.* q0(' "$CB3" || true
echo '===== cb3.r0 ====='
grep -n -A180 -B30 '^\.method.* r0(' "$CB3" || true

echo '===== lu1 constructors / checkbox-ish methods ====='
grep -n -A120 -B20 '^\.method.*constructor <init>' "$LU1" || true
grep -n -A80 -B15 '^\.method.* g(Z)' "$LU1" || true
grep -n -A80 -B15 '^\.method.* h(Z)' "$LU1" || true

echo '===== mu1 constructors/boolean accessors ====='
grep -n -A120 -B20 '^\.method.*constructor <init>' "$MU1" || true
grep -n -A70 -B15 '^\.method.* [a-zA-Z0-9_]*(.*)Z' "$MU1" | head -1200 || true

echo '===== h22 ====='
cat "$H22" | head -1200 || true

echo '===== ta3 fields + clinit ====='
grep -n '^\.field.*Lta3;' "$TA3" | head -1000 || true
sed -n '/^\.method static constructor <clinit>()V$/,/^\.end method$/p' "$TA3" | head -5000 || true

echo '===== yh3 constructors ====='
grep -n -A180 -B20 '^\.method.*constructor <init>' "$YH3" | head -1800 || true

echo '===== references to ta3.E / ta3.pe and s0 in all settings classes ====='
grep -RIn -B15 -A25 'Lta3;->E:Lta3;\|Lta3;->pe:Lta3;\|Lcb3;->s0' /tmp/pjtrace-helper/smali* | head -4000 || true
