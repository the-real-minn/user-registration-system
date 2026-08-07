@echo off
REM Double-click this file on Windows to start FIDES ID Management.
setlocal EnableExtensions
cd /d "%~dp0"

set "JAR=target\user-registration-system-0.0.1-SNAPSHOT.jar"
set "URL=http://localhost:8080"

echo ============================================
echo   FIDES ID Management — Starting...
echo ============================================
echo Folder: %CD%
echo.

set "JAVA_BIN="
if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"
)
if not defined JAVA_BIN (
  where java >nul 2>&1
  if not errorlevel 1 set "JAVA_BIN=java"
)

if not defined JAVA_BIN (
  echo ERROR: Java not found.
  echo Install JDK 17+ and add it to PATH, or set JAVA_HOME.
  echo.
  pause
  exit /b 1
)

echo Java: %JAVA_BIN%
"%JAVA_BIN%" -version
echo.

if not exist "%JAR%" (
  echo JAR not found: %JAR%
  echo Building now first time may take a few minutes...
  echo.
  if not exist "mvnw.cmd" (
    echo ERROR: mvnw.cmd not found. Use the module folder that contains pom.xml.
    pause
    exit /b 1
  )
  call mvnw.cmd -DskipTests package
  if errorlevel 1 (
    echo.
    echo ERROR: Build failed.
    pause
    exit /b 1
  )
)

REM Free port 8080 if another instance is listening
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
  echo Port 8080 is busy — stopping PID %%P...
  taskkill /PID %%P /F >nul 2>&1
)

echo Opening browser: %URL%
start "" "%URL%"
echo.
echo App is starting. Keep this window open.
echo Stop: close this window, Ctrl+C, or run Stop FIDES.bat
echo ============================================
echo.

"%JAVA_BIN%" -jar "%JAR%"
set "EXIT_CODE=%ERRORLEVEL%"

echo.
echo App stopped exit %EXIT_CODE%.
pause
exit /b %EXIT_CODE%
