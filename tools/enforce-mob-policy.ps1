param([string]$PackRoot = (Split-Path -Parent $PSScriptRoot))

$ErrorActionPreference = 'Stop'
$registry = Get-Content -Raw -LiteralPath (Join-Path $PackRoot 'kubejs\config\7dtc_mob_registry.json') | ConvertFrom-Json
$ecosystems = Get-Content -Raw -LiteralPath (Join-Path $PackRoot 'config\7dtc_cave_ecosystems.json') | ConvertFrom-Json
$depth = Get-Content -Raw -LiteralPath (Join-Path $PackRoot 'kubejs\config\7dtc_depth_matrix.json') | ConvertFrom-Json
$blueprints = Get-Content -Raw -LiteralPath (Join-Path $PackRoot 'config\7dtc_gun_blueprints.json') | ConvertFrom-Json

$ids = [System.Collections.Generic.HashSet[string]]::new()
foreach ($encounter in $depth.encounters) { [void]$ids.Add([string]$encounter.mob) }
foreach ($family in $ecosystems.families) {
    foreach ($encounter in $family.encounters) {
        foreach ($id in $encounter.mobs) { [void]$ids.Add([string]$id) }
    }
}

$errors = [System.Collections.Generic.List[string]]::new()
foreach ($id in $ids) {
    $entry = $registry.mobs.PSObject.Properties[$id]
    if ($null -eq $entry) { $errors.Add("Mob sem registro central: $id"); continue }
    $mob = $entry.Value
    if ($mob.spawn.allow -ne $true) { $errors.Add("Mob de encontro bloqueado no registro: $id") }
    if ([int]$mob.threat.tier -lt 1 -or [int]$mob.threat.tier -gt 4) {
        $errors.Add("Mob sem nivel de ameaca valido para loot: $id")
    }
}

foreach ($tier in 1..4) {
    $entry = $blueprints.PSObject.Properties["tier$tier"]
    if ($null -eq $entry -or [double]$entry.Value.chance -lt 0 -or
        [double]$entry.Value.chance -gt 1 -or @($entry.Value.blueprints).Count -eq 0) {
        $errors.Add("Pool de blueprint invalido: tier$tier")
    }
}

# Advanced Compass gera novos arquivos quando um mod e instalado. O filtro em
# tempo real do 7DTC oculta os icones; isto tambem corrige os arquivos locais.
$compassRoot = Join-Path $PackRoot 'config\AdvancedCompass\mobs'
if (Test-Path -LiteralPath $compassRoot) {
    foreach ($file in Get-ChildItem -LiteralPath $compassRoot -Recurse -Filter 'mobs.json' -File) {
        $raw = [System.IO.File]::ReadAllText($file.FullName)
        if ($file.DirectoryName -eq $compassRoot) {
            $next = [regex]::Replace($raw, '("entity\.(?!minecraft\.player")[^\"]+"\s*:\s*)true', '${1}false')
        } else {
            $next = [regex]::Replace($raw, '("show"\s*:\s*)true', '${1}false')
        }
        if ($next -ne $raw) { [System.IO.File]::WriteAllText($file.FullName, $next) }
        $data = $next | ConvertFrom-Json
        foreach ($property in $data.PSObject.Properties) {
            $visible = if ($property.Value -is [bool]) { $property.Value } else { $property.Value.show }
            if ($visible -and $property.Name -ne 'entity.minecraft.player') {
                $errors.Add("Icone de mob ativo no Advanced Compass: $($file.Name) / $($property.Name)")
            }
        }
    }
}

if ($errors.Count -gt 0) {
    foreach ($error in $errors) { Write-Error $error }
    exit 1
}
Write-Host "Politica de mobs valida: $($ids.Count) entidades de encontro com nivel/loot; icones restritos a players." -ForegroundColor Green
