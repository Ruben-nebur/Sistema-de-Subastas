# NetAuction (PSP) - Guia rapida

Proyecto final de **Programacion de Servicios y Procesos** (2o DAM).
Aplicacion de subastas en tiempo real con arquitectura cliente-servidor en Java.

## 1) Estructura simplificada

- `src/common`: protocolo y constantes compartidas
- `src/server`: servidor (usuarios, sesiones, subastas, notificaciones, BD)
- `src/client`: cliente de consola
- `src/client/gui`: cliente JavaFX
- `scripts`: scripts de compilacion/ejecucion
- `run.bat` / `run.sh`: lanzador unificado (comando corto)
- `lib`: dependencias JAR
- `data`: base de datos SQLite (`netauction.db`)
- `logs`: auditoria (`audit.log`)

## 2) Requisitos

- JDK 17+ (recomendado JDK 22)
- (Opcional GUI) JavaFX SDK y variable `JAVAFX_HOME`

## 3) Arranque rapido (Windows)

```bat
run.bat setup
run.bat compile
run.bat server 9999 --ssl
```

En otra terminal:

```bat
run.bat client localhost 9999 --ssl
```

## 4) Arranque rapido (Linux/macOS)

```bash
./run.sh setup
./run.sh compile
./run.sh server 9999 --ssl
```

En otra terminal:

```bash
./run.sh client localhost 9999 --ssl
```

## 5) Comandos utiles

```bat
run.bat certs
run.bat gui
```

## 6) Flujo funcional del sistema

1. El cliente se conecta por TCP (opcional SSL/TLS).
2. `LOGIN` crea una sesion con token UUID.
3. Las acciones autenticadas usan ese token:
   - `CREATE_AUCTION`
   - `LIST_AUCTIONS`
   - `AUCTION_DETAIL`
   - `BID`
   - `MY_HISTORY`
4. El servidor envia notificaciones push:
   - `NEW_BID`
   - `OUTBID`
   - `AUCTION_CLOSED`
5. Un scheduler cierra subastas expiradas automaticamente.
6. Se persiste en SQLite (`data/netauction.db`) y se audita en `logs/audit.log`.

## 7) Usuario admin por defecto

- Usuario: `admin`
- Password: `admin123`

## 8) Notas de mantenimiento

- Si falta `lib/`, ejecutar `run.bat setup` o `./run.sh setup`.
- Si hay warning SLF4J en consola, no bloquea el funcionamiento.
