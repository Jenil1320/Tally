# Fetches the Geist + Geist Mono TTFs Tally bundles, from Vercel's official
# repo (SIL OFL 1.1), pinned to a tag for reproducibility. Drops them into
# app/src/main/assets/fonts/. Binaries are git-ignored on purpose.
#
# Run once after cloning; re-run if you change $GeistTag.
#
#   pwsh ./scripts/fetch-geist-fonts.ps1   (or: powershell -File scripts/fetch-geist-fonts.ps1)
#
# Uses curl.exe --ssl-no-revoke so it works behind the corporate TLS proxy
# (Windows PowerShell's Invoke-WebRequest truncates large files).

$ErrorActionPreference = 'Stop'

$GeistTag = 'v1.7.1'
$Base = "https://raw.githubusercontent.com/vercel/geist-font/$GeistTag/fonts"
$Dest = Join-Path $PSScriptRoot '..\app\src\main\assets\fonts'
New-Item -ItemType Directory -Force -Path $Dest | Out-Null

# Weights the design canvas uses: Geist 300/400/500/600/700, Mono 400/500/600.
$files = @(
    'Geist/ttf/Geist-Light.ttf',
    'Geist/ttf/Geist-Regular.ttf',
    'Geist/ttf/Geist-Medium.ttf',
    'Geist/ttf/Geist-SemiBold.ttf',
    'Geist/ttf/Geist-Bold.ttf',
    'GeistMono/ttf/GeistMono-Regular.ttf',
    'GeistMono/ttf/GeistMono-Medium.ttf',
    'GeistMono/ttf/GeistMono-SemiBold.ttf'
)

Write-Host "-> Fetching Geist $GeistTag into $Dest"
foreach ($f in $files) {
    $name = Split-Path $f -Leaf
    Write-Host "   . $name"
    & curl.exe -fsSL --ssl-no-revoke "$Base/$f" -o (Join-Path $Dest $name)
    if ($LASTEXITCODE -ne 0) { throw "Failed to download $f" }
}

Write-Host "-> Done. Bundled fonts:"
Get-ChildItem -Path $Dest -Filter '*.ttf' | ForEach-Object { Write-Host "   $($_.Name)" }
