#!/usr/bin/env bash
set -euo pipefail

# Build exact frozen CLEAN13D2 base so we inspect the same APK lineage.
bash .github/clean13d/build-clean13d.sh
APK=/tmp/Projectivy-CLEAN-13D2-Date-Background-Fix.apk
APKTOOL=/tmp/apktool.jar
if [ ! -f "$APKTOOL" ]; then
  curl -fL --retry 3 'https://github.com/iBotPeaches/Apktool/releases/download/v2.11.1/apktool_2.11.1.jar' -o "$APKTOOL"
fi
rm -rf /tmp/pjtrace131
java -jar "$APKTOOL" d -r -f "$APK" -o /tmp/pjtrace131 >/tmp/trace-decode.log 2>&1

echo '===== REFERENCES TO Lpg; (Clock settings fragment) ====='
grep -R -n -B12 -A25 'Lpg;' /tmp/pjtrace131/smali* | head -500 || true

echo '===== STATUS BAR / CLOCK STRING CONTEXT ====='
grep -R -n -i -B15 -A35 -E 'status.?bar|clock format|appearance_statusbar|statusbar_icons_container' /tmp/pjtrace131/smali* | head -900 || true

echo '===== CLASSES CONTAINING BOTH pg AND ACTION LIST METHODS ====='
for f in $(grep -R -l 'Lpg;' /tmp/pjtrace131/smali* 2>/dev/null | head -100); do
  echo "--- $f"
  grep -n -E '^\.class|new-instance.*Lpg;|invoke-.*Lpg;|e0\(Ljava/util/List;|i0\(Lmu1;' "$f" | head -120 || true
done
