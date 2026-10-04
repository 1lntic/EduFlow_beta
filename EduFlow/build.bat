@echo off
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build.ps1"
if errorlevel 1 (
    echo.
    echo Budowanie nie powiodlo sie. Sprawdz komunikat wyzej i README.md.
    pause
    exit /b 1
)
pause
