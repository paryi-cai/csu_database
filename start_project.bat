:: 一键启动：检查 Docker → 创建数据卷 → 起 MySQL/Redis/Java 后端 → 等健康 → 补齐测试账号 → 起前端
@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
title CSu Database Course Design - Start (Java backend)

echo ==========================================
echo   CSu Database Course Design - Starting
echo   Backend: Spring Boot 3 (backend-java)
echo ==========================================

:: 切换到脚本所在目录（防止路径错误）
cd /d %~dp0

set COMPOSE_PROJECT=csu_db
set SEED_SQL=%~dp0tools\seed_test_accounts.sql

:: 1. 检查 Docker Desktop 是否运行
echo.
echo [1/6] Checking Docker Desktop...
docker info >nul 2>&1
IF ERRORLEVEL 1 (
    echo.
    echo [ERROR] Docker Desktop is not running.
    echo Please start Docker Desktop first, then run this script again.
    pause
    exit /b 1
)
echo       Docker Desktop is running.

:: 2. 首次运行需要的外部数据卷
echo.
echo [2/6] Checking external volume heu_db_mysql_data...
docker volume inspect heu_db_mysql_data >nul 2>&1
IF ERRORLEVEL 1 (
    echo       Volume not found, creating it...
    docker volume create heu_db_mysql_data >nul
    IF ERRORLEVEL 1 (
        echo [ERROR] Failed to create volume heu_db_mysql_data.
        pause
        exit /b 1
    )
)
echo       Volume is ready.

:: 3. 构建并启动 MySQL / Redis / Java 后端
echo.
echo [3/6] Building and starting backend services (this may take a few minutes on first run)...
docker compose -p %COMPOSE_PROJECT% -f backend-java\docker-compose.yml up -d --build
IF ERRORLEVEL 1 (
    echo.
    echo [ERROR] Failed to start Docker containers.
    pause
    exit /b 1
)
echo       Containers are up.

:: 4. 等待 MySQL 健康检查通过（compose 已声明 healthcheck）
echo.
echo [4/6] Waiting for MySQL to become healthy...
set /a WAIT_COUNT=0
:wait_mysql
for /f "tokens=*" %%i in ('docker compose -p %COMPOSE_PROJECT% -f backend-java\docker-compose.yml ps --format "{{.Health}}" mysql 2^>nul') do set HEALTH=%%i
if /i "!HEALTH!"=="healthy" goto mysql_ready
set /a WAIT_COUNT+=1
if !WAIT_COUNT! GEQ 60 (
    echo [ERROR] MySQL did not become healthy in time. Check: docker compose -p %COMPOSE_PROJECT% logs mysql
    pause
    exit /b 1
)
timeout /t 3 /nobreak >nul
goto wait_mysql
:mysql_ready
echo       MySQL is healthy.

:: 5. 幂等补齐测试账号 CMX / YCX
::    说明：database/init.sql 中的示例账号是 iu / mty，与 README 使用的测试账号不一致；
::    为不修改 init.sql，这里在启动后执行一次幂等 SQL 补齐账号。
echo.
echo [5/6] Seeding test accounts (CMX / YCX, idempotent)...
docker compose -p %COMPOSE_PROJECT% -f backend-java\docker-compose.yml exec -T mysql mysql -uroot -p123456 caimingxuan < "%SEED_SQL%"
IF ERRORLEVEL 1 (
    echo [WARN] Failed to seed test accounts automatically.
    echo        You can run it manually:
    echo        docker exec -i %COMPOSE_PROJECT%-mysql-1 mysql -uroot -p123456 caimingxuan ^< tools\seed_test_accounts.sql
) else (
    echo       Test accounts are ready.
)

:: 6. 启动前端（Vue）
echo.
echo [6/6] Starting frontend (Vue)...
if exist frontend\sjk\package.json (
    cd frontend\sjk
    if not exist node_modules (
        echo       node_modules not found, running npm install first...
        call npm install
    )
    start cmd /k npm run serve
    cd /d %~dp0
) else (
    echo [WARN] frontend\sjk not found, skipping frontend.
)

echo.
echo ==========================================
echo   Project is starting successfully!
echo.
echo   Backend API : http://localhost:5000
echo   API docs    : http://localhost:5000/doc.html
echo   Frontend UI : http://localhost:8080
echo.
echo   Test accounts:
echo     CMX / Aa.460734909   (admin)
echo     YCX / aA.460734909   (user)
echo ==========================================
echo.
pause
