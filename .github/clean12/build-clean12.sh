#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-10-3-Sanitized-TV-Browse-URI.apk
FINAL=/tmp/Projectivy-CLEAN-12-Throttled-RedLight-Refresh.apk
ART=/tmp/clean12-artifact

# Rebuild the frozen CLEAN10.3 payload while compiling this branch's CLEAN12
# RedLightDiscoveryProvider into classes4.dex. The Red Light engine itself is
# intentionally unchanged in CLEAN12.
bash .github/clean103/build-clean103.sh

test -f "$BASE"
python3 .github/clean12/verify-clean12.py "$BASE"

cp -f "$BASE" "$FINAL"
sha256sum "$FINAL" | tee /tmp/SHA256-clean12.txt

rm -rf "$ART"
mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean12.txt "$ART/"
cp -f /tmp/apksigner-clean103.txt "$ART/apksigner-clean12.txt"
cp -f /tmp/zipalign-clean103.txt "$ART/zipalign-clean12.txt"
cp -f /tmp/package-clean103.txt "$ART/package-clean12.txt"
cp -f /tmp/manifest-clean103.txt "$ART/manifest-clean12.txt"

printf '%s\n' \
  'CLEAN12: throttled repeat Red Light refresh lifecycle' \
  '- Frozen CLEAN10.3 movie/episode playback retained.' \
  '- Frozen CLEAN10.3 TV-show browse retained.' \
  '- Frozen CLEAN11 cold-native readiness gate retained.' \
  '- Frozen CLEAN11.1 Kodi ContentProvider retention lease retained.' \
  '- Initial successful sync no longer unregisters the Home lifecycle callback.' \
  '- Later Home resumes can refresh Red Light after a 5-minute freshness window.' \
  '- Home resumes inside the 5-minute window are throttled without touching Kodi provider data.' \
  '- Kodi is never launched or foregrounded by discovery.' \
  '- RedLightDiscoveryEngine programme write behaviour is intentionally unchanged in CLEAN12.' \
  > "$ART/CLEAN12-NOTES.txt"
