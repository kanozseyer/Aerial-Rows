#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-10-3-Sanitized-TV-Browse-URI.apk
FINAL=/tmp/Projectivy-CLEAN-11-Cold-Readiness-Gate.apk
ART=/tmp/clean11-artifact

# Rebuild the frozen CLEAN10.3 payload first, now compiling the CLEAN11-gated
# RedLightDiscoveryProvider from this branch into classes4.dex.
bash .github/clean103/build-clean103.sh

test -f "$BASE"
python3 .github/clean11/verify-clean11.py "$BASE"

cp -f "$BASE" "$FINAL"
sha256sum "$FINAL" | tee /tmp/SHA256-clean11.txt

rm -rf "$ART"
mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean11.txt "$ART/"
cp -f /tmp/apksigner-clean103.txt "$ART/apksigner-clean11.txt"
cp -f /tmp/zipalign-clean103.txt "$ART/zipalign-clean11.txt"
cp -f /tmp/package-clean103.txt "$ART/package-clean11.txt"
cp -f /tmp/manifest-clean103.txt "$ART/manifest-clean11.txt"

printf '%s\n' \
  'CLEAN11: Kodi-native readiness gate' \
  '- Frozen CLEAN10.3 movie/episode playback retained.' \
  '- Frozen CLEAN10.3 TV-show browse retained.' \
  '- Red Light discovery never queries cold Kodi.' \
  '- Cached Android TV rows/cards remain visible while Kodi is cold.' \
  '- Sync runs only when 127.0.0.1:9090 proves Kodi native core is ready.' \
  '- No Kodi activity is launched by the discovery provider.' \
  > "$ART/CLEAN11-NOTES.txt"
