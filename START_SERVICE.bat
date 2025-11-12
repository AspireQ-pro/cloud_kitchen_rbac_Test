@echo off
echo Starting Cloud Kitchen RBAC Service...
echo.

cd /d "%~dp0"

echo Checking Java installation...
java -version
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    pause
    exit /b 1
)

echo.
echo Starting Spring Boot application on port 8081...
echo.

mvn spring-boot:run

pause
