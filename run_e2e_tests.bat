@echo off
REM =============================================================================
REM Picture Book E2E Test Runner (Windows)
REM =============================================================================
REM This script runs comprehensive end-to-end tests for the Android Picture Book
REM Management System and generates detailed reports with artifacts.
REM
REM Usage:
REM   run_e2e_tests.bat [options]
REM
REM Options:
REM   --all              Run all E2E tests (default)
REM   --quick            Run quick sanity tests only
REM   --flaky            Run flaky test detection (repeat each test 10 times)
REM   --test=NAME        Run specific test class
REM   --help             Show help message
REM =============================================================================

setlocal enabledelayedexpansion

REM Configuration
set PROJECT_DIR=%~dp0
set TEST_REPORT_DIR=%PROJECT_DIR%build\reports\e2e-tests
set TEST_ARTIFACTS_DIR=%PROJECT_DIR%build\test-artifacts
set TEST_RESULTS_XML=%PROJECT_DIR%build\test-results\test\TEST-results.xml

REM Counters
set TESTS_PASSED=0
set TESTS_FAILED=0

REM Colors (Windows 10+)
for /F "tokens=1,2 delims=#" %%a in ('"prompt #$H#$E# & echo on & for %%b in (1) do rem"') do (
  set "DEL=%%a"
  set "COLOR_BLUE=%%b[34m"
  set "COLOR_GREEN=%%b[32m"
  set "COLOR_RED=%%b[31m"
  set "COLOR_YELLOW=%%b[33m"
  set "COLOR_RESET=%%b[0m"
)

REM Helper Functions
:log_info
echo %COLOR_BLUE%[INFO]%COLOR_RESET% %~1
goto :eof

:log_success
echo %COLOR_GREEN%[PASS]%COLOR_RESET% %~1
goto :eof

:log_warning
echo %COLOR_YELLOW%[WARN]%COLOR_RESET% %~1
goto :eof

:log_error
echo %COLOR_RED%[FAIL]%COLOR_RESET% %~1
goto :eof

:show_header
echo ============================================================
echo   Picture Book E2E Test Runner
echo   %date% %time%
echo ============================================================
echo.
goto :eof

:setup_directories
%log_info% Setting up test directories...
if not exist "%TEST_REPORT_DIR%" mkdir "%TEST_REPORT_DIR%"
if not exist "%TEST_ARTIFACTS_DIR%" mkdir "%TEST_ARTIFACTS_DIR%"
if not exist "%PROJECT_DIR%build\test-results\test" mkdir "%PROJECT_DIR%build\test-results\test"
goto :eof

:check_prerequisites
%log_info% Checking prerequisites...
if not exist "%PROJECT_DIR%gradlew.bat" (
    %log_error% Gradle wrapper not found. Make sure you're in the project root.
    exit /b 1
)
%log_success% Prerequisites check passed
goto :eof

:run_all_tests
%log_info% Running all E2E tests...
echo.

call gradlew.bat connectedAndroidTest ^
    --tests "com.maomaochongapp.picturebook.e2e.*" ^
    --info ^
    --stacktrace

if %ERRORLEVEL% EQU 0 (
    %log_success% All E2E tests passed!
    set /a TESTS_PASSED+=1
) else (
    %log_error% Some E2E tests failed!
    set /a TESTS_FAILED+=1
)
goto :eof

:run_quick_tests
%log_info% Running quick sanity tests...
echo.

call gradlew.bat connectedAndroidTest ^
    --tests "com.maomaochongapp.picturebook.e2e.PictureBookCreationE2ETest" ^
    --info

if %ERRORLEVEL% EQU 0 (
    %log_success% Quick sanity tests passed!
    set /a TESTS_PASSED+=1
) else (
    %log_error% Quick sanity tests failed!
    set /a TESTS_FAILED+=1
)
goto :eof

:run_specific_test
set test_class=%1
%log_info% Running specific test: %test_class%...
echo.

call gradlew.bat connectedAndroidTest ^
    --tests "%test_class%" ^
    --info

if %ERRORLEVEL% EQU 0 (
    %log_success% Test %test_class% passed!
    set /a TESTS_PASSED+=1
) else (
    %log_error% Test %test_class% failed!
    set /a TESTS_FAILED+=1
)
goto :eof

:run_flaky_detection
%log_info% Running flaky test detection (10 iterations)...
echo.

set test_classes[0]=com.maomaochongapp.picturebook.e2e.PictureBookCreationE2ETest
set test_classes[1]=com.maomaochongapp.picturebook.e2e.ImageImportDisplayE2ETest
set test_classes[2]=com.maomaochongapp.picturebook.e2e.SearchFunctionalityE2ETest
set test_classes[3]=com.maomaochongapp.picturebook.e2e.BookDeletionE2ETest
set test_classes[4]=com.maomaochongapp.picturebook.e2e.DataPersistenceE2ETest

for %%i in (0,1,2,3,4) do (
    set test_class=!test_classes[%%i]!
    %log_info% Testing !test_class! for flakiness...

    set pass_count=0
    set fail_count=0

    for /l %%j in (1,1,10) do (
        set /p "iteration=  Iteration %%j/10: " <nul

        call gradlew.bat connectedAndroidTest ^
            --tests "!test_class!" ^
            --quiet 2>nul

        if %ERRORLEVEL% EQU 0 (
            echo %COLOR_GREEN%PASS%C%COLOR_RESET%
            set /a pass_count+=1
        ) else (
            echo %COLOR_RED%FAIL%C%COLOR_RESET%
            set /a fail_count+=1
        )
    )

    if !fail_count! GTR 0 (
        %log_warning% !test_class!: !pass_count!/10 passed, !fail_count!/10 failed (FLAKY)
    ) else (
        %log_success% !test_class!: 10/10 passed (STABLE)
    )
    echo.
)
goto :eof

:generate_reports
%log_info% Test reports available at:
%log_info%   - %PROJECT_DIR%build\reports\androidTests\connected\index.html
%log_info%   - %PROJECT_DIR%build\test-results\
goto :eof

:show_summary
echo.
echo ============================================================
echo   Test Run Summary
echo ============================================================
echo   Passed:  %TESTS_PASSED%
echo   Failed:  %TESTS_FAILED%
echo ============================================================

if %TESTS_FAILED% GTR 0 (
    exit /b 1
)
goto :eof

:show_help
echo.
echo Picture Book E2E Test Runner
echo.
echo Usage: run_e2e_tests.bat [options]
echo.
echo Options:
echo   --all              Run all E2E tests (default)
echo   --quick            Run quick sanity tests only
echo   --flaky            Run flaky test detection (10 iterations)
echo   --test=NAME        Run specific test class
echo   --help             Show this help message
echo.
echo Test Flows Covered:
echo   1. Book Creation Flow
echo   2. Image Import Flow
echo   3. Search and Filter Flow
echo   4. Book Deletion Flow
echo   5. Data Persistence Flow
echo.
goto :eof

REM Main Execution
:main
call :show_header

REM Parse arguments
if "%~1"=="" goto run_all_tests
if "%~1"=="--all" goto run_all_tests
if "%~1"=="--quick" goto run_quick_tests
if "%~1"=="--flaky" goto run_flaky_detection
if "%~1"=="--help" goto show_help
if "%~1"=="-h" goto show_help

if "%~1" neq "" (
    if "!substring:~0,7!"=="--test=" (
        set "test_name=%~1"
        set "test_name=!test_name:~7!"
        goto :run_specific_test
    )
)

%log_error% Unknown option: %~1
goto show_help

REM Initialize
call :setup_directories
call :check_prerequisites

REM Run tests based on argument
goto main

REM Cleanup and generate reports
:end
call :generate_reports
call :show_summary

endlocal
exit /b 0
