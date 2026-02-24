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
- `server-app/certs/server.keystore`
- `server-app/certs/client.truststore`

Ademas sincroniza automaticamente `client.truststore` en `../client-app/certs`.
