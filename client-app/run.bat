@echo off
setlocal EnableExtensions
cd /d "%~dp0"

if "%~1"=="" set "CMD=help"
if not "%~1"=="" set "CMD=%~1"

if /I "%CMD%"=="setup" goto :setup
if /I "%CMD%"=="compile" goto :compile
if /I "%CMD%"=="client" goto :client
if /I "%CMD%"=="gui" goto :gui
if /I "%CMD%"=="help" goto :help

goto :help

:setup
if not exist "lib" mkdir lib
if not exist "lib\gson-2.11.0.jar" (
  echo [ERROR] Falta lib\gson-2.11.0.jar
  exit /b 1
)
if not exist "certs" mkdir certs
echo [OK] Dependencias cliente listas.
exit /b 0

:prepare_ssl
if not exist "certs\server.cer" (
  echo [ERROR] Falta certs\server.cer.
  echo         El cliente necesita el certificado publico versionado del servidor.
  exit /b 1
)
echo [OK] Material SSL cliente listo.
exit /b 0

:compile
call :setup || exit /b 1
if not exist "bin" mkdir bin
javac -cp "lib/*" -d bin -sourcepath src ^
  src/common/*.java ^
  src/client/*.java
if errorlevel 1 exit /b 1
if defined JAVAFX_HOME (
  javac -cp "lib/*;bin" -d bin ^
    --module-path "%JAVAFX_HOME%\lib" ^
    --add-modules javafx.controls,javafx.fxml ^
    src/client/gui/*.java
  if errorlevel 1 exit /b 1
)
echo [OK] Cliente compilado.
exit /b 0

:client
if not exist "bin\client\NetAuctionClient.class" call :compile || exit /b 1
call :prepare_ssl || exit /b 1
set "HOST=localhost"
set "PORT=9999"
if not "%~2"=="" set "HOST=%~2"
if not "%~3"=="" set "PORT=%~3"
java -cp "lib/*;bin" client.NetAuctionClient %HOST% %PORT% --ssl
exit /b %ERRORLEVEL%

:gui
if not exist "bin\client\gui\MainApp.class" call :compile || exit /b 1
call :prepare_ssl || exit /b 1
if not defined JAVAFX_HOME (
  echo [ERROR] JAVAFX_HOME no esta definido.
  exit /b 1
)
java -cp "lib/*;bin" ^
  --module-path "%JAVAFX_HOME%\lib" ^
  --add-modules javafx.controls,javafx.fxml ^
  client.gui.MainApp
exit /b %ERRORLEVEL%

:help
echo Uso:
echo   .\run.bat compile
echo   .\run.bat client [host] [puerto]
echo   .\run.bat gui
exit /b 0
