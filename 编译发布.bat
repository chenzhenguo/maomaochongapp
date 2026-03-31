@echo off
chcp 65001 >nul
cls
echo.
echo ======================================================================
echo 毛毛虫项目 - 编译发布工具
echo ======================================================================
echo.
echo 项目目录：D:\ideaworkspace\maomaochongapp
echo 版本：1.0.0 (versionCode: 1)
echo.
echo ======================================================================
echo.

cd /d "%~dp0"

:: 检查 Java 环境
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 Java 环境
    echo.
    echo 请安装 JDK 17 或更高版本
    echo 下载：https://adoptium.net/
    echo.
    pause
    exit /b 1
)

echo [OK] Java 环境已安装
java -version
echo.

:: 检查 Gradle
if exist "gradlew.bat" (
    echo [OK] Gradle Wrapper 已就绪
) else (
    echo [错误] 未找到 gradlew.bat
    echo.
    pause
    exit /b 1
)

echo.
echo ======================================================================
echo 编译选项
echo ======================================================================
echo.
echo   1. 清理项目 (clean)
echo   2. 编译 Debug 版本
echo   3. 编译 Release 版本 (推荐)
echo   4. 运行测试
echo   5. 编译并运行测试
echo   6. 打包 APK 并打开目录
echo   0. 退出
echo.
set /p CHOICE="请选择操作 (0-6): "

if "%CHOICE%"=="1" goto :clean
if "%CHOICE%"=="2" goto :debug
if "%CHOICE%"=="3" goto :release
if "%CHOICE%"=="4" goto :test
if "%CHOICE%"=="5" goto :buildtest
if "%CHOICE%"=="6" goto :pack
if "%CHOICE%"=="0" goto :end

echo 无效选项
pause
goto :end

:clean
echo.
echo ======================================================================
echo 清理项目
echo ======================================================================
echo.
call gradlew clean
echo.
echo 清理完成
echo.
pause
goto :end

:debug
echo.
echo ======================================================================
echo 编译 Debug 版本
echo ======================================================================
echo.
call gradlew assembleDebug
if errorlevel 1 (
    echo.
    echo [错误] 编译失败
    echo.
    pause
    exit /b 1
)
echo.
echo Debug APK 位置：app\build\outputs\apk\debug\app-debug.apk
echo.
pause
goto :end

:release
echo.
echo ======================================================================
echo 编译 Release 版本
echo ======================================================================
echo.
echo 正在编译 Release 版本...
echo 这可能需要几分钟...
echo.
call gradlew assembleRelease
if errorlevel 1 (
    echo.
    echo [错误] 编译失败
    echo 请检查错误信息
    echo.
    pause
    exit /b 1
)
echo.
echo ======================================================================
echo 编译成功！
echo ======================================================================
echo.
echo Release APK 位置:
echo   app\build\outputs\apk\release\app-release.apk
echo.
echo APK 信息:
echo   版本：1.0.0
echo   versionCode: 1
echo   已混淆：是
echo   已压缩资源：是
echo.
pause
goto :end

:test
echo.
echo ======================================================================
echo 运行测试
echo ======================================================================
echo.
call gradlew test
echo.
echo 测试报告位置：app\build\reports\tests\testDebugUnitTest\
echo.
pause
goto :end

:buildtest
echo.
echo ======================================================================
echo 编译并运行测试
echo ======================================================================
echo.
call gradlew build
if errorlevel 1 (
    echo.
    echo [错误] 编译或测试失败
    echo.
    pause
    exit /b 1
)
echo.
echo 构建成功！
echo.
pause
goto :end

:pack
echo.
echo ======================================================================
echo 打包 APK 并打开目录
echo ======================================================================
echo.
echo 正在编译 Release 版本...
call gradlew assembleRelease
if errorlevel 1 (
    echo.
    echo [错误] 编译失败
    echo.
    pause
    exit /b 1
)

echo.
echo 打开 APK 输出目录...
start explorer "%CD%\app\build\outputs\apk\release"

echo.
echo APK 文件位置:
echo   %CD%\app\build\outputs\apk\release\app-release.apk
echo.
pause
goto :end

:end
echo.
echo 再见！
echo.
