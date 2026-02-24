@echo off
setlocal EnableExtensions

if "%~1"=="" set "CMD=help"
if not "%~1"=="" set "CMD=%~1"

if /I "%CMD%"=="setup" goto :setup
if /I "%CMD%"=="compile" goto :compile
if /I "%CMD%"=="initdb" goto :initdb
if /I "%CMD%"=="certs" goto :certs
if /I "%CMD%"=="server" goto :server
if /I "%CMD%"=="help" goto :help

goto :help

:setup
if not exist "lib" mkdir lib
if not exist "lib\gson-2.11.0.jar" (
  echo [ERROR] Falta lib\gson-2.11.0.jar
  exit /b 1
)
if not exist "lib\sqlite-jdbc-3.45.0.0.jar" (
  echo [ERROR] Falta lib\sqlite-jdbc-3.45.0.0.jar
  exit /b 1
)
if not exist "lib\slf4j-api-1.7.36.jar" (
  echo [ERROR] Falta lib\slf4j-api-1.7.36.jar
  exit /b 1
)
echo [OK] Dependencias servidor listas.
exit /b 0

:compile
call :setup || exit /b 1
if not exist "bin" mkdir bin
javac -cp "lib/*" -d bin -sourcepath src ^
  src/common/*.java ^
  src/server/model/*.java ^
  src/server/security/*.java ^
  src/server/persistence/*.java ^
  src/server/manager/*.java ^
  src/server/service/NotificationService.java ^
  src/server/*.java
if errorlevel 1 exit /b 1
echo [OK] Servidor compilado.
exit /b 0

:initdb
if not exist "bin\server\DatabaseInit.class" call :compile || exit /b 1
java -cp "lib/*;bin" server.DatabaseInit
exit /b %ERRORLEVEL%

:certs
if not exist "certs" mkdir certs
set "KEYTOOL="
if defined JAVA_HOME (
  set "JAVA_HOME_CLEAN=%JAVA_HOME:"=%"
  if exist "%JAVA_HOME_CLEAN%\bin\keytool.exe" set "KEYTOOL=%JAVA_HOME_CLEAN%\bin\keytool.exe"
)
if not defined KEYTOOL (
  for /f "delims=" %%I in ('where keytool 2^>nul') do (
    set "KEYTOOL=%%I"
    goto :keytool_found
  )
)
if not defined KEYTOOL (
  for /f "delims=" %%I in ('where javac 2^>nul') do (
    if exist "%%~dpIkeytool.exe" (
      set "KEYTOOL=%%~dpIkeytool.exe"
      goto :keytool_found
    )
  )
)
:keytool_found
if not defined KEYTOOL (
  echo [ERROR] No se encontro keytool. Instala un JDK completo o configura JAVA_HOME.
  exit /b 1
)

if exist "certs\server.keystore" del /q "certs\server.keystore"
if exist "certs\server.cer" del /q "certs\server.cer"
if exist "certs\client.truststore" del /q "certs\client.truststore"

"%KEYTOOL%" -genkeypair -alias netauction ^
  -keyalg RSA -keysize 2048 ^
  -validity 365 ^
  -keystore certs/server.keystore ^
  -storepass netauction123 ^
  -dname "CN=localhost, OU=NetAuction, O=PSP, L=Madrid, ST=Madrid, C=ES"
if errorlevel 1 exit /b 1

"%KEYTOOL%" -exportcert -alias netauction ^
  -keystore certs/server.keystore ^
  -storepass netauction123 ^
  -file certs/server.cer
if errorlevel 1 exit /b 1

"%KEYTOOL%" -importcert -alias netauction ^
  -file certs/server.cer ^
  -keystore certs/client.truststore ^
  -storepass netauction123 ^
  -noprompt
if errorlevel 1 exit /b 1

if not exist "..\client-app\certs" mkdir "..\client-app\certs"
copy /Y "certs\client.truststore" "..\client-app\certs\client.truststore" >nul
echo [OK] Certificados generados y truststore sincronizado con client-app.
exit /b 0

:server
if not exist "bin\server\NetAuctionServer.class" call :compile || exit /b 1
set "PORT=9999"
set "SSL_FLAG=--ssl"
if not "%~2"=="" (
  if /I not "%~2"=="--ssl" if /I not "%~2"=="-ssl" set "PORT=%~2"
)
if not exist "certs\server.keystore" call :certs || exit /b 1
if not exist "certs\client.truststore" call :certs || exit /b 1
java -cp "lib/*;bin" server.NetAuctionServer %PORT% %SSL_FLAG%
exit /b %ERRORLEVEL%

:help
echo Uso:
echo   .\run.bat compile
echo   .\run.bat initdb
echo   .\run.bat certs
echo   .\run.bat server [puerto]
exit /b 0
