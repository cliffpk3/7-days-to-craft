@echo off
setlocal
cd /d "%~dp0"
title 7 Days to Craft - Publicar atualizacao

where git >nul 2>nul || (
  echo ERRO: Git nao esta instalado ou nao esta no PATH.
  pause
  exit /b 1
)

git rev-parse --is-inside-work-tree >nul 2>nul || (
  echo ERRO: esta pasta ainda nao e um repositorio Git.
  pause
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "tools\split-large-mod.ps1" || goto :erro
git add -A

git diff --cached --quiet && (
  echo Nenhuma mudanca do modpack para publicar.
  pause
  exit /b 0
)

echo.
echo Arquivos preparados:
git status --short
echo.

for /f %%D in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd"') do set "DATA_ATUAL=%%D"
if not defined DATA_ATUAL goto :erro

set "ATUALIZACOES_HOJE=0"
for /f %%V in ('git rev-list --all --count --grep^="^%DATA_ATUAL% - v[0-9][0-9]*$"') do set "ATUALIZACOES_HOJE=%%V"
set /a PROXIMA_VERSAO=ATUALIZACOES_HOJE+1

if %PROXIMA_VERSAO% LSS 10 (
  set "VERSAO_FORMATADA=0%PROXIMA_VERSAO%"
) else (
  set "VERSAO_FORMATADA=%PROXIMA_VERSAO%"
)

set "MENSAGEM=%DATA_ATUAL% - v%VERSAO_FORMATADA%"
echo Descricao automatica: %MENSAGEM%
echo.

git commit -m "%MENSAGEM%" || goto :erro
git push -u origin main || goto :erro

echo.
echo Atualizacao publicada com sucesso.
pause
exit /b 0

:erro
echo.
echo ERRO: a atualizacao nao foi publicada. O commit local foi preservado.
pause
exit /b 1
