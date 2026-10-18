$ErrorActionPreference = 'Stop'

$staged = @(git diff --cached --name-only --diff-filter=ACMR)
if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel examinar os arquivos preparados.' }

$forbidden = @($staged | Where-Object {
    $_ -match '^(backups|staging|saves|logs|crash-reports)/' -or
    $_ -match '(^|/)(__pycache__|build)/'
})
if ($forbidden.Count -gt 0) {
    Write-Error ('Arquivos temporarios preparados para publicacao: ' + ($forbidden -join ', '))
    exit 1
}

$awsKeyFiles = @(git grep --cached -I -l -E 'AKIA[0-9A-Z]{16}|ASIA[0-9A-Z]{16}' -- .)
if ($LASTEXITCODE -gt 1) { throw 'Nao foi possivel verificar credenciais nos arquivos preparados.' }
if ($awsKeyFiles.Count -gt 0) {
    Write-Error ('Possiveis credenciais AWS em: ' + ($awsKeyFiles -join ', '))
    exit 1
}
