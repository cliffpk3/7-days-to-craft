$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $root "mods\Wesley's+Roguelike+Dungeons+1.20.1-2.3.2.jar"
$part1 = "$jar.part1"
$part2 = "$jar.part2"

if (-not (Test-Path -LiteralPath $part1) -or -not (Test-Path -LiteralPath $part2)) {
    throw "Partes do mod WRD nao foram encontradas."
}

$output = [System.IO.File]::Open($jar, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
try {
    foreach ($part in @($part1, $part2)) {
        $input = [System.IO.File]::OpenRead($part)
        try { $input.CopyTo($output) } finally { $input.Dispose() }
    }
} finally {
    $output.Dispose()
}

Write-Host "Mod WRD restaurado com sucesso."
