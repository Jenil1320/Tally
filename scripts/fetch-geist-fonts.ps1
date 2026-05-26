# Fetches Geist + Geist Mono from Vercel's official OFL release into
# app/src/main/assets/fonts/. Run once after cloning; binaries are git-ignored.
#
# Usage:  pwsh ./scripts/fetch-geist-fonts.ps1

$ErrorActionPreference = 'Stop'

$Version = '1.3.0'
$BaseUrl = "https://github.com/vercel/geist-font/releases/download/$Version"
$Dest = Join-Path $PSScriptRoot '..\app\src\main\assets\fonts'

New-Item -ItemType Directory -Force -Path $Dest | Out-Null

$tmp = New-Item -ItemType Directory -Path ([System.IO.Path]::GetTempPath() + [System.Guid]::NewGuid())
try {
    Write-Host "-> Fetching Geist $Version from $BaseUrl"
    Invoke-WebRequest "$BaseUrl/geist-$Version.zip"      -OutFile (Join-Path $tmp 'geist.zip')
    Invoke-WebRequest "$BaseUrl/geist-mono-$Version.zip" -OutFile (Join-Path $tmp 'geist-mono.zip')

    Expand-Archive -Path (Join-Path $tmp 'geist.zip')      -DestinationPath (Join-Path $tmp 'geist')      -Force
    Expand-Archive -Path (Join-Path $tmp 'geist-mono.zip') -DestinationPath (Join-Path $tmp 'geist-mono') -Force

    Get-ChildItem -Path (Join-Path $tmp 'geist')      -Recurse -Filter 'Geist-*.ttf'     | Copy-Item -Destination $Dest -Force
    Get-ChildItem -Path (Join-Path $tmp 'geist-mono') -Recurse -Filter 'GeistMono-*.ttf' | Copy-Item -Destination $Dest -Force

    Write-Host "-> Fonts placed in $Dest"
    Get-ChildItem -Path $Dest -Filter '*.ttf' | ForEach-Object { Write-Host "   $($_.Name)" }
    Write-Host "-> Done. Geist + Geist Mono are now bundled for the next assembleDebug."
}
finally {
    Remove-Item -Recurse -Force $tmp
}
