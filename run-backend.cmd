@echo off
setlocal
title SignageTV - Backend (Spring Boot)
cd /d "%~dp0backend"

REM Variables de entorno para desarrollo local (usar comillas para que & no rompa)
set "SPRING_PROFILES_ACTIVE=dev"
set "DB_URL=jdbc:mysql://localhost:3306/signagetv?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true"
set "DB_USER=root"
set "DB_PASS=root"
set "JWT_SECRET=dev-only-secret-key-please-change-in-production-must-be-at-least-32-bytes"
set "STORAGE_PATH=%~dp0data\media"
set "PUBLIC_BASE_URL=http://localhost:8080"
set "CORS_ORIGINS=http://localhost:5173,http://localhost:3000"

echo.
echo === SignageTV Backend ===
echo Storage:  %STORAGE_PATH%
echo Profile:  %SPRING_PROFILES_ACTIVE%
echo.
echo Primera ejecucion: Maven descargara dependencias (3-8 min).
echo Cuando veas "Started SignageTvApplication" -^> backend listo en http://localhost:8080
echo.

call mvnw.cmd spring-boot:run
echo.
echo === Backend detenido. Pulsa una tecla para cerrar. ===
pause
