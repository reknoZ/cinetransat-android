#!/usr/bin/env bash
# Builds a 1024×500 Play Store feature graphic from a real app screenshot.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SHOT="${1:-$ROOT/store-assets/phone-01-program.png}"
OUT="$ROOT/store-assets/play-store-feature-graphic.png"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

NAVY='#192B59'
PINK='#DC82A5'
TITLE='#FFF8FA'

if [[ ! -f "$SHOT" ]]; then
  echo "Missing screenshot: $SHOT" >&2
  exit 1
fi

# Phone frame sized for the banner (shows program grid, trims status/nav chrome lightly).
PHONE_W=300
PHONE_H=460
RADIUS=28
FRAME=6

# Crop mid-screen content (drop status bar + bottom nav), then fit into the phone.
magick "$SHOT" \
  -gravity North -chop 0x72 \
  -gravity South -chop 0x140 \
  -resize "${PHONE_W}x${PHONE_H}^" -gravity center -extent "${PHONE_W}x${PHONE_H}" \
  "$TMP/screen-raw.png"

magick "$TMP/screen-raw.png" \
  \( -size "${PHONE_W}x${PHONE_H}" xc:none \
     -fill white -draw "roundrectangle 0,0 $((PHONE_W - 1)),$((PHONE_H - 1)) ${RADIUS},${RADIUS}" \) \
  -alpha set -compose DstIn -composite \
  "$TMP/screen.png"

OUTER_W=$((PHONE_W + FRAME * 2))
OUTER_H=$((PHONE_H + FRAME * 2))
OUTER_R=$((RADIUS + 4))
magick -size "${OUTER_W}x${OUTER_H}" xc:none \
  -fill '#0E1833' -draw "roundrectangle 0,0 $((OUTER_W - 1)),$((OUTER_H - 1)) ${OUTER_R},${OUTER_R}" \
  "$TMP/bezel.png"
magick "$TMP/bezel.png" "$TMP/screen.png" -gravity center -compose over -composite "$TMP/phone.png"

# Soft pink glow behind the phone
magick -size 1024x500 "xc:${NAVY}" \
  \( -size 420x500 radial-gradient:"${PINK}-#192B5900" -channel A -evaluate multiply 0.22 +channel \) \
  -gravity east -geometry +40+0 -compose over -composite \
  "$TMP/bg.png"

# Brand lockup (matches splash: pink name on navy)
magick "$TMP/bg.png" \
  -font Helvetica-Bold -pointsize 64 -fill "$PINK" \
  -gravity West -annotate +72-36 'CinéTransat' \
  -font Helvetica -pointsize 28 -fill "$TITLE" \
  -gravity West -annotate +72+36 'Outdoor cinema · Geneva' \
  "$TMP/phone.png" -gravity East -geometry +56+0 -compose over -composite \
  -depth 8 "$OUT"

magick "$OUT" -quality 92 "$ROOT/store-assets/play-store-feature-graphic.jpg"

echo "Wrote $OUT"
echo "Wrote $ROOT/store-assets/play-store-feature-graphic.jpg"
