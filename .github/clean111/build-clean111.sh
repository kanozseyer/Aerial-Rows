#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-10-3-Sanitized-TV-Browse-URI.apk
FINAL=/tmp/Projectivy-CLEAN-11-1-Kodi-Retention-Lease.apk
ART=/tmp/clean111-artifact

# Rebuild frozen CLEAN10.3 while compiling this branch's CLEAN11.1 provider.
bash .github/clean103/build-clean103.sh

test -f "$BASE"
python3 .github/clean111/verify-clean111.py "$BASE"

cp -f "$BASE" "$FINAL"
sha256sum "$FINAL" | tee /tmp/SHA256-clean111.txt

rm -rf "$ART"
mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean111.txt "$ART/"
cp -f /tmp/apksigner-clean103.txt "$ART/apksigner-clean111.txt"
cp -f /tmp/zipalign-clean103.txt "$ART/zipalign-clean111.txt"
cp -f /tmp/package-clean103.txt "$ART/package-clean111.txt"
cp -f /tmp/manifest-clean103.txt "$ART/manifest-clean111.txt"

printf '%s\n' \
  'CLEAN11.1: Kodi retention lease experiment' \
  '- Frozen CLEAN10.3 movie/episode playback retained.' \
  '- Frozen CLEAN10.3 TV-show browse retained.' \
  '- Frozen CLEAN11 cold-readiness gate retained.' \
  '- Never acquires Kodi provider while native port 9090 is absent.' \
  '- Once Kodi is genuinely ready, Projectivy holds a stable ContentProviderClient.' \
  '- Goal: raise Kodi process importance while Projectivy Home is foreground.' \
  '- No Kodi activity is launched by the discovery provider.' \
  > "$ART/CLEAN11.1-NOTES.txt"
