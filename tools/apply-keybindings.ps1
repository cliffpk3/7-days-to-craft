$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$sourcePath = Join-Path $root 'config\7dtc-keybindings.txt'
$optionsPath = Join-Path $root 'options.txt'

if (-not (Test-Path -LiteralPath $sourcePath)) {
    throw "Arquivo oficial de atalhos nao encontrado: $sourcePath"
}

$officialLines = @(
    Get-Content -LiteralPath $sourcePath -Encoding UTF8 |
        Where-Object { $_ -match '^key_[^:]+:' }
)
if ($officialLines.Count -eq 0) {
    throw 'O arquivo oficial de atalhos esta vazio ou invalido.'
}

$official = [ordered]@{}
foreach ($line in $officialLines) {
    $separator = $line.IndexOf(':')
    $official[$line.Substring(0, $separator)] = $line
}

$existing = if (Test-Path -LiteralPath $optionsPath) {
    @(Get-Content -LiteralPath $optionsPath -Encoding UTF8)
} else {
    @()
}

$written = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
$result = [System.Collections.Generic.List[string]]::new()
foreach ($line in $existing) {
    if ($line -match '^(key_[^:]+):') {
        $key = $Matches[1]
        if ($official.Contains($key)) {
            $result.Add([string]$official[$key])
            [void]$written.Add($key)
            continue
        }
    }
    $result.Add($line)
}

foreach ($entry in $official.GetEnumerator()) {
    if ($written.Add([string]$entry.Key)) {
        $result.Add([string]$entry.Value)
    }
}

$utf8NoBom = [System.Text.UTF8Encoding]::new($false)
[System.IO.File]::WriteAllLines($optionsPath, $result, $utf8NoBom)
Write-Host ("Atalhos oficiais sincronizados: {0}" -f $official.Count)