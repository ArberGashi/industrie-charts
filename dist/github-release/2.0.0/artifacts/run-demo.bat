@echo off
setlocal
set "DIR=%~dp0"
set "JAR=%DIR%arbercharts-demo-2.0.0.jar"

if not exist "%JAR%" (
  echo Demo JAR nicht gefunden: %JAR%
  exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
  echo Java nicht gefunden. Bitte Java 25 installieren.
  exit /b 1
)

for /f "tokens=3 delims=.\"" %%a in ('java -version 2^>^&1 ^| findstr /i "version"') do set MAJOR=%%a
if "%MAJOR%"=="" (
  echo Java Version konnte nicht gelesen werden.
  java -version
  exit /b 1
)

if %MAJOR% LSS 25 (
  echo Gefundene Java-Version ist zu alt:
  java -version
  echo ArberCharts Demo 2.0.0 benötigt Java 25 oder höher.
  exit /b 1
)

java --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.vector -jar "%JAR%"
endlocal
