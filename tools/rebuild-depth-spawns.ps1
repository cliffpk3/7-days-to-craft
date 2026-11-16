param([string]$PackRoot = (Split-Path -Parent $PSScriptRoot))

# 7DTC: regenerates config/incontrol/spawner.json from kubejs/config/7dtc_depth_matrix.json (the single source).
# 16/11/2026: always writes a JSON array (Windows PowerShell 5.1 wrote a lone object when the matrix had one rule, which
# broke the update), validates before writing (a failure never leaves a broken file) and accepts structure-anchored
# rules (mineshaft zombie/runner, maxlight <= 4) besides cave rules (cave=true, maxlight=0).
$matrixPath = Join-Path $PackRoot 'kubejs\config\7dtc_depth_matrix.json'
$outputPath = Join-Path $PackRoot 'config\incontrol\spawner.json'
$matrix = Get-Content -Raw -LiteralPath $matrixPath | ConvertFrom-Json
$encounters = @($matrix.encounters)

if ($encounters.Count -eq 0) {
    throw 'Depth matrix contains no encounters.'
}

foreach ($rule in $encounters) {
    if (-not $rule.mob -or -not $rule.conditions.dimension) {
        throw "Invalid depth encounter: every rule needs mob and conditions.dimension."
    }
    if ($rule.conditions.PSObject.Properties.Name -contains 'maxlight') {
        throw "Invalid InControl 9.5.0 rule for $($rule.mob): maxlight belongs in conditions.and."
    }
    if ([int]$rule.conditions.mindist -lt 32) {
        throw "Unsafe encounter distance for $($rule.mob): mindist must be at least 32 blocks."
    }
    $and = $rule.conditions.and
    $cave = $and.cave -eq $true -and $and.maxlight -eq 0
    $structure = $and.structure -and $null -ne $and.maxlight -and [int]$and.maxlight -le 4
    if (-not ($cave -or $structure)) {
        throw "Unsafe encounter for $($rule.mob): conditions.and needs cave=true with maxlight=0, or a structure with maxlight <= 4."
    }
}

$json = ConvertTo-Json -InputObject @($encounters) -Depth 30
# Parse once more so a partial or malformed output never reaches InControl.
$generated = @((ConvertFrom-Json -InputObject $json) | ForEach-Object { $_ })  # enumerate: 5.1 returns the array as one object
if (-not $json.TrimStart().StartsWith('[') -or $generated.Count -ne $encounters.Count) {
    throw "Generated rule count mismatch: expected $($encounters.Count), got $($generated.Count)."
}
# UTF-8 without BOM (InControl's JSON reader does not expect one).
[System.IO.File]::WriteAllText($outputPath, $json, (New-Object System.Text.UTF8Encoding $false))

Write-Host "Depth encounter rules rebuilt and validated ($($encounters.Count) rules)."
