@echo off
REM start_all.bat - Start all components on Windows

echo ==========================================
echo Load Balancer System Startup
echo ==========================================
echo.

REM Check if compiled
if not exist BackendManager.class (
    echo Compiling Java files...
    javac *.java
    if errorlevel 1 (
        echo X Compilation failed!
        pause
        exit /b 1
    )
    echo + Compilation successful
    echo.
)

REM Start backends
echo Starting Backend Servers...
start "Backend Manager" javaw BackendManager
echo + Backend Manager started
timeout /t 3 /nobreak > nul

REM Start load balancer
echo Starting Load Balancer...
start "Load Balancer" javaw LoadBalancer
echo + Load Balancer started
timeout /t 2 /nobreak > nul

REM Start dashboard
echo Starting Dashboard GUI...
start "Dashboard" javaw DashboardApp
echo + Dashboard started

echo.
echo ==========================================
echo System Started Successfully!
echo ==========================================
echo.
echo Components:
echo   - Backend Servers: localhost:8081-8084
echo   - Load Balancer: localhost:8080
echo   - Dashboard: GUI window should be visible
echo.
echo To test:
echo   java ClientSimulator 10 single
echo   java ClientSimulator 5 continuous
echo.
echo To stop all:
echo   stop_all.bat
echo.
echo Logs: app.log
echo ==========================================
echo.
pause
