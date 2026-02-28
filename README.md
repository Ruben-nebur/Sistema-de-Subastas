# NetAuction (PSP) - Guia rapida

Proyecto final de Programacion de Servicios y Procesos (2o DAM).

## Proyectos independientes

- `server-app`: proyecto del servidor (src, lib, certs, data, run.bat propios).
- `client-app`: proyecto del cliente (consola + GUI, src, lib, certs, run.bat propios).

## Requisitos

- JDK 17+ con `keytool` disponible en PATH o JAVA_HOME.
- (GUI) JavaFX SDK con `JAVAFX_HOME`.

## Modelo TLS usado

- Servidor: `certs/servidor.p12` con clave privada.
- Cliente: `certs/server.cer` como certificado publico confiable.
- Cliente: `certs/truststore.p12` generado localmente a partir de `server.cer`.

El archivo que puede compartirse entre equipos es solo `server.cer`.
No debe compartirse `servidor.p12`.

## Inicializar base de datos

En `server-app`:
```bat
run.bat initdb
```

Crea `server-app/data/netauction.db` y deja el sistema listo para persistir usuarios y subastas.

## SSL obligatorio - mismo equipo

1. Terminal 1 (servidor):
```bat
cd server-app
run.bat compile
run.bat certs
run.bat server 9999
```

2. Terminal 2 (cliente consola):
```bat
cd client-app
run.bat truststore
run.bat compile
run.bat client localhost 9999
```

3. Terminal 2 alternativa (GUI):
```bat
cd client-app
run.bat gui
```

## SSL obligatorio - equipos distintos

1. En el equipo servidor:
- Ejecuta `server-app\\run.bat certs` y `server-app\\run.bat server 9999`.
- Abre el puerto `9999` en firewall/router si aplica.

2. En el equipo cliente debe existir el certificado publico:
- `client-app\\certs\\server.cer`
- Ese fichero puede ir ya incluido en el repositorio o en el paquete del cliente.

3. En el equipo cliente:
- Genera el truststore local:
```bat
cd client-app
run.bat truststore
```
- Conecta usando la IP del servidor:
```bat
cd client-app
run.bat client <IP_SERVIDOR> 9999
```
- En GUI, usa esa misma IP en el campo Host.
