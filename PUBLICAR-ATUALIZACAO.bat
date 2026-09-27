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
set /p "MENSAGEM=Descricao desta atualizacao: "
if not defined MENSAGEM set "MENSAGEM=Atualizacao 7 Days to Craft %date% %time%"

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
