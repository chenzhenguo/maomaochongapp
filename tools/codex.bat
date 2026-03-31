@echo off
chcp 65001 >nul
REM ======================================================================
REM Codex 命令执行脚本 - 毛毛虫项目
REM ======================================================================
REM 
REM 用法：codex [命令] [参数...]
REM 
REM 示例:
REM   codex plan          - 进入计划模式
REM   codex build         - 构建项目
REM   codex test          - 运行测试
REM   codex run [task]    - 执行任务
REM   codex help          - 显示帮助
REM ======================================================================

set PROJECT_DIR=%~dp0..
cd /d "%PROJECT_DIR%"

REM 检查 codex 是否可用
where codex >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 codex 命令
    echo.
    echo 请确保 codex 已安装并添加到 PATH
    echo 或使用以下方式:
    echo   1. npx @anthropic-ai/codex [命令]
    echo   2. 安装 codex CLI: npm install -g @anthropic-ai/codex
    echo.
    goto :use_npx
)

REM 执行 codex 命令
codex %*

goto :end

:use_npx
echo 尝试使用 npx 运行...
echo.
npx @anthropic-ai/codex %*

:end
pause
