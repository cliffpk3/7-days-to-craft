$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$mods = Join-Path $root 'mods'
$name = 'functional-sculptures-forge-1.20.1-1.0.0.jar'
$target = Join-Path $mods $name
$expectedSha1 = 'F8F8C3A5E2E50F3D7D3DF3D7E769773189B22A4C'
$officialFile = 'https://www.curseforge.com/minecraft/mc-mods/functional-statues/files/8467610'
$download = 'https://edge.forgecdn.net/files/8467/610/functional-sculptures-forge-1.20.1-1.0.0.jar'

if (Test-Path -LiteralPath $target) {
    if ((Get-FileHash -LiteralPath $target -Algorithm SHA1).Hash -eq $expectedSha1) {
        Write-Host "Functional Statues pronto: $name"
        exit 0
    }
    throw "O arquivo $target existe com hash inesperado. Consulte $officialFile"
}

New-Item -ItemType Directory -Path $mods -Force | Out-Null
$temporary = Join-Path $mods ($name + '.download')
try {
    Invoke-WebRequest -Uri $download -OutFile $temporary -TimeoutSec 120
    if ((Get-FileHash -LiteralPath $temporary -Algorithm SHA1).Hash -ne $expectedSha1) {
        throw "O download nao corresponde ao arquivo oficial: $officialFile"
    }
    Move-Item -LiteralPath $temporary -Destination $target
    Write-Host "Functional Statues instalado do CDN do CurseForge: $name"
} finally {
    if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary }
}
