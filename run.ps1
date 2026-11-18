$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    throw 'Java JDK 11 or newer is required. Install it from https://adoptium.net.'
}

New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot 'out') | Out-Null
$sources = Get-ChildItem -Path (Join-Path $PSScriptRoot 'dsa'), (Join-Path $PSScriptRoot 'backend') -Filter '*.java' -File | ForEach-Object { $_.FullName }
& javac --add-modules jdk.httpserver -d (Join-Path $PSScriptRoot 'out') $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& java --add-modules jdk.httpserver -cp (Join-Path $PSScriptRoot 'out') Main
