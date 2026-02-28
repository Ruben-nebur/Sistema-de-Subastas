@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"

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
set "CERT_MODE=%~2"
if exist "certs\ca.p12" if exist "certs\ca.cer" if exist "certs\servidor.p12" if exist "certs\server.cer" (
  if /I not "%CERT_MODE%"=="--force" if /I not "%CERT_MODE%"=="--reset-ca" (
    echo [OK] Certificados existentes reutilizados.
    exit /b 0
  )
)
call :find_keytool || exit /b 1
if not defined KEYTOOL (
  echo [ERROR] No se encontro keytool. Instala un JDK completo o configura JAVA_HOME.
  exit /b 1
)

if /I "%CERT_MODE%"=="--reset-ca" (
  if exist "certs\ca.p12" del /q "certs\ca.p12"
  if exist "certs\ca.cer" del /q "certs\ca.cer"
)
if exist "certs\servidor.p12" del /q "certs\servidor.p12"
if exist "certs\server.cer" del /q "certs\server.cer"
if exist "certs\server.csr" del /q "certs\server.csr"
if exist "certs\truststore.p12" del /q "certs\truststore.p12"

set "SAN_ENTRIES=dns:localhost,dns:%COMPUTERNAME%,ip:127.0.0.1"
for /f "tokens=2 delims=:" %%I in ('ipconfig ^| findstr /R /C:"IPv4.*:.*"') do (
  for /f "tokens=* delims= " %%J in ("%%I") do (
    if not "%%J"=="127.0.0.1" (
      echo !SAN_ENTRIES! | find /I "ip:%%J" >nul
      if errorlevel 1 set "SAN_ENTRIES=!SAN_ENTRIES!,ip:%%J"
    )
  )
)

if not exist "certs\ca.p12" (
  "%KEYTOOL%" -genkeypair -alias netauction-ca ^
    -keyalg RSA -keysize 2048 ^
    -validity 3650 ^
    -keystore certs/ca.p12 ^
    -storetype PKCS12 ^
    -storepass netauction123 ^
    -keypass netauction123 ^
    -dname "CN=NetAuction CA, OU=NetAuction, O=PSP, L=Madrid, ST=Madrid, C=ES" ^
    -ext bc:c
  if errorlevel 1 exit /b 1
)

"%KEYTOOL%" -exportcert -alias netauction-ca ^
  -keystore certs/ca.p12 ^
  -storetype PKCS12 ^
  -storepass netauction123 ^
  -rfc ^
  -file certs/ca.cer
if errorlevel 1 exit /b 1

"%KEYTOOL%" -genkeypair -alias servidor ^
  -keyalg RSA -keysize 2048 ^
  -validity 825 ^
  -keystore certs/servidor.p12 ^
  -storetype PKCS12 ^
  -storepass netauction123 ^
  -keypass netauction123 ^
  -dname "CN=%COMPUTERNAME%, OU=NetAuction, O=PSP, L=Madrid, ST=Madrid, C=ES"
if errorlevel 1 exit /b 1

"%KEYTOOL%" -certreq -alias servidor ^
  -keystore certs/servidor.p12 ^
  -storetype PKCS12 ^
  -storepass netauction123 ^
  -file certs/server.csr
if errorlevel 1 exit /b 1

"%KEYTOOL%" -gencert -alias netauction-ca ^
  -keystore certs/ca.p12 ^
  -storetype PKCS12 ^
  -storepass netauction123 ^
  -keypass netauction123 ^
  -infile certs/server.csr ^
  -outfile certs/server.cer ^
  -rfc ^
  -validity 825 ^
  -ext "KU=digitalSignature,keyEncipherment" ^
  -ext "EKU=serverAuth" ^
  -ext "SAN=%SAN_ENTRIES%"
if errorlevel 1 exit /b 1

"%KEYTOOL%" -importcert -alias netauction-ca ^
  -file certs/ca.cer ^
  -keystore certs/servidor.p12 ^
  -storetype PKCS12 ^
  -storepass netauction123 ^
  -noprompt
if errorlevel 1 exit /b 1

"%KEYTOOL%" -importcert -alias servidor ^
  -file certs/server.cer ^
  -keystore certs/servidor.p12 ^
  -storetype PKCS12 ^
  -storepass netauction123 ^
  -noprompt
if errorlevel 1 exit /b 1

if exist "certs\truststore.p12" del /q "certs\truststore.p12"
"%KEYTOOL%" -importcert -alias netauction-ca ^
  -file certs/ca.cer ^
  -keystore certs/truststore.p12 ^
  -storetype PKCS12 ^
  -storepass netauction123 ^
  -noprompt
if errorlevel 1 exit /b 1

if exist "certs\server.csr" del /q "certs\server.csr"
echo [OK] CA y certificado de servidor generados. El cliente debe confiar en certs\ca.cer.
exit /b 0

:find_keytool
set "KEYTOOL="
if defined JAVA_HOME (
  set "JAVA_HOME_CLEAN=%JAVA_HOME:"=%"
  if exist "%JAVA_HOME_CLEAN%\bin\keytool.exe" set "KEYTOOL=%JAVA_HOME_CLEAN%\bin\keytool.exe"
)
if not defined KEYTOOL (
  for /f "delims=" %%I in ('where keytool 2^>nul') do (
    set "KEYTOOL=%%I"
    goto :find_keytool_done
  )
)
if not defined KEYTOOL (
  for /f "tokens=2,* delims==" %%A in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /I /C:"java.home ="') do (
    set "JAVA_HOME_FROM_JAVA=%%B"
  )
  if defined JAVA_HOME_FROM_JAVA (
    for /f "tokens=* delims= " %%H in ("%JAVA_HOME_FROM_JAVA%") do set "JAVA_HOME_FROM_JAVA=%%H"
    if exist "%JAVA_HOME_FROM_JAVA%\bin\keytool.exe" set "KEYTOOL=%JAVA_HOME_FROM_JAVA%\bin\keytool.exe"
  )
)
if not defined KEYTOOL (
  for /d %%D in ("%ProgramFiles%\Java\jdk*") do (
    if exist "%%~fD\bin\keytool.exe" (
      set "KEYTOOL=%%~fD\bin\keytool.exe"
      goto :find_keytool_done
    )
  )
)
if not defined KEYTOOL (
  for /f "delims=" %%I in ('where javac 2^>nul') do (
    if exist "%%~dpIkeytool.exe" (
      set "KEYTOOL=%%~dpIkeytool.exe"
      goto :find_keytool_done
    )
  )
)
:find_keytool_done
exit /b 0

:server
if not exist "bin\server\NetAuctionServer.class" call :compile || exit /b 1
set "PORT=9999"
set "SSL_FLAG=--ssl"
if not "%~2"=="" (
  if /I not "%~2"=="--ssl" if /I not "%~2"=="-ssl" set "PORT=%~2"
)
if not exist "certs\servidor.p12" call :certs || exit /b 1
if not exist "certs\truststore.p12" call :certs || exit /b 1
java -cp "lib/*;bin" server.NetAuctionServer %PORT% %SSL_FLAG%
exit /b %ERRORLEVEL%

:help
echo Uso:
echo   .\run.bat compile
echo   .\run.bat initdb
echo   .\run.bat certs [--force^|--reset-ca]
echo   .\run.bat server [puerto]
exit /b 0
