param([string]$PackRoot = (Split-Path -Parent $PSScriptRoot))

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
    if ($rule.conditions.and.maxlight -ne 0 -or $rule.conditions.and.cave -ne $true) {
        throw "Unsafe cave encounter for $($rule.mob): conditions.and must require cave=true and maxlight=0."
    }
}

$encounters | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath $outputPath -Encoding utf8

# Parse the generated file once more so a partial or malformed output never reaches InControl.
$generated = @(Get-Content -Raw -LiteralPath $outputPath | ConvertFrom-Json)
if ($generated.Count -ne $encounters.Count) {
    throw "Generated rule count mismatch: expected $($encounters.Count), got $($generated.Count)."
}

Write-Host "Depth encounter rules rebuilt and validated ($($generated.Count) rules)."
