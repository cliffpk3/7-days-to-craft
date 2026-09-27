@echo off
setlocal
cd /d "%~dp0"
title 7 Days to Craft - Atualizador
set "REPO_URL=https://github.com/cliffpk3/7-days-to-craft.git"

where git >nul 2>nul || (
  echo ERRO: instale o Git for Windows antes de atualizar.
  echo https://git-scm.com/download/win
  pause
  exit /b 1
)

if not exist ".git" (
  echo Preparando o 7 Days to Craft nesta pasta...
  git init || goto :erro
  git branch -M main || goto :erro
  git remote add origin "%REPO_URL%" || goto :erro
) else (
  git remote get-url origin >nul 2>nul || git remote add origin "%REPO_URL%"
)

echo Baixando a versao mais recente...
git fetch origin main || goto :erro
git reset --hard origin/main || goto :erro
powershell -NoProfile -ExecutionPolicy Bypass -File "tools\restore-large-mod.ps1" || goto :erro

echo.
echo 7 Days to Craft atualizado com sucesso.
echo Seus mundos, mapas, waypoints e opcoes pessoais foram preservados.
echo Abrindo o TLauncher...

if exist "%APPDATA%\.minecraft\TLauncher.exe" (
  start "" "%APPDATA%\.minecraft\TLauncher.exe"
) else if exist "%APPDATA%\.tlauncher\TLauncher.exe" (
  start "" "%APPDATA%\.tlauncher\TLauncher.exe"
) else if exist "%LOCALAPPDATA%\Programs\TLauncher\TLauncher.exe" (
  start "" "%LOCALAPPDATA%\Programs\TLauncher\TLauncher.exe"
) else (
  echo AVISO: TLauncher.exe nao foi encontrado automaticamente.
  echo Instale o TLauncher em uma pasta padrao ou abra-o manualmente.
)

pause
exit /b 0

:erro
echo.
echo ERRO: nao foi possivel atualizar. Nenhum save pessoal foi removido.
pause
exit /b 1
