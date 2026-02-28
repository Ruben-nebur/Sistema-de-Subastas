# server-app

Proyecto independiente del servidor.

## Comandos

```bat
run.bat compile
run.bat initdb
run.bat certs
run.bat server 9999
```

## Persistencia

La base de datos se guarda en `server-app/data/netauction.db`.

## SSL

`run.bat certs` genera:
- `server-app/certs/servidor.p12`
- `server-app/certs/server.cer`
- `server-app/certs/truststore.p12`

Ademas sincroniza automaticamente `server.cer` y `truststore.p12` en `../client-app/certs`.

Para despliegue en otro equipo cliente, solo debe compartirse `server.cer`.
El archivo `servidor.p12` contiene la clave privada del servidor y no debe salir del equipo servidor.
