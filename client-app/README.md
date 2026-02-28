# client-app

Proyecto independiente del cliente (consola + GUI).

## Comandos

```bat
run.bat truststore
run.bat compile
run.bat client <host> <puerto>
run.bat gui
```

## SSL obligatorio

El cliente requiere:
- `certs/server.cer` con el certificado publico del servidor.
- `certs/truststore.p12`, que puede generarse con `run.bat truststore`.

Si trabajas en el mismo repositorio que el servidor, `run.bat client` y `run.bat gui` intentan copiar automaticamente `../server-app/certs/server.cer`.

En equipos distintos, basta con distribuir `client-app` ya incluyendo `certs/server.cer`. No hace falta copiar `truststore.p12` desde el servidor.

## GUI

Define `JAVAFX_HOME` con la ruta del JavaFX SDK.
