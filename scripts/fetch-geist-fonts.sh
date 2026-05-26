#!/usr/bin/env bash
# Fetches Geist + Geist Mono from Vercel's official OFL release into
# app/src/main/assets/fonts/. Run once after cloning; the binaries are
# git-ignored deliberately (see .gitignore + docs/DECISIONS/0001-tech-stack.md).
#
# Re-run any time you bump versions in libs.versions.toml.

set -euo pipefail

VERSION="1.3.0"
BASE_URL="https://github.com/vercel/geist-font/releases/download/${VERSION}"
DEST="$(dirname "$0")/../app/src/main/assets/fonts"

mkdir -p "$DEST"

declare -A FILES=(
  ["Geist-Regular.ttf"]="geist-1.3.0.zip"
  ["Geist-Medium.ttf"]="geist-1.3.0.zip"
  ["Geist-SemiBold.ttf"]="geist-1.3.0.zip"
  ["GeistMono-Regular.ttf"]="geist-mono-1.3.0.zip"
  ["GeistMono-Medium.ttf"]="geist-mono-1.3.0.zip"
)

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

echo "→ Fetching Geist ${VERSION} from ${BASE_URL}"
curl -fL "${BASE_URL}/geist-${VERSION}.zip" -o "$TMP/geist.zip"
curl -fL "${BASE_URL}/geist-mono-${VERSION}.zip" -o "$TMP/geist-mono.zip"

unzip -qo "$TMP/geist.zip" -d "$TMP/geist"
unzip -qo "$TMP/geist-mono.zip" -d "$TMP/geist-mono"

# The Vercel zips ship under fonts/ttf/ — flatten into assets/fonts/.
find "$TMP/geist" -name 'Geist-*.ttf' -exec cp {} "$DEST/" \;
find "$TMP/geist-mono" -name 'GeistMono-*.ttf' -exec cp {} "$DEST/" \;

echo "→ Fonts placed in $DEST:"
ls -1 "$DEST"

echo "→ Done. Geist + Geist Mono are now bundled for the next assembleDebug."
