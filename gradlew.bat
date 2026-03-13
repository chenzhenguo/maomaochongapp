@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Default Gradle user home to a writable directory when not set.
@rem In this environment, deletes/renames may be blocked on the workspace drive, which breaks Gradle's temp->cache moves.
@rem Prefer a user-scoped folder under %USERPROFILE%\.codex\memories (writable in Codex CLI), then fall back to project-local.
if not defined GRADLE_USER_HOME (
  set "CODEX_MEMORIES=%USERPROFILE%\.codex\memories"
  if exist "%CODEX_MEMORIES%" (
    set "GRADLE_USER_HOME=%CODEX_MEMORIES%\gradle-user-home"
  ) else (
    set "GRADLE_USER_HOME=%APP_HOME%\.gradle-user-home"
  )
)

@rem Gradle relies on delete/rename for temp->cache moves. If GRADLE_USER_HOME is on a drive
@rem that denies deletes, override it to a known-writable location.
if not exist "%GRADLE_USER_HOME%" mkdir "%GRADLE_USER_HOME%" >NUL 2>&1
set "GH_TEST=%GRADLE_USER_HOME%\.gradle_delete_test.tmp"
echo x>"%GH_TEST%" 2>NUL
del /f /q "%GH_TEST%" >NUL 2>&1
if exist "%GH_TEST%" (
  del /f /q "%GH_TEST%" >NUL 2>&1
  set "CODEX_MEMORIES=%USERPROFILE%\.codex\memories"
  if exist "%CODEX_MEMORIES%" (
    echo.
    echo WARNING: GRADLE_USER_HOME=%GRADLE_USER_HOME% does not allow deletes/renames.
    echo Switching to %CODEX_MEMORIES%\gradle-user-home
    echo.
    set "GRADLE_USER_HOME=%CODEX_MEMORIES%\gradle-user-home"
  ) else (
    set "GRADLE_USER_HOME=%APP_HOME%\.gradle-user-home"
  )
)

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%\bin\java.exe

if exist "%JAVA_EXE%" goto init

@rem If JAVA_HOME is invalid, fall back to java.exe on PATH (helps when JAVA_HOME is stale).
set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" (
echo.
echo WARNING: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo Using 'java' from your PATH instead.
echo.
goto init
)

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@rem Execute Gradle
@rem If a previous wrapper download left a *.zip.part (rename blocked), create the *.zip by copying.
if defined GRADLE_USER_HOME (
  set "DIST_ROOT=%GRADLE_USER_HOME%\wrapper\dists"
  if exist "%DIST_ROOT%" (
    for /r "%DIST_ROOT%" %%F in (*.zip.part) do (
      if not exist "%%~dpnF" (
        copy /b "%%F" "%%~dpnF" >NUL 2>&1
      )
    )
  )
)
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
if not "%ERRORLEVEL%" == "0" (
  if defined GRADLE_USER_HOME (
    set "DIST_ROOT=%GRADLE_USER_HOME%\wrapper\dists"
    if exist "%DIST_ROOT%" (
      for /r "%DIST_ROOT%" %%F in (*.zip.part) do (
        if not exist "%%~dpnF" (
          copy /b "%%F" "%%~dpnF" >NUL 2>&1
        )
      )
    )
  )
  "%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
)

:end
endlocal & set ERRORLEVEL=%ERRORLEVEL%

:fail
if not "" == "%ERRORLEVEL%" exit /b %ERRORLEVEL%
exit /b 1
