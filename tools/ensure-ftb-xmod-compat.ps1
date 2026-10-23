$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$mods = Join-Path $root 'mods'
$name = 'ftb-xmod-compat-forge-2.1.3.jar'
$target = Join-Path $mods $name
$expectedSha1 = '0584F628D2BC6A612D79DEE368324357EBAE1423'
$officialFile = 'https://www.curseforge.com/minecraft/mc-mods/ftb-xmod-compat/files/6402486'
$download = 'https://edge.forgecdn.net/files/6402/486/ftb-xmod-compat-forge-2.1.3.jar'

New-Item -ItemType Directory -Path $mods -Force | Out-Null
if (Test-Path -LiteralPath $target) {
    if ((Get-FileHash -LiteralPath $target -Algorithm SHA1).Hash -ne $expectedSha1) {
        throw "O arquivo $target existe com hash inesperado. Consulte $officialFile"
    }
} else {
    $temporary = Join-Path $mods ($name + '.download')
    try {
        Invoke-WebRequest -Uri $download -OutFile $temporary -TimeoutSec 120
        if ((Get-FileHash -LiteralPath $temporary -Algorithm SHA1).Hash -ne $expectedSha1) {
            throw "O download nao corresponde ao arquivo oficial: $officialFile"
        }
        Move-Item -LiteralPath $temporary -Destination $target
    } finally {
        if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary }
    }
}
Write-Host "FTB XMod Compat pronto: $name"

$old = Join-Path $mods 'functional-sculptures-forge-1.20.1-1.0.0.jar'
if (Test-Path -LiteralPath $old) {
    $oldSha1 = 'F8F8C3A5E2E50F3D7D3DF3D7E769773189B22A4C'
    if ((Get-FileHash -LiteralPath $old -Algorithm SHA1).Hash -eq $oldSha1) {
        Remove-Item -LiteralPath $old
        Write-Host 'Functional Statues anterior removido.'
    } else {
        Write-Warning "Functional Statues anterior tem hash inesperado e nao foi removido: $old"
    }
}
