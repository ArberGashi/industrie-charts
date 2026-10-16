@echo off
setlocal

set "DIR=%~dp0"
set "JAR=%DIR%@DEMO_JAR@"
if not exist "%JAR%" (
  echo Could not find @DEMO_JAR@ next to this script.
  exit /b 1
)

java --add-modules jdk.incubator.vector -jar "%JAR%" %*

