#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MOD_DIR="$ROOT/projectivy-mod"
EXPECTED_SHA="a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae"
OFFICIAL_URL="https://github.com/spocky/miproja1/releases/download/4.72/ProjectivyLauncher-4.72-c97-xda-release.apk"
APKTOOL_VERSION="3.0.3"
APKTOOL_SHA="dbf930b076c6b9be08d57c449cacefc3bdd6b71ebd59b3066fc0e1f5b14f9423"
APKTOOL_URL="https://github.com/iBotPeaches/Apktool/releases/download/v${APKTOOL_VERSION}/apktool_${APKTOOL_VERSION}.jar"

CACHE_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/projectivy-liquid-glass"
KEY_DIR="$HOME/.projectivy-liquid-glass"
DIST_DIR="$MOD_DIR/dist"
mkdir -p "$CACHE_DIR" "$KEY_DIR" "$DIST_DIR"
chmod 700 "$KEY_DIR" || true

for tool in java python3 curl keytool openssl; do
  command -v "$tool" >/dev/null 2>&1 || { echo "Missing required tool: $tool" >&2; exit 1; }
done

INPUT_APK="${1:-$CACHE_DIR/ProjectivyLauncher-4.72-c97-xda-release.apk}"
if [[ ! -f "$INPUT_APK" ]]; then
  echo "Downloading official Projectivy 4.72…"
  curl -fL --retry 3 "$OFFICIAL_URL" -o "$INPUT_APK"
fi

ACTUAL_SHA="$(shasum -a 256 "$INPUT_APK" | awk '{print $1}')"
if [[ "$ACTUAL_SHA" != "$EXPECTED_SHA" ]]; then
  echo "Refusing to patch: Projectivy APK SHA-256 does not match official 4.72." >&2
  echo "Expected: $EXPECTED_SHA" >&2
  echo "Actual  : $ACTUAL_SHA" >&2
  exit 1
fi

echo "Verified official Projectivy 4.72: $ACTUAL_SHA"

APKTOOL_JAR="$CACHE_DIR/apktool_${APKTOOL_VERSION}.jar"
if [[ ! -f "$APKTOOL_JAR" ]]; then
  echo "Downloading Apktool $APKTOOL_VERSION…"
  curl -fL --retry 3 "$APKTOOL_URL" -o "$APKTOOL_JAR"
fi
APKTOOL_ACTUAL="$(shasum -a 256 "$APKTOOL_JAR" | awk '{print $1}')"
if [[ "$APKTOOL_ACTUAL" != "$APKTOOL_SHA" ]]; then
  echo "Refusing to run unexpected Apktool binary." >&2
  echo "Expected: $APKTOOL_SHA" >&2
  echo "Actual  : $APKTOOL_ACTUAL" >&2
  exit 1
fi

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK_ROOT" || ! -d "$SDK_ROOT/build-tools" ]]; then
  echo "ANDROID_SDK_ROOT/ANDROID_HOME is not set to an Android SDK with build-tools." >&2
  echo "Android Studio normally installs this for you." >&2
  exit 1
fi

BUILD_TOOLS="$(python3 - "$SDK_ROOT/build-tools" <<'PY'
from pathlib import Path
import re, sys
root=Path(sys.argv[1])
def key(p):
    nums=[int(x) for x in re.findall(r'\d+', p.name)]
    return nums
paths=[p for p in root.iterdir() if p.is_dir() and (p/'zipalign').exists() and (p/'apksigner').exists()]
if not paths:
    raise SystemExit(1)
print(max(paths,key=key))
PY
)"
ZIPALIGN="$BUILD_TOOLS/zipalign"
APKSIGNER="$BUILD_TOOLS/apksigner"

echo "Using Android build-tools: $BUILD_TOOLS"

WORK="$(mktemp -d "${TMPDIR:-/tmp}/projectivy-glass.XXXXXX")"
trap 'rm -rf "$WORK"' EXIT

DECODED="$WORK/decoded"
UNSIGNED="$WORK/projectivy-glass-unsigned.apk"
ALIGNED="$WORK/projectivy-glass-aligned.apk"
OUTPUT="$DIST_DIR/ProjectivyLauncher-4.72-liquid-glass.apk"

echo "Decoding code only while preserving Projectivy resources…"
java -jar "$APKTOOL_JAR" d -f -r "$INPUT_APK" -o "$DECODED" >/dev/null

echo "Applying liquid-glass dock patch…"
python3 "$MOD_DIR/patch_ox3.py" "$DECODED" \
  --fill-alpha "${PROJECTIVY_GLASS_FILL_ALPHA:-96}" \
  --rim-alpha "${PROJECTIVY_GLASS_RIM_ALPHA:-176}" \
  --rim-lighten "${PROJECTIVY_GLASS_RIM_LIGHTEN:-0.45}"

echo "Rebuilding…"
java -jar "$APKTOOL_JAR" b "$DECODED" -o "$UNSIGNED" >/dev/null
"$ZIPALIGN" -f -p 4 "$UNSIGNED" "$ALIGNED"

KEYSTORE="$KEY_DIR/projectivy-liquid-glass.jks"
PASS_FILE="$KEY_DIR/signing.pass"
ALIAS="projectivy-liquid-glass"

if [[ ! -f "$KEYSTORE" ]]; then
  if [[ -e "$PASS_FILE" ]]; then
    echo "Signing password exists but keystore does not. Refusing to replace signing identity." >&2
    exit 1
  fi
  PASS="$(openssl rand -hex 24)"
  printf '%s\n' "$PASS" > "$PASS_FILE"
  chmod 600 "$PASS_FILE"
  echo "Creating your persistent personal APK signing key at $KEYSTORE"
  keytool -genkeypair -noprompt \
    -keystore "$KEYSTORE" \
    -storepass "$PASS" \
    -keypass "$PASS" \
    -alias "$ALIAS" \
    -keyalg RSA -keysize 3072 -validity 10000 \
    -dname "CN=Projectivy Liquid Glass Personal Mod, O=Local Build" >/dev/null
else
  [[ -f "$PASS_FILE" ]] || { echo "Missing signing password file: $PASS_FILE" >&2; exit 1; }
  PASS="$(cat "$PASS_FILE")"
fi

rm -f "$OUTPUT"
"$APKSIGNER" sign \
  --ks "$KEYSTORE" \
  --ks-key-alias "$ALIAS" \
  --ks-pass "pass:$PASS" \
  --key-pass "pass:$PASS" \
  --out "$OUTPUT" \
  "$ALIGNED"

"$APKSIGNER" verify --verbose "$OUTPUT" >/dev/null
OUT_SHA="$(shasum -a 256 "$OUTPUT" | awk '{print $1}')"
printf '%s  %s\n' "$OUT_SHA" "$(basename "$OUTPUT")" > "$OUTPUT.sha256"

cat <<EOF

Built successfully:
  $OUTPUT
  SHA-256: $OUT_SHA

Important:
- This APK is signed by your personal local key, not Projectivy's official certificate.
- Android will not install it over the official Projectivy build. Export/backup Projectivy first.
- This patch does not alter or bypass Projectivy Premium/licensing logic.
- Keep $KEYSTORE and $PASS_FILE safe; future glass builds need the same key to update in-place.
EOF
