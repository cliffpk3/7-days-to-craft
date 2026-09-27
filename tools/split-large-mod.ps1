$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $root "mods\Wesley's+Roguelike+Dungeons+1.20.1-2.3.2.jar"
$part1 = "$jar.part1"
$part2 = "$jar.part2"
$chunkSize = 90MB

if (-not (Test-Path -LiteralPath $jar)) {
    throw "Mod principal do WRD nao encontrado: $jar"
}

$bytes = [System.IO.File]::ReadAllBytes($jar)
$firstSize = [Math]::Min($chunkSize, $bytes.Length)
$first = New-Object byte[] $firstSize
[Array]::Copy($bytes, 0, $first, 0, $firstSize)
[System.IO.File]::WriteAllBytes($part1, $first)

$remaining = $bytes.Length - $firstSize
$second = New-Object byte[] $remaining
if ($remaining -gt 0) {
    [Array]::Copy($bytes, $firstSize, $second, 0, $remaining)
}
[System.IO.File]::WriteAllBytes($part2, $second)

Write-Host "WRD preparado em duas partes para o GitHub."
