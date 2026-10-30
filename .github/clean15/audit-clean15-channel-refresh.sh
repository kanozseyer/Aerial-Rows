#!/usr/bin/env bash
set -euo pipefail

OUT=/tmp/clean15-audit
DOWNLOAD=/tmp/clean14-approved
OUTER="$DOWNLOAD/clean14-artifact.zip"
UNPACKED=/tmp/clean14-approved-unpacked
APK="$UNPACKED/clean14-artifact/Projectivy-CLEAN-14-Universal-Carousel.apk"
DECODED=/tmp/clean14-decoded
APKTOOL=/tmp/apktool-clean15.jar
EXPECTED=06a5ed63040a61dd3705f7fb9e3e0b6eef4393e129d00d92eaef2b4781fb092d

# actions/download-artifact has already extracted the GitHub artifact envelope.
# Keep that downloaded directory intact; it contains the nested build archive.
rm -rf "$OUT" "$DECODED" "$UNPACKED"
mkdir -p "$OUT" "$UNPACKED"
test -s "$OUTER" || { echo "Missing downloaded build archive: $OUTER"; find "$DOWNLOAD" -maxdepth 3 -type f -printf '%p %s bytes\\n'; exit 1; }
unzip -t "$OUTER" > "$OUT/approved-artifact-zip-test.txt"
unzip -q "$OUTER" -d "$UNPACKED"
test -s "$APK" || { echo "Missing CLEAN14 APK at: $APK"; find "$UNPACKED" -maxdepth 4 -type f -printf '%p %s bytes\\n'; exit 1; }
ACTUAL="$(sha256sum "$APK" | awk '{print $1}')"
test "$ACTUAL" = "$EXPECTED"
printf '%s\n' "$ACTUAL" > "$OUT/approved-CLEAN14-sha256.txt"

curl -fL --retry 3 'https://github.com/iBotPeaches/Apktool/releases/download/v2.11.1/apktool_2.11.1.jar' -o "$APKTOOL"
java -jar "$APKTOOL" d -f "$APK" -o "$DECODED" > "$OUT/apktool-decode.txt" 2>&1

# Preserve the exact smali for the left rail, row model, TV cache and row refresh path.
mkdir -p "$OUT/smali"
for name in w12 r12 w40 v40 e70 i5 m60 jo4 f23 hv ui2 eb3; do
  while IFS= read -r file; do
    [ -n "$file" ] || continue
    rel="${file#"$DECODED"/}"
    dest="$OUT/smali/${rel//\//__}"
    cp "$file" "$dest"
  done < <(find "$DECODED" -type f -name "$name.smali" | sort)
done
while IFS= read -r file; do
  [ -n "$file" ] || continue
  rel="${file#"$DECODED"/}"
  dest="$OUT/smali/${rel//\//__}"
  cp "$file" "$dest"
done < <(find "$DECODED" -type f -iname '*MainFragment*.smali' | sort)

# Include layout XML for the left rail/category header and any related drawable names.
mkdir -p "$OUT/resources"
find "$DECODED/res" -type f \( -path '*/layout*/*' -o -path '*/drawable*/*' \) \
  | while IFS= read -r f; do
      if grep -qiE 'reorder|category|sidebar|settings|header|refresh' "$f"; then
        rel="${f#"$DECODED/res/}"
        dest="$OUT/resources/${rel//\//__}"
        cp "$f" "$dest"
      fi
    done

# A concise index of methods and call sites relevant to refresh and channel identity.
{
  echo "CLEAN15 CHANNEL-ROW REFRESH SOURCE AUDIT"
  echo "APK SHA256: $ACTUAL"
  echo
  echo "===== EXTRACTED SMALI FILES ====="
  find "$OUT/smali" -type f -maxdepth 1 -printf '%f\n' | sort
  echo
  echo "===== RESOURCE FILES ====="
  find "$OUT/resources" -type f -maxdepth 1 -printf '%f\n' | sort
  echo
  echo "===== CHANNEL / TV PROVIDER / REFRESH REFERENCES ====="
  grep -RniE 'PreviewPrograms|PreviewChannels|buildProgramsUriForChannel|channel_id|clear\(\)|refresh|invalidate|TvContract|requestChannelBrowsable|w40;->|Lw40;' \
    "$DECODED"/smali* > "$OUT/channel-refresh-references.txt" || true
  sed -n '1,1200p' "$OUT/channel-refresh-references.txt"
} > "$OUT/CLEAN15-AUDIT.txt"

# Save the exact classes and XML as a compact downloadable audit artifact.
cd /tmp
rm -f CLEAN15-CHANNEL-ROW-REFRESH-AUDIT.zip
zip -qr CLEAN15-CHANNEL-ROW-REFRESH-AUDIT.zip clean15-audit
echo "AUDIT=$OUT/CLEAN15-AUDIT.txt"
echo "ARTIFACT=/tmp/CLEAN15-CHANNEL-ROW-REFRESH-AUDIT.zip"
