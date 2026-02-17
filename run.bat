@echo off
setlocal EnableExtensions

if "%~1"=="" goto :help
set "CMD=%~1"

if /I "%CMD%"=="setup" goto :setup
if /I "%CMD%"=="compile" goto :compile
if /I "%CMD%"=="certs" goto :certs
if /I "%CMD%"=="server" goto :server
if /I "%CMD%"=="client" goto :client
if /I "%CMD%"=="gui" goto :gui
if /I "%CMD%"=="cleanup" goto :cleanup

goto :help

:setup
if not exist "lib" mkdir lib
set "COPIED=0"

if exist "%USERPROFILE%\.gradle\caches\modules-2\files-2.1\com.google.code.gson\gson\2.11.0\527175ca6d81050b53bdd4c457a6d6e017626b0e\gson-2.11.0.jar" (
  copy /Y "%USERPROFILE%\.gradle\caches\modules-2\files-2.1\com.google.code.gson\gson\2.11.0\527175ca6d81050b53bdd4c457a6d6e017626b0e\gson-2.11.0.jar" "lib\gson-2.11.0.jar" >nul
  set "COPIED=1"
)

if exist "%USERPROFILE%\.m2\repository\org\xerial\sqlite-jdbc\3.45.0.0\sqlite-jdbc-3.45.0.0.jar" (
  copy /Y "%USERPROFILE%\.m2\repository\org\xerial\sqlite-jdbc\3.45.0.0\sqlite-jdbc-3.45.0.0.jar" "lib\sqlite-jdbc-3.45.0.0.jar" >nul
  set "COPIED=1"
)

if exist "%USERPROFILE%\.m2\repository\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar" (
  copy /Y "%USERPROFILE%\.m2\repository\org\slf4j\slf4j-api\1.7.36\slf4j-api-1.7.36.jar" "lib\slf4j-api-1.7.36.jar" >nul
  set "COPIED=1"
)

if "%COPIED%"=="1" (
  echo [OK] Dependencias listas en lib\
  dir /b lib
  exit /b 0
)

echo [ERROR] No se encontraron dependencias en caches locales.
echo         Copia manualmente a lib\:
echo         - gson-2.11.0.jar
echo         - sqlite-jdbc-3.45.0.0.jar
echo         - slf4j-api-1.7.36.jar
exit /b 1

:compile
if not exist "src" (
  echo [ERROR] Ejecuta este comando desde la raiz del proyecto.
  exit /b 1
)
if not exist "lib" (
  echo [ERROR] Falta lib\. Ejecuta: .\run.bat setup
  exit /b 1
)
if not exist "bin" mkdir bin

echo [1/2] Compilando servidor y cliente de consola...
javac -cp "lib/*" -d bin -sourcepath src ^
  src/common/*.java ^
  src/server/model/*.java ^
  src/server/security/*.java ^
  src/server/persistence/*.java ^
  src/server/manager/*.java ^
  src/server/service/*.java ^
  src/server/util/*.java ^
  src/server/*.java ^
  src/client/*.java
if errorlevel 1 (
  echo [ERROR] Fallo la compilacion de servidor/cliente.
  exit /b 1
)
echo [OK] Servidor y cliente compilados.

echo [2/2] Compilando GUI (si JAVAFX_HOME esta configurado)...
if defined JAVAFX_HOME (
  javac -cp "lib/*;bin" -d bin ^
    --module-path "%JAVAFX_HOME%\lib" ^
    --add-modules javafx.controls,javafx.fxml ^
    src/client/gui/*.java
  if errorlevel 1 (
    echo [WARN] No se pudo compilar la GUI.
  ) else (
    echo [OK] GUI compilada.
  )
) else (
  echo [WARN] JAVAFX_HOME no esta definido. Se omite GUI.
)

echo [DONE] Compilacion terminada.
exit /b 0

:certs
if not exist "certs" mkdir certs
set "KEYTOOL=keytool"
if defined JAVA_HOME set "KEYTOOL=%JAVA_HOME%\bin\keytool"

if exist "certs\server.keystore" del /q "certs\server.keystore"
if exist "certs\server.cer" del /q "certs\server.cer"
if exist "certs\client.truststore" del /q "certs\client.truststore"

echo [1/3] Generando keystore del servidor...
"%KEYTOOL%" -genkeypair -alias netauction ^
  -keyalg RSA -keysize 2048 ^
  -validity 365 ^
  -keystore certs/server.keystore ^
  -storepass netauction123 ^
  -dname "CN=localhost, OU=NetAuction, O=PSP, L=Madrid, ST=Madrid, C=ES"
if errorlevel 1 exit /b 1

echo [2/3] Exportando certificado publico...
"%KEYTOOL%" -exportcert -alias netauction ^
  -keystore certs/server.keystore ^
  -storepass netauction123 ^
  -file certs/server.cer
if errorlevel 1 exit /b 1

echo [3/3] Creando truststore del cliente...
"%KEYTOOL%" -importcert -alias netauction ^
  -file certs/server.cer ^
  -keystore certs/client.truststore ^
  -storepass netauction123 ^
  -noprompt
if errorlevel 1 exit /b 1

echo [DONE] Certificados generados.
exit /b 0

:server
if not exist "bin\server\NetAuctionServer.class" (
  echo [ERROR] Servidor no compilado. Ejecuta: .\run.bat compile
  exit /b 1
)
if not exist "logs" mkdir logs
set "PORT=9999"
set "SSL_FLAG="
if not "%~2"=="" set "PORT=%~2"
if /I "%~2"=="--ssl" (
  set "PORT=9999"
  set "SSL_FLAG=--ssl"
)
if /I "%~2"=="-ssl" (
  set "PORT=9999"
  set "SSL_FLAG=--ssl"
)
if /I "%~3"=="--ssl" set "SSL_FLAG=--ssl"
if /I "%~3"=="-ssl" set "SSL_FLAG=--ssl"

echo [INFO] Iniciando servidor en puerto %PORT% %SSL_FLAG%
java -cp "lib/*;bin" server.NetAuctionServer %PORT% %SSL_FLAG%
exit /b %ERRORLEVEL%

:client
if not exist "bin\client\NetAuctionClient.class" (
  echo [ERROR] Cliente no compilado. Ejecuta: .\run.bat compile
  exit /b 1
)
set "HOST=localhost"
set "PORT=9999"
set "SSL_FLAG="
if not "%~2"=="" set "HOST=%~2"
if not "%~3"=="" set "PORT=%~3"
if /I "%~4"=="--ssl" set "SSL_FLAG=--ssl"
if /I "%~4"=="-ssl" set "SSL_FLAG=--ssl"

echo [INFO] Iniciando cliente contra %HOST%:%PORT% %SSL_FLAG%
java -cp "lib/*;bin" client.NetAuctionClient %HOST% %PORT% %SSL_FLAG%
exit /b %ERRORLEVEL%

:gui
if not exist "bin\client\gui\MainApp.class" (
  echo [ERROR] GUI no compilada. Ejecuta: .\run.bat compile
  exit /b 1
)
if not defined JAVAFX_HOME (
  echo [ERROR] JAVAFX_HOME no esta definido.
  exit /b 1
)
java -cp "lib/*;bin" ^
  --module-path "%JAVAFX_HOME%\lib" ^
  --add-modules javafx.controls,javafx.fxml ^
  client.gui.MainApp
exit /b %ERRORLEVEL%

:cleanup
if not exist "Sistema-de-Subastas-main" (
  echo [OK] No hay carpeta anidada para limpiar.
  exit /b 0
)
rmdir /S /Q "Sistema-de-Subastas-main"
if exist "Sistema-de-Subastas-main" (
  echo [WARN] No se pudo borrar. Hay archivos en uso.
  exit /b 1
)
echo [OK] Estructura antigua eliminada.
exit /b 0

:help
echo Uso:
echo   .\run.bat setup
echo   .\run.bat compile
echo   .\run.bat certs
echo   .\run.bat server [puerto] [--ssl]
echo   .\run.bat client [host] [puerto] [--ssl]
echo   .\run.bat gui
echo   .\run.bat cleanup
exit /b 0
