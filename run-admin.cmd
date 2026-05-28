@echo off
title SignageTV - Admin Panel (React + Vite)
cd /d "%~dp0admin-panel"

if not exist node_modules (
  echo === Instalando dependencias npm (primera vez, ~2 min)... ===
  call npm install
  if errorlevel 1 (
    echo.
    echo npm install fallo. Revisa el error arriba.
    pause
    exit /b 1
  )
)

if not exist .env (
  echo VITE_API_URL=http://localhost:8080> .env
  echo VITE_WS_URL=http://localhost:8080>> .env
)

echo.
echo === Arrancando admin en http://localhost:5173 ===
echo.
call npm run dev
pause
