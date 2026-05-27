@echo off
echo ============================================
echo   Community Microservices — 一键启动
echo ============================================
echo.

cd /d "%~dp0"

echo [1/6] 启动 gateway :8000 ...
start "gateway-8000" cmd /k "cd /d %~dp0 && title gateway-8000 && mvn -pl community-gateway spring-boot:run"

echo [2/6] 启动 user :8086 ...
start "user-8086" cmd /k "cd /d %~dp0 && title user-8086 && mvn -pl community-user spring-boot:run"

echo [3/6] 启动 post :8087 ...
start "post-8087" cmd /k "cd /d %~dp0 && title post-8087 && mvn -pl community-post spring-boot:run"

echo [4/6] 启动 interact :8083 ...
start "interact-8083" cmd /k "cd /d %~dp0 && title interact-8083 && mvn -pl community-interact spring-boot:run"

echo [5/6] 启动 message :8084 ...
start "message-8084" cmd /k "cd /d %~dp0 && title message-8084 && mvn -pl community-message spring-boot:run"

echo [6/6] 启动 search :8085 ...
start "search-8085" cmd /k "cd /d %~dp0 && title search-8085 && mvn -pl community-search spring-boot:run"

echo.
echo 全部启动命令已发出，等待各窗口就绪...
echo Gateway 入口: http://localhost:8000
echo.
pause
