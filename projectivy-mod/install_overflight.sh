#!/usr/bin/env bash
set -euo pipefail

VERSION="1.20"
EXPECTED_SHA="949b27eed4cc2a213d68d39e499cc1037c972dfa8a8cbdd9f659fe28f58daa4b"
URL="https://github.com/spocky/projectivy-plugin-wallpaper-overflight/releases/download/${VERSION}/ProjectivyPlugin-Overflight.apk"
CACHE_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/projectivy-liquid-glass"
APK="$CACHE_DIR/ProjectivyPlugin-Overflight-${VERSION}.apk"
mkdir -p "$CACHE_DIR"

if ! command -v adb >/dev/null 2>&1; then
  SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
  if [[ -z "$SDK_ROOT" && -d "$HOME/Library/Android/sdk" ]]; then
    SDK_ROOT="$HOME/Library/Android/sdk"
  fi
  if [[ -n "$SDK_ROOT" && -x "$SDK_ROOT/platform-tools/adb" ]]; then
    ADB="$SDK_ROOT/platform-tools/adb"
  else
    echo "Could not find adb. Install Android SDK Platform Tools or add adb to PATH." >&2
    exit 1
  fi
else
  ADB="$(command -v adb)"
fi

if [[ ! -f "$APK" ]]; then
  echo "Downloading official Projectivy Overflight ${VERSION}…"
  curl -fL --retry 3 "$URL" -o "$APK"
fi

ACTUAL_SHA="$(shasum -a 256 "$APK" | awk '{print $1}')"
if [[ "$ACTUAL_SHA" != "$EXPECTED_SHA" ]]; then
  echo "Refusing to install unexpected Overflight APK." >&2
  echo "Expected: $EXPECTED_SHA" >&2
  echo "Actual  : $ACTUAL_SHA" >&2
  exit 1
fi

echo "Verified official Overflight ${VERSION}: $ACTUAL_SHA"

DEVICE_COUNT="$($ADB devices | awk 'NR>1 && $2=="device" {count++} END {print count+0}')"
if [[ "$DEVICE_COUNT" -ne 1 ]]; then
  echo "Expected exactly one authorized adb device; found $DEVICE_COUNT." >&2
  "$ADB" devices
  exit 1
fi

"$ADB" install -r "$APK"
cat <<'EOF'

Overflight is installed.
On the Shield open:
  Projectivy Settings → Appearance → Wallpaper → Overflight

Then open Overflight's provider settings from Projectivy if you want to filter the Aerial catalogue by keywords.
EOF
