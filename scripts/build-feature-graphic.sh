#!/usr/bin/env bash
# Builds a 1024x500 Play Store feature graphic (2026 navy/pink + week 1 posters).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
POSTERS="$ROOT/store-assets/posters-2026"
OUT="$ROOT/store-assets/play-store-feature-graphic.png"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

NAVY='#192B59'
NAVY_SOFT='#263A6C'
PINK='#DC82A5'
TITLE='#FFF8FA'
MUTED='#B0B8CC'

poster() {
  local src="$1" out="$2" w="$3" h="$4"
  magick "$src" -resize "${w}x${h}^" -gravity center -extent "${w}x${h}" \
    \( -size "${w}x${h}" xc:none -fill black -draw "roundrectangle 0,0,${w},${h},16,16" \) \
    -alpha set -compose CopyOpacity -composite +repage "$out"
}

W=1024
H=2200
magick -size "${W}x${H}" "xc:${NAVY}" "$TMP/screen.png"

magick "$TMP/screen.png" \
  -font Helvetica-Bold -pointsize 42 -fill "$TITLE" \
  -gravity north -annotate +0+72 'Programme 2026' \
  "$TMP/screen.png"

PILL_W=300
PILL_H=48
PILL_X=$(((W - PILL_W) / 2))
PILL_Y=130
magick "$TMP/screen.png" \
  -fill "$NAVY_SOFT" -draw "roundrectangle ${PILL_X},${PILL_Y},$((PILL_X + PILL_W)),$((PILL_Y + PILL_H)),24,24" \
  -font Helvetica-Bold -pointsize 22 -fill "$TITLE" \
  -gravity north -annotate +0+142 'Week 1 • 9-12 July' \
  "$TMP/screen.png"

PW=430
PH=645
GAP=36
OX=$(((W - (PW * 2 + GAP)) / 2))
OY=210

poster "$POSTERS/back-to-the-future.jpg" "$TMP/p1.png" "$PW" "$PH"
poster "$POSTERS/la-famille-belier.jpg" "$TMP/p2.png" "$PW" "$PH"
poster "$POSTERS/billy-elliot.jpg" "$TMP/p3.png" "$PW" "$PH"
poster "$POSTERS/flow.jpg" "$TMP/p4.png" "$PW" "$PH"

ROW2_Y=$((OY + PH + 56))

magick "$TMP/screen.png" \
  "$TMP/p1.png" -geometry "+${OX}+${OY}" -composite \
  "$TMP/p2.png" -geometry "+$((OX + PW + GAP))+${OY}" -composite \
  "$TMP/p3.png" -geometry "+${OX}+${ROW2_Y}" -composite \
  "$TMP/p4.png" -geometry "+$((OX + PW + GAP))+${ROW2_Y}" -composite \
  "$TMP/screen.png"

# Crop top to 1024x500
magick "$TMP/screen.png" -crop "${W}x500+0+0" +repage -depth 8 "$OUT"
magick "$OUT" -quality 95 "$ROOT/store-assets/play-store-feature-graphic.jpg"

echo "Wrote $OUT"
echo "Wrote $ROOT/store-assets/play-store-feature-graphic.jpg"
