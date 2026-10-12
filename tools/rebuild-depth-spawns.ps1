param([string]$PackRoot = (Split-Path -Parent $PSScriptRoot))
$matrix = Get-Content -Raw (Join-Path $PackRoot 'kubejs\config\7dtc_depth_matrix.json') | ConvertFrom-Json
$matrix.encounters | ConvertTo-Json -Depth 20 | Set-Content (Join-Path $PackRoot 'config\incontrol\spawner.json') -Encoding utf8
Write-Host "Depth encounter rules rebuilt from kubejs/config/7dtc_depth_matrix.json"
