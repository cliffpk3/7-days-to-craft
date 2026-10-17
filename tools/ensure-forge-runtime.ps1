$ErrorActionPreference = 'Stop'

$minecraftRoot = Join-Path $env:APPDATA '.minecraft'
$forgeVersion = '1.20.1-47.4.10'
$forgeLibrary = Join-Path $minecraftRoot "libraries\net\minecraftforge\forge\$forgeVersion"
$clientJar = Join-Path $forgeLibrary "forge-$forgeVersion-client.jar"
$universalJar = Join-Path $forgeLibrary "forge-$forgeVersion-universal.jar"

if ((Test-Path -LiteralPath $clientJar) -and (Test-Path -LiteralPath $universalJar)) {
    Write-Host "Forge $forgeVersion ja instalado."
    exit 0
}

$java = Join-Path $minecraftRoot 'runtime\java-runtime-gamma\windows\java-runtime-gamma\bin\java.exe'
if (-not (Test-Path -LiteralPath $java)) {
    $java = (Get-Command java.exe -ErrorAction Stop).Source
}

$installerName = "forge-$forgeVersion-installer.jar"
$installer = Join-Path $env:TEMP $installerName
$expectedSha1 = '66BFEA9963BFA60D88BAB6B2750E74A958392715'
if (-not (Test-Path -LiteralPath $installer) -or
    (Get-FileHash -LiteralPath $installer -Algorithm SHA1).Hash -ne $expectedSha1) {
    $url = "https://maven.minecraftforge.net/net/minecraftforge/forge/$forgeVersion/$installerName"
    Write-Host "Baixando Forge $forgeVersion do Maven oficial..."
    Invoke-WebRequest -Uri $url -OutFile $installer
}
if ((Get-FileHash -LiteralPath $installer -Algorithm SHA1).Hash -ne $expectedSha1) {
    throw 'O hash do instalador do Forge nao confere.'
}

Write-Host "Instalando Forge $forgeVersion..."
& $java -jar $installer --installClient $minecraftRoot
if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $clientJar) -or
    -not (Test-Path -LiteralPath $universalJar)) {
    throw "A instalacao do Forge $forgeVersion nao foi concluida."
}
Write-Host "Forge $forgeVersion pronto."
