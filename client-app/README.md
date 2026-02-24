# client-app

Proyecto independiente del cliente (consola + GUI).

## Comandos

```bat
run.bat compile
run.bat client <host> <puerto>
run.bat gui
```

## SSL obligatorio

El cliente requiere `certs/client.truststore`.
Si no existe, intenta copiarlo automaticamente desde `../server-app/certs/client.truststore`.

## GUI

Define `JAVAFX_HOME` con la ruta del JavaFX SDK.
