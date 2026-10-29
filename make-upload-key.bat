@echo off
setlocal
cd /d "%~dp0"
echo This creates a Google Play upload key. Back up both files and the password.
set /p ALIAS=Key alias [irvoider]:
if "%ALIAS%"=="" set ALIAS=irvoider
set /p STOREPASS=Keystore password (min 6 chars):
set /p KEYPASS=Key password (Enter = same as keystore):
if "%KEYPASS%"=="" set KEYPASS=%STOREPASS%
if not exist keys mkdir keys
keytool -genkeypair -v -keystore keys\irvoider-upload.jks -alias %ALIAS% -keyalg RSA -keysize 4096 -validity 10000 -storepass %STOREPASS% -keypass %KEYPASS% -dname "CN=IR Voider, O=poging kyle, C=US"
if errorlevel 1 exit /b 1
(
echo storeFile=../keys/irvoider-upload.jks
echo storePassword=%STOREPASS%
echo keyAlias=%ALIAS%
echo keyPassword=%KEYPASS%
) > keystore.properties
echo.
echo Wrote keys\irvoider-upload.jks and keystore.properties (both gitignored).
echo Back them up. Without them you cannot update this Play listing.
endlocal
