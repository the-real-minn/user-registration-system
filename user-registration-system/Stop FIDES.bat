@echo off
REM Double-click to stop FIDES on Windows (frees port 8080).
setlocal EnableExtensions
cd /d "%~dp0"

echo Stopping FIDES on port 8080...
set "FOUND=0"
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
  set "FOUND=1"
  echo Stopping PID %%P
  taskkill /PID %%P /F >nul 2>&1
)

if "%FOUND%"=="0" (
  echo Nothing was running on port 8080.
) else (
  echo Stopped.
)
echo.
pause
