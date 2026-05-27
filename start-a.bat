@echo off
cd /d "%~dp0"

echo ==========================================
echo   Machine A — 启动本地服务
echo ==========================================
echo.

echo [1/3] 启动 user :8086 ...
start "user-8086" cmd /k "cd /d %~dp0 && mvn -pl community-user spring-boot:run"

echo [2/3] 启动 post :8087 ...
start "post-8087" cmd /k "cd /d %~dp0 && mvn -pl community-post spring-boot:run"

echo [3/3] 启动 gateway :8000 ...
start "gateway-8000" cmd /k "cd /d %~dp0 && mvn -pl community-gateway spring-boot:run"

echo.
echo Gateway: http://localhost:8000
echo.
pause
