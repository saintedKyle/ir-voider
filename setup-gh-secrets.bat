@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo ============================================
echo  IR Voider - one-time GitHub build setup
echo ============================================
echo This reads your local upload key and stores it
echo as encrypted GitHub Actions secrets. It never
echo prints the key or passwords.
echo.

where gh >nul 2>&1
if errorlevel 1 (
  echo GitHub CLI not found. Install it first: winget install GitHub.cli
  pause
  exit /b 1
)

gh auth status >nul 2>&1
if errorlevel 1 (
  echo Not logged in. Run: gh auth login
  pause
  exit /b 1
)

if not exist "keys\irvoider-upload.jks" (
  echo Missing keys\irvoider-upload.jks
  pause
  exit /b 1
)
if not exist "keystore.properties" (
  echo Missing keystore.properties
  pause
  exit /b 1
)

for /f "tokens=1,* delims==" %%A in ('findstr /b "storePassword=" keystore.properties') do set "STOREPASS=%%B"
for /f "tokens=1,* delims==" %%A in ('findstr /b "keyAlias=" keystore.properties') do set "KEYALIAS=%%B"
for /f "tokens=1,* delims==" %%A in ('findstr /b "keyPassword=" keystore.properties') do set "KEYPASS=%%B"

if "%STOREPASS%"=="" (
  echo Could not read storePassword from keystore.properties
  pause
  exit /b 1
)

echo Uploading encrypted secrets...
powershell -NoProfile -Command "[Convert]::ToBase64String([IO.File]::ReadAllBytes('keys/irvoider-upload.jks'))" | gh secret set KEYSTORE_BASE64
if errorlevel 1 goto :failed
echo %STOREPASS%| gh secret set KEYSTORE_PASSWORD
if errorlevel 1 goto :failed
echo %KEYALIAS%| gh secret set KEY_ALIAS
if errorlevel 1 goto :failed
echo %KEYPASS%| gh secret set KEY_PASSWORD
if errorlevel 1 goto :failed

echo.
echo Secrets set. Triggering a test build...
gh workflow run release.yml
if errorlevel 1 (
  echo Could not trigger the workflow. Open the Actions tab and run it manually.
) else (
  echo Build triggered. Open: https://github.com/saintedKyle/ir-voider/actions
)
echo.
echo Done. After this build succeeds, the PC can be off.
pause
exit /b 0

:failed
echo.
echo Failed to set secrets. Check gh auth and repository permissions.
pause
exit /b 1
