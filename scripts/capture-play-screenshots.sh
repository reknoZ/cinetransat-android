#!/usr/bin/env bash
set -euo pipefail
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/store-assets"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
mkdir -p "$OUT"

# Prefer physical device
SERIAL=$(adb devices | awk '/\tdevice$/{print $1}' | grep -v emulator | head -1 || true)
if [[ -z "$SERIAL" ]]; then
  SERIAL=$(adb devices | awk '/\tdevice$/{print $1}' | head -1 || true)
fi
if [[ -z "$SERIAL" ]]; then
  echo "No device connected. Plug in your phone with USB debugging on."
  exit 1
fi
echo "Using device: $SERIAL"
ADB=(adb -s "$SERIAL")

"${ADB[@]}" install -r -t "$APK"
"${ADB[@]}" shell pm grant com.heewhack.cinetransat android.permission.POST_NOTIFICATIONS 2>/dev/null || true
"${ADB[@]}" shell am force-stop com.heewhack.cinetransat
"${ADB[@]}" shell am start -n com.heewhack.cinetransat/.MainActivity

W=$("${ADB[@]}" shell wm size | awk '{print $3}' | tr -d '\r' | cut -dx -f1)
H=$("${ADB[@]}" shell wm size | awk '{print $3}' | tr -d '\r' | cut -dx -f2)
NAV_Y=$((H - 100))
echo "size=${W}x${H}"

shot() {
  local name="$1"
  "${ADB[@]}" exec-out screencap -p > "$OUT/$name"
  echo "saved $name ($(wc -c < "$OUT/$name") bytes)"
}

# Wait for content
for i in $(seq 1 30); do
  sleep 2
  # dismiss review / ANR
  "${ADB[@]}" shell input tap $((W*65/100)) $((H*60/100)) 2>/dev/null || true
  "${ADB[@]}" shell input tap $((W/2)) $((H*65/100)) 2>/dev/null || true
  shot _wait.png
  sz=$(wc -c < "$OUT/_wait.png")
  echo "wait $i size=$sz"
  if [[ "$sz" -gt 700000 ]]; then break; fi
done

# Ensure program tab (leftmost common)
N=4
# detect today tab via size jump later; start with program as first or second
"${ADB[@]}" shell input tap $((W/8)) $NAV_Y
sleep 2
# swipe to week 1
for i in 1 2 3 4 5; do
  "${ADB[@]}" shell input swipe $((W/8)) $((H/2)) $((W*7/8)) $((H/2)) 250
  sleep 0.8
done
sleep 3
shot phone-01-program.png

# Open detail - tap top-left poster
"${ADB[@]}" shell input tap $((W/4)) $((H*38/100))
sleep 3
shot phone-02-detail.png
"${ADB[@]}" shell input keyevent KEYCODE_BACK
sleep 2

# Tabs — assume 4 tabs: Program Watchlist Info Settings
"${ADB[@]}" shell input tap $((3*W/8)) $NAV_Y; sleep 2; shot phone-03-watchlist.png
"${ADB[@]}" shell input tap $((5*W/8)) $NAV_Y; sleep 2; shot phone-04-info.png
"${ADB[@]}" shell input tap $((7*W/8)) $NAV_Y; sleep 2; shot phone-05-settings.png

rm -f "$OUT/_wait.png"
ls -lah "$OUT"/phone-*.png
echo "Done. Files in $OUT"
