@echo off
chcp 65001 >nul
cls
echo.
echo ======================================================================
echo 毛毛虫项目 - Codex 执行工具
echo ======================================================================
echo.
echo 项目目录：D:\ideaworkspace\maomaochongapp
echo.
echo 可用命令:
echo   plan [任务名]  - 创建任务计划
echo   build         - 构建项目
echo   test          - 运行测试
echo   clean         - 清理项目
echo   help          - 显示帮助
echo.
echo ======================================================================
echo.

cd /d "%~dp0"

:menu
echo 请选择操作:
echo.
echo   1. 创建任务计划 (plan)
echo   2. 构建项目 (build)
echo   3. 运行测试 (test)
echo   4. 清理项目 (clean)
echo   5. 运行 Codex 命令
echo   6. 查看帮助
echo   0. 退出
echo.
set /p CHOICE="请输入选项 (0-6): "

if "%CHOICE%"=="1" goto :plan
if "%CHOICE%"=="2" goto :build
if "%CHOICE%"=="3" goto :test
if "%CHOICE%"=="4" goto :clean
if "%CHOICE%"=="5" goto :codex
if "%CHOICE%"=="6" goto :help
if "%CHOICE%"=="0" goto :end

echo 无效选项，请重新输入
echo.
goto :menu

:plan
set /p TASKNAME="请输入任务名称："
python tools/codex.py plan %TASKNAME%
echo.
echo 任务计划已创建，现在可以运行 codex 进入计划模式
echo.
goto :menu

:build
echo 正在构建项目...
echo.
call gradlew build
echo.
goto :menu

:test
echo 正在运行测试...
echo.
call gradlew test
echo.
goto :menu

:clean
echo 正在清理项目...
echo.
call gradlew clean
echo.
goto :menu

:codex
set /p CMD=请输入 codex 命令：
codex %CMD%
if errorlevel 1 (
    echo.
    echo [提示] codex 命令未找到，尝试使用 npx...
    echo.
    npx @anthropic-ai/codex %CMD%
)
echo.
goto :menu

:help
python tools/codex.py help
echo.
goto :menu

:end
echo.
echo 再见！
echo.
pause
