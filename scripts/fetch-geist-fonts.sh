#!/usr/bin/env bash
# Fetches the Geist + Geist Mono TTFs Tally bundles, from Vercel's official
# repo (SIL OFL 1.1), pinned to a tag for reproducibility. Drops them into
# app/src/main/assets/fonts/. Binaries are git-ignored on purpose
# (see .gitignore + docs/DECISIONS/0001-tech-stack.md).
#
# Run once after cloning; re-run if you change GEIST_TAG.
#
#   ./scripts/fetch-geist-fonts.sh
#
# --ssl-no-revoke is a no-op off Windows; on a corporate Windows network it
# avoids the CRL/OCSP revocation-check failure (see docs/ARCHITECTURE.md).

set -euo pipefail

GEIST_TAG="v1.7.1"
BASE="https://raw.githubusercontent.com/vercel/geist-font/${GEIST_TAG}/fonts"
DEST="$(cd "$(dirname "$0")/.." && pwd)/app/src/main/assets/fonts"

mkdir -p "$DEST"

# Weights the design canvas uses: Geist 300/400/500/600/700, Mono 400/500/600.
FILES=(
  "Geist/ttf/Geist-Light.ttf"
  "Geist/ttf/Geist-Regular.ttf"
  "Geist/ttf/Geist-Medium.ttf"
  "Geist/ttf/Geist-SemiBold.ttf"
  "Geist/ttf/Geist-Bold.ttf"
  "GeistMono/ttf/GeistMono-Regular.ttf"
  "GeistMono/ttf/GeistMono-Medium.ttf"
  "GeistMono/ttf/GeistMono-SemiBold.ttf"
)

echo "→ Fetching Geist ${GEIST_TAG} into ${DEST}"
for f in "${FILES[@]}"; do
  name="$(basename "$f")"
  echo "  · $name"
  curl -fsSL --ssl-no-revoke "$BASE/$f" -o "$DEST/$name"
done

echo "→ Done. Bundled fonts:"
ls -1 "$DEST"
