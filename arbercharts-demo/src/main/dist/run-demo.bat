@echo off
setlocal enabledelayedexpansion

set "DIR=%~dp0"
set "JAR="

for %%F in ("%DIR%arbercharts-demo-*.jar") do (
  set "JAR=%%~fF"
  goto :found
)

:found
if "%JAR%"=="" (
  echo Could not find arbercharts-demo-*.jar next to this script.
  exit /b 1
)

java --add-modules jdk.incubator.vector -jar "%JAR%" %*

