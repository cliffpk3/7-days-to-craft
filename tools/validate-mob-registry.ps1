$ErrorActionPreference = 'Stop'

$packRoot = Split-Path -Parent $PSScriptRoot
$registryPath = Join-Path $packRoot 'kubejs\config\7dtc_mob_registry.json'
$registry = Get-Content -Raw -LiteralPath $registryPath | ConvertFrom-Json
$errors = [System.Collections.Generic.List[string]]::new()

if ($registry.settings.schemaVersion -ne 1) {
    $errors.Add('settings.schemaVersion deve ser 1.')
}

$attributeNames = @('maxHealth', 'attackDamage', 'armor', 'armorToughness', 'movementSpeed', 'followRange', 'knockbackResistance')
$mobCount = 0

foreach ($property in $registry.mobs.PSObject.Properties) {
    $mobCount++
    $id = $property.Name
    $mob = $property.Value

    if ($id -notmatch '^[a-z0-9_.-]+:[a-z0-9_./-]+$') {
        $errors.Add("ID inválido: $id")
    }

    foreach ($attribute in $attributeNames) {
        $value = $mob.attributes.$attribute
        if ($null -ne $value -and $value -isnot [ValueType]) {
            $errors.Add("$id.attributes.$attribute deve ser número ou null.")
        }
        if ($null -ne $value -and [double]$value -lt 0) {
            $errors.Add("$id.attributes.$attribute não pode ser negativo.")
        }
    }

    $spawn = $mob.spawn
    if ($spawn.allow -isnot [bool]) {
        $errors.Add("$id.spawn.allow deve ser true ou false.")
    }
    if ($null -ne $spawn.minY -and $null -ne $spawn.maxY -and [double]$spawn.minY -gt [double]$spawn.maxY) {
        $errors.Add("$id.spawn.minY não pode ser maior que maxY.")
    }
    if ([double]$spawn.chance -lt 0 -or [double]$spawn.chance -gt 1) {
        $errors.Add("$id.spawn.chance deve estar entre 0.0 e 1.0.")
    }
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Host "Registro central válido: $mobCount mobs catalogados." -ForegroundColor Green
