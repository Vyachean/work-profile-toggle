@echo off
setlocal

set APP_HOME=%~dp0
set PROPERTIES_FILE=%APP_HOME%gradle\wrapper\gradle-wrapper.properties

if not exist "%PROPERTIES_FILE%" (
  echo Missing %PROPERTIES_FILE% 1>&2
  exit /b 1
)

for /f "tokens=2 delims==" %%A in ('findstr /b "distributionUrl=" "%PROPERTIES_FILE%"') do set DISTRIBUTION_URL=%%A
set DISTRIBUTION_URL=%DISTRIBUTION_URL:\:=:%

for %%A in ("%DISTRIBUTION_URL%") do set ZIP_NAME=%%~nxA
set GRADLE_VERSION=%ZIP_NAME:gradle-=%
set GRADLE_VERSION=%GRADLE_VERSION:-bin.zip=%

if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set CACHE_DIR=%GRADLE_USER_HOME%\wrapper\manual-dists
set DIST_DIR=%CACHE_DIR%\gradle-%GRADLE_VERSION%
set ZIP_FILE=%CACHE_DIR%\gradle-%GRADLE_VERSION%-bin.zip
set GRADLE_BIN=%DIST_DIR%\bin\gradle.bat

if exist "%GRADLE_BIN%" goto runGradle

if not exist "%CACHE_DIR%" mkdir "%CACHE_DIR%"

if not exist "%ZIP_FILE%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%ZIP_FILE%'"
  if errorlevel 1 exit /b 1
)

if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP_FILE%' -DestinationPath '%CACHE_DIR%' -Force"
if errorlevel 1 exit /b 1

:runGradle
call "%GRADLE_BIN%" %*
