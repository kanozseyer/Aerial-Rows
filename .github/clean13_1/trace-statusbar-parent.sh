#!/usr/bin/env bash
set -euo pipefail

# Build exact frozen CLEAN13D2 base so we inspect the same APK lineage.
# Trace v3: dump exact qg parent method control flow so Weather is injected
# on a guaranteed-executed path, not merely before an arbitrary final return.
bash .github/clean13d/build-clean13d.sh
APK=/tmp/Projectivy-CLEAN-13D2-Date-Background-Fix.apk
APKTOOL=/tmp/apktool.jar
if [ ! -f "$APKTOOL" ]; then
  curl -fL --retry 3 'https://github.com/iBotPeaches/Apktool/releases/download/v2.11.1/apktool_2.11.1.jar' -o "$APKTOOL"
fi
rm -rf /tmp/pjtrace131
java -jar "$APKTOOL" d -r -f "$APK" -o /tmp/pjtrace131 >/tmp/trace-decode.log 2>&1

QG="$(find /tmp/pjtrace131 -path '*/qg.smali' -print -quit)"
PG="$(find /tmp/pjtrace131 -path '*/pg.smali' -print -quit)"
test -n "$QG" && test -n "$PG"

echo '===== EXACT qg.e0 STATUS BAR PARENT POPULATION ====='
sed -n '/^\.method public final e0(Ljava\/util\/List;Landroid\/os\/Bundle;)V$/,/^\.end method$/p' "$QG"

echo '===== EXACT qg.i0 STATUS BAR PARENT CLICK HANDLER ====='
sed -n '/^\.method public final i0(Lmu1;)V$/,/^\.end method$/p' "$QG"

echo '===== EXACT pg.e0 CLOCK PAGE POPULATION ====='
sed -n '/^\.method public final e0(Ljava\/util\/List;Landroid\/os\/Bundle;)V$/,/^\.end method$/p' "$PG" | head -800

echo '===== qg -> pg REFERENCES ====='
grep -n -B20 -A40 'new-instance.*Lpg;' "$QG" || true
