#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if command -v adb >/dev/null 2>&1; then
  ADB="$(command -v adb)"
elif [[ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]]; then
  ADB="$HOME/Library/Android/sdk/platform-tools/adb"
else
  SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
  if [[ -n "$SDK_ROOT" && -x "$SDK_ROOT/platform-tools/adb" ]]; then
    ADB="$SDK_ROOT/platform-tools/adb"
  else
    echo "Could not find adb. Install Android SDK Platform Tools or add adb to PATH." >&2
    exit 1
  fi
fi

DEVICE_COUNT="$($ADB devices | awk 'NR>1 && $2=="device" {count++} END {print count+0}')"
if [[ "$DEVICE_COUNT" -ne 1 ]]; then
  echo "Expected exactly one authorized adb device; found $DEVICE_COUNT." >&2
  "$ADB" devices
  exit 1
fi

cd "$ROOT"
echo "Building Aerial Rows Projectivy wallpaper provider…"
./gradlew :aerialrows-wallpaper-provider:assembleDebug

APK="$(find aerialrows-wallpaper-provider/build/outputs/apk/debug -type f -name '*.apk' -print -quit)"
if [[ -z "$APK" ]]; then
  echo "Provider APK was not produced." >&2
  exit 1
fi

echo "Installing Aerial Rows on the connected Android TV device…"
"$ADB" install -r "$APK"

cat <<'EOF'

Aerial Rows wallpaper provider installed.

On Projectivy open:
  Settings → Appearance → Wallpaper → Aerial Rows

Then open Aerial Rows settings to choose any combination of:
  • Apple
  • Amazon
  • Community 1
  • Community 2

All four sources are enabled by default.
EOF
