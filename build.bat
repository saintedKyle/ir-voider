@echo off
setlocal
cd /d "%~dp0"
call gradlew.bat :app:assembleDebug --console=plain
if errorlevel 1 exit /b 1
if not exist dist mkdir dist
copy /Y app\build\outputs\apk\debug\app-debug.apk dist\IRVoider-1.0.apk
echo.
echo APK: %CD%\dist\IRVoider-1.0.apk
endlocal
