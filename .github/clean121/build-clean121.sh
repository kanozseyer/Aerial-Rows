#!/usr/bin/env bash
set -euo pipefail

BASE=/tmp/Projectivy-CLEAN-10-3-Sanitized-TV-Browse-URI.apk
FINAL=/tmp/Projectivy-CLEAN-12-1-Differential-RedLight-Programs.apk
ART=/tmp/clean121-artifact

# CLEAN12.1 is a build-time source delta over frozen CLEAN12. Patch only the
# PreviewProgram reconciliation path, then let the frozen CLEAN10.3 builder
# apply its TV-browse routing patch and compile the complete embedded DEX.
python3 .github/clean121/prepare-clean121.py
bash .github/clean103/build-clean103.sh

test -f "$BASE"
python3 .github/clean121/verify-clean121.py "$BASE"

cp -f "$BASE" "$FINAL"
sha256sum "$FINAL" | tee /tmp/SHA256-clean121.txt

rm -rf "$ART"
mkdir -p "$ART"
cp -f "$FINAL" "$ART/"
cp -f /tmp/SHA256-clean121.txt "$ART/"
cp -f /tmp/apksigner-clean103.txt "$ART/apksigner-clean121.txt"
cp -f /tmp/zipalign-clean103.txt "$ART/zipalign-clean121.txt"
cp -f /tmp/package-clean103.txt "$ART/package-clean121.txt"
cp -f /tmp/manifest-clean103.txt "$ART/manifest-clean121.txt"

printf '%s\n' \
  'CLEAN12.1: differential Red Light PreviewProgram synchronization' \
  '- Frozen CLEAN12 5-minute refresh lifecycle retained.' \
  '- Frozen CLEAN11.1 Kodi retention lease retained.' \
  '- Frozen CLEAN11 cold-native safety gate retained.' \
  '- Existing cards are matched by stable internal_provider_id/plugin URI.' \
  '- Unchanged cards keep the same PreviewProgram row/ID.' \
  '- Changed cards are updated in place.' \
  '- New cards are inserted; only stale/duplicate cards are deleted.' \
  '- Existing artwork is preserved if metadata artwork lookup is temporarily unavailable.' \
  '- PreviewPrograms query failure leaves the cached row untouched.' \
  '- Frozen CLEAN10.3 movie/episode playback and TV-show browse retained.' \
  > "$ART/CLEAN12.1-NOTES.txt"
