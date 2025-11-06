@echo off
REM stop_all.bat - Stop all components on Windows

echo ==========================================
echo Stopping Load Balancer System
echo ==========================================
echo.

echo Stopping all components...

taskkill /F /FI "WINDOWTITLE eq Backend Manager*" > nul 2>&1
if %errorlevel%==0 echo + Backend Manager stopped

taskkill /F /FI "WINDOWTITLE eq Load Balancer*" > nul 2>&1
if %errorlevel%==0 echo + Load Balancer stopped

taskkill /F /FI "WINDOWTITLE eq Dashboard*" > nul 2>&1
if %errorlevel%==0 echo + Dashboard stopped

taskkill /F /FI "WINDOWTITLE eq Client*" > nul 2>&1
if %errorlevel%==0 echo + Client Simulator stopped

echo.
echo ==========================================
echo All processes stopped
echo ==========================================
echo.
pause
