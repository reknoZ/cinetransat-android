#!/usr/bin/env bash
# Builds a 1024x500 Play Store feature graphic that looks like the program tab.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
POSTERS="$ROOT/app/src/main/res/drawable-nodpi"
OUT="$ROOT/store-assets/play-store-feature-graphic.png"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

CREAM='#FEF9E2'
INK='#1A1A12'
PRIMARY='#2F4FA0'
MUTED='#3E3E30'

poster() {
  local src="$1" out="$2" w="$3" h="$4"
  magick "$src" -resize "${w}x${h}^" -gravity center -extent "${w}x${h}" \
    \( -size "${w}x${h}" xc:none -fill black -draw "roundrectangle 0,0,${w},${h},16,16" \) \
    -alpha set -compose CopyOpacity -composite +repage "$out"
}

# Build a tall phone screen, then crop to the Play banner ratio.
W=1024
H=2200
magick -size "${W}x${H}" "xc:${CREAM}" "$TMP/screen.png"

magick "$TMP/screen.png" \
  -font Helvetica-Bold -pointsize 42 -fill "$INK" \
  -gravity north -annotate +0+72 'CinéTransat 2025' \
  "$TMP/screen.png"

PW=430
PH=645
GAP=36
OX=$(((W - (PW * 2 + GAP)) / 2))
OY=200

poster "$POSTERS/poster_20250710.jpg" "$TMP/p1.png" "$PW" "$PH"
poster "$POSTERS/poster_20250711.jpg" "$TMP/p2.png" "$PW" "$PH"
poster "$POSTERS/poster_20250712.jpg" "$TMP/p3.png" "$PW" "$PH"
poster "$POSTERS/poster_20250713.jpg" "$TMP/p4.png" "$PW" "$PH"

ROW2_Y=$((OY + PH + 56))

magick "$TMP/screen.png" \
  "$TMP/p1.png" -geometry "+${OX}+${OY}" -composite \
  "$TMP/p2.png" -geometry "+$((OX + PW + GAP))+${OY}" -composite \
  "$TMP/p3.png" -geometry "+${OX}+${ROW2_Y}" -composite \
  "$TMP/p4.png" -geometry "+$((OX + PW + GAP))+${ROW2_Y}" -composite \
  -font Helvetica -pointsize 24 -fill "$INK" \
  -gravity northwest -annotate +${OX}+$((OY + PH + 10)) 'Les Bronzés font du ski' \
  -gravity northwest -annotate +$((OX + PW + GAP))+$((OY + PH + 10)) 'Le Vieux qui ne voulait pas fêter son anniversaire' \
  -gravity northwest -annotate +${OX}+$((ROW2_Y + PH + 10)) 'Shaun of the Dead' \
  -gravity northwest -annotate +$((OX + PW + GAP))+$((ROW2_Y + PH + 10)) 'Bottoms' \
  "$TMP/screen.png"

for i in 0 1 2 3; do
  DX=$((W / 2 - 42 + i * 24))
  DY=$((ROW2_Y + PH + 90))
  if [[ "$i" -eq 0 ]]; then
    magick "$TMP/screen.png" -fill "$PRIMARY" -draw "circle ${DX},${DY} $((DX + 8)),${DY}" "$TMP/screen.png"
  else
    magick "$TMP/screen.png" -fill '#1A1A1240' -draw "circle ${DX},${DY} $((DX + 6)),${DY}" "$TMP/screen.png"
  fi
done

NAV_Y=$((H - 140))
magick "$TMP/screen.png" \
  -stroke '#E6DFC8' -strokewidth 2 -draw "line 0,${NAV_Y} ${W},${NAV_Y}" \
  "$TMP/screen.png"

labels=("Program" "Watch List" "Info" "Settings")
for i in "${!labels[@]}"; do
  X=$(((2 * i + 1) * W / 8 - W / 2))
  COLOR=$MUTED
  if [[ "$i" -eq 0 ]]; then COLOR=$PRIMARY; fi
  magick "$TMP/screen.png" \
    -font Helvetica -pointsize 24 -fill "$COLOR" \
    -gravity south -annotate "+${X}+42" "${labels[$i]}" \
    "$TMP/screen.png"
done

# Crop the top of the screen to 1024x500 (title + first row of posters + nav hint).
magick "$TMP/screen.png" -crop "${W}x500+0+0" +repage "$OUT"
magick "$OUT" -quality 95 "$ROOT/store-assets/play-store-feature-graphic.jpg"

echo "Wrote $OUT"
