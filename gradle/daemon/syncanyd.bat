@if "%DEBUG%" == "" @echo off

set APP_NAME=syncanyd
set APP_USER_DIR=%AppData%\Syncany
set APP_DAEMON_CONTROL=%APP_USER_DIR%\daemon.ctrl
set APP_LOG_DIR=%APP_USER_DIR%\logs
set APP_LOG_FILE=%APP_LOG_DIR%\daemon.log

if not exist "%APP_USER_DIR%" mkdir "%APP_USER_DIR%"
if not exist "%APP_LOG_DIR%" mkdir "%APP_LOG_DIR%"

@if "%1" == "start" goto start
@if "%1" == "stop" goto stop
@if "%1" == "reload" goto reload
@if "%1" == "status" goto status

echo Usage: syncanyd (start/stop/reload/status)
goto mainEnd

:stop
if exist %APP_DAEMON_CONTROL% (
  echo shutdown >> %APP_DAEMON_CONTROL%
  echo Stopping daemon: %APP_NAME%.
) else (
  echo Stopping daemon: %APP_NAME% not running
)
goto mainEnd

:reload
if exist %APP_DAEMON_CONTROL% (
  echo reload >> %APP_DAEMON_CONTROL%
  echo Reloading daemon: %APP_NAME%.
) else (
  echo Reloading daemon: %APP_NAME% not running
)
goto mainEnd

:status
if exist %APP_DAEMON_CONTROL% (
  echo Checking daemon: %APP_NAME% running
) else (
  echo Checking daemon: %APP_NAME% not running
)
goto mainEnd

:start
@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Add default JVM options here. You can also use JAVA_OPTS and SYNCANY_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx1024m" "-Dfile.encoding=utf-8"

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=javaw.exe
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
set JAVA_EXE=%JAVA_HOME%/bin/javaw.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\*;%AppData%\Syncany\plugins\*

@rem Execute syncany
echo Starting daemon: %APP_NAME%.
start "" /b "%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% -classpath "%CLASSPATH%" org.syncany.Syncany --log=%APP_LOG_FILE% daemon

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable SYNCANY_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%SYNCANY_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
