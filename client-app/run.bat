@echo off
setlocal EnableExtensions

if "%~1"=="" set "CMD=help"
if not "%~1"=="" set "CMD=%~1"

if /I "%CMD%"=="setup" goto :setup
if /I "%CMD%"=="truststore" goto :truststore
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
  if exist "..\server-app\certs\server.cer" (
    copy /Y "..\server-app\certs\server.cer" "certs\server.cer" >nul
  )
)
if not exist "certs\truststore.p12" (
  call :truststore || exit /b 1
)
if not exist "certs\truststore.p12" (
  echo [ERROR] Falta certs\truststore.p12.
  echo         Debes disponer de certs\server.cer para generarlo localmente.
  echo         Si trabajas en el mismo repo: cd ..\server-app ^&^& .\run.bat certs
  exit /b 1
)
echo [OK] Material SSL cliente listo.
exit /b 0

:truststore
if not exist "certs" mkdir certs
if not exist "certs\server.cer" (
  echo [ERROR] Falta certs\server.cer.
  echo         Incluye el certificado publico del servidor en client-app\certs\server.cer.
  exit /b 1
)
set "KEYTOOL="
if defined JAVA_HOME (
  set "JAVA_HOME_CLEAN=%JAVA_HOME:"=%"
  if exist "%JAVA_HOME_CLEAN%\bin\keytool.exe" set "KEYTOOL=%JAVA_HOME_CLEAN%\bin\keytool.exe"
)
if not defined KEYTOOL (
  for /f "delims=" %%I in ('where keytool 2^>nul') do (
    set "KEYTOOL=%%I"
    goto :client_keytool_found
  )
)
if not defined KEYTOOL (
  for /f "delims=" %%I in ('where javac 2^>nul') do (
    if exist "%%~dpIkeytool.exe" (
      set "KEYTOOL=%%~dpIkeytool.exe"
      goto :client_keytool_found
    )
  )
)
:client_keytool_found
if not defined KEYTOOL (
  echo [ERROR] No se encontro keytool. Instala un JDK completo o configura JAVA_HOME.
  exit /b 1
)
if exist "certs\truststore.p12" del /q "certs\truststore.p12"
"%KEYTOOL%" -importcert -alias servidor ^
  -file certs/server.cer ^
  -keystore certs/truststore.p12 ^
  -storetype PKCS12 ^
  -storepass netauction123 ^
  -noprompt
if errorlevel 1 exit /b 1
echo [OK] Truststore local generado desde certs\server.cer.
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
echo   .\run.bat truststore
echo   .\run.bat compile
echo   .\run.bat client [host] [puerto]
echo   .\run.bat gui
exit /b 0
