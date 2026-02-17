# Protocolo de Comunicación NetAuction

Documentación completa del protocolo de comunicación cliente-servidor.

## Visión General

El protocolo NetAuction utiliza mensajes JSON sobre TCP. Cada mensaje es una línea JSON completa terminada en `\n`. La conexión puede ser plana o cifrada con SSL/TLS.

## Formato de Mensaje

### Estructura Base

```json
{
    "action": "string",
    "token": "string | null",
    "timestamp": "long",
    "payload": "object"
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `action` | String | Tipo de acción a realizar |
| `token` | String | Token de sesión (null si no autenticado) |
| `timestamp` | long | Timestamp Unix en milisegundos |
| `payload` | Object | Datos específicos de la acción |

### Respuestas del Servidor

```json
{
    "action": "RESPONSE",
    "token": null,
    "timestamp": 1234567890,
    "payload": {
        "success": true,
        "message": "Mensaje descriptivo",
        "data": { ... }
    }
}
```

## Acciones del Protocolo

### 1. REGISTER - Registro de Usuario

**Request:**
```json
{
    "action": "REGISTER",
    "token": null,
    "timestamp": 1234567890,
    "payload": {
        "username": "usuario1",
        "password": "contraseña123",
        "email": "usuario@ejemplo.com"
    }
}
```

**Response (éxito):**
```json
{
    "action": "RESPONSE",
    "timestamp": 1234567890,
    "payload": {
        "success": true,
        "message": "Usuario registrado correctamente"
    }
}
```

**Response (error):**
```json
{
    "action": "RESPONSE",
    "timestamp": 1234567890,
    "payload": {
        "success": false,
        "message": "El nombre de usuario ya existe"
    }
}
```

**Validaciones:**
- Username: 3-20 caracteres, alfanumérico
- Password: mínimo 6 caracteres
- Email: formato válido de email

---

### 2. LOGIN - Iniciar Sesión

**Request:**
```json
{
    "action": "LOGIN",
    "token": null,
    "timestamp": 1234567890,
    "payload": {
        "username": "usuario1",
        "password": "contraseña123"
    }
}
```

**Response (éxito):**
```json
{
    "action": "RESPONSE",
    "timestamp": 1234567890,
    "payload": {
        "success": true,
        "message": "Login exitoso",
        "token": "550e8400-e29b-41d4-a716-446655440000",
        "role": "USER"
    }
}
```

**Response (error - credenciales):**
```json
{
    "action": "RESPONSE",
    "timestamp": 1234567890,
    "payload": {
        "success": false,
        "message": "Credenciales inválidas"
    }
}
```

**Response (error - cuenta bloqueada):**
```json
{
    "action": "RESPONSE",
    "timestamp": 1234567890,
    "payload": {
        "success": false,
        "message": "Cuenta bloqueada por múltiples intentos fallidos"
    }
}
```

**Notas:**
- Máximo 3 intentos fallidos antes de bloqueo
- Token válido por 30 minutos
- Se renueva automáticamente con actividad

---

### 3. LOGOUT - Cerrar Sesión

**Request:**
```json
{
    "action": "LOGOUT",
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": 1234567890,
    "payload": {}
}
```

**Response:**
```json
{
    "action": "RESPONSE",
    "timestamp": 1234567890,
    "payload": {
        "success": true,
        "message": "Sesión cerrada"
    }
}
```

---

### 4. CREATE_AUCTION - Crear Subasta

**Request:**
```json
{
    "action": "CREATE_AUCTION",
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": 1234567890,
    "payload": {
        "title": "iPhone 15 Pro",
        "description": "iPhone 15 Pro 256GB, nuevo sin abrir",
        "startingPrice": 800.00,
        "durationMinutes": 60
    }
}
```

**Response (éxito):**
```json
{
    "action": "RESPONSE",
    "timestamp": 1234567890,
    "payload": {
        "success": true,
        "message": "Subasta creada",
        "auctionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    }
}
```

**Validaciones:**
- Título: 1-100 caracteres
- Descripción: 1-1000 caracteres
- Precio inicial: > 0
- Duración: 1-10080 minutos (máximo 7 días)

---

### 5. LIST_AUCTIONS - Listar Subastas

**Request:**
```json
{
    "action": "LIST_AUCTIONS",
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": 1234567890,
    "payload": {
        "status": "ACTIVE"
    }
}
```

**Response:**
```json
{
    "action": "RESPONSE",
    "timestamp": 1234567890,
    "payload": {
        "success": true,
        "auctions": [
            {
                "id": "a1b2c3d4...",
                "title": "iPhone 15 Pro",
                "description": "iPhone 15 Pro 256GB...",
                "seller": "vendedor1",
                "startingPrice": 800.00,
                "currentPrice": 950.00,
                "highestBidder": "comprador1",
                "status": "ACTIVE",
                "startTime": 1234567890,
                "endTime": 1234571490,
                "bidCount": 5
            }
        ]
    }
}
```

**Filtros disponibles:**
- `status`: "ACTIVE", "CLOSED", "CANCELLED", "ALL" (default: "ACTIVE")

---

### 6. GET_AUCTION - Detalle de Subasta

**Request:**
```json
{
    "action": "GET_AUCTION",
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": 1234567890,
    "payload": {
        "auctionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    }
}
```

**Response:**
```json
{
    "action": "RESPONSE",
    "timestamp": 1234567890,
    "payload": {
        "success": true,
        "auction": {
            "id": "a1b2c3d4...",
            "title": "iPhone 15 Pro",
            "description": "iPhone 15 Pro 256GB, nuevo sin abrir",
            "seller": "vendedor1",
            "startingPrice": 800.00,
            "currentPrice": 950.00,
            "highestBidder": "comprador1",
            "status": "ACTIVE",
            "startTime": 1234567890,
            "endTime": 1234571490,
            "bidCount": 5,
            "bids": [
                {
                    "bidder": "comprador1",
                    "amount": 950.00,
                    "timestamp": 1234569000
                },
                {
                    "bidder": "comprador2",
                    "amount": 900.00,
                    "timestamp": 1234568500
                }
            ]
        }
    }
}
```

---

### 7. PLACE_BID - Realizar Puja

**Request:**
```json
{
    "action": "PLACE_BID",
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": 1234567890,
    "payload": {
        "auctionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "amount": 1000.00
    }
}
```

**Response (éxito):**
```json
{
    "action": "RESPONSE",
    "timestamp": 1234567890,
    "payload": {
        "success": true,
        "message": "Puja aceptada",
        "newPrice": 1000.00
    }
}
```

**Response (error):**
```json
{
    "action": "RESPONSE",
    "timestamp": 1234567890,
    "payload": {
        "success": false,
        "message": "La puja debe ser mayor que el precio actual (950.00)"
    }
}
```

**Validaciones:**
- Monto > precio actual
- Subasta en estado ACTIVE
- El pujador no puede ser el vendedor
- El pujador no puede ser ya el mejor postor

---

### 8. MY_AUCTIONS - Mis Subastas

**Request:**
```json
{
    "action": "MY_AUCTIONS",
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": 1234567890,
    "payload": {}
}
```

**Response:**
```json
{
    "action": "RESPONSE",
    "timestamp": 1234567890,
    "payload": {
        "success": true,
        "auctions": [...]
    }
}
```

---

### 9. MY_BIDS - Mis Pujas

**Request:**
```json
{
    "action": "MY_BIDS",
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": 1234567890,
    "payload": {}
}
```

**Response:**
```json
{
    "action": "RESPONSE",
    "timestamp": 1234567890,
    "payload": {
        "success": true,
        "auctions": [...]
    }
}
```

---

### 10. SUBSCRIBE - Suscribirse a Subasta

**Request:**
```json
{
    "action": "SUBSCRIBE",
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": 1234567890,
    "payload": {
        "auctionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    }
}
```

**Response:**
```json
{
    "action": "RESPONSE",
    "timestamp": 1234567890,
    "payload": {
        "success": true,
        "message": "Suscrito a notificaciones de la subasta"
    }
}
```

---

### 11. UNSUBSCRIBE - Cancelar Suscripción

**Request:**
```json
{
    "action": "UNSUBSCRIBE",
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "timestamp": 1234567890,
    "payload": {
        "auctionId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
    }
}
```

**Response:**
```json
{
    "action": "RESPONSE",
    "timestamp": 1234567890,
    "payload": {
        "success": true,
        "message": "Suscripción cancelada"
    }
}
```

---

## Notificaciones Push

El servidor envía notificaciones a los clientes suscritos sin solicitud previa.

### NOTIFY_NEW_BID - Nueva Puja

```json
{
    "action": "NOTIFY_NEW_BID",
    "timestamp": 1234567890,
    "payload": {
        "auctionId": "a1b2c3d4...",
        "auctionTitle": "iPhone 15 Pro",
        "bidder": "comprador1",
        "amount": 1000.00
    }
}
```

### NOTIFY_OUTBID - Superado

```json
{
    "action": "NOTIFY_OUTBID",
    "timestamp": 1234567890,
    "payload": {
        "auctionId": "a1b2c3d4...",
        "auctionTitle": "iPhone 15 Pro",
        "newAmount": 1050.00,
        "yourBid": 1000.00
    }
}
```

### NOTIFY_AUCTION_CLOSED - Subasta Cerrada

```json
{
    "action": "NOTIFY_AUCTION_CLOSED",
    "timestamp": 1234567890,
    "payload": {
        "auctionId": "a1b2c3d4...",
        "auctionTitle": "iPhone 15 Pro",
        "winner": "comprador1",
        "finalPrice": 1050.00,
        "isWinner": true
    }
}
```

### NOTIFY_SESSION_EXPIRED - Sesión Expirada

```json
{
    "action": "NOTIFY_SESSION_EXPIRED",
    "timestamp": 1234567890,
    "payload": {
        "message": "Su sesión ha expirado. Por favor, inicie sesión nuevamente."
    }
}
```

---

## Códigos de Error

| Código | Mensaje | Descripción |
|--------|---------|-------------|
| AUTH_001 | Credenciales inválidas | Usuario o contraseña incorrectos |
| AUTH_002 | Cuenta bloqueada | Demasiados intentos fallidos |
| AUTH_003 | Sesión expirada | Token ya no es válido |
| AUTH_004 | Token inválido | Token no reconocido |
| REG_001 | Usuario ya existe | El username está en uso |
| REG_002 | Email ya registrado | El email está en uso |
| REG_003 | Datos inválidos | Formato de datos incorrecto |
| AUC_001 | Subasta no encontrada | ID de subasta no existe |
| AUC_002 | Subasta no activa | La subasta ya cerró |
| AUC_003 | Puja inválida | Monto menor al actual |
| AUC_004 | Auto-puja no permitida | No puedes pujar en tu propia subasta |
| AUC_005 | Ya eres el mejor postor | Tu puja ya es la más alta |
| SYS_001 | Error interno | Error inesperado del servidor |

---

## Flujo de Conexión

```
Cliente                                    Servidor
   |                                          |
   |----------- TCP Connect ----------------->|
   |                                          |
   |----------- SSL Handshake --------------->| (si SSL)
   |                                          |
   |----------- LOGIN Request --------------->|
   |<---------- LOGIN Response (token) -------|
   |                                          |
   |----------- Actions (con token) --------->|
   |<---------- Responses --------------------|
   |                                          |
   |<---------- Notifications (push) ---------|
   |                                          |
   |----------- LOGOUT Request -------------->|
   |<---------- LOGOUT Response --------------|
   |                                          |
   |----------- TCP Close ------------------->|
```

---

## Consideraciones de Implementación

### Timeout de Conexión
- Conexión: 5000ms
- Lectura: 30000ms

### Thread Safety
- Todas las operaciones de puja son atómicas (ReadWriteLock)
- Las colecciones usan ConcurrentHashMap

### Persistencia
- Las subastas y pujas se persisten en SQLite
- Los usuarios se almacenan con contraseña hasheada

### Logs
- Todas las acciones se registran en audit.log
- Formato: `[TIMESTAMP] [ACTION] [USER] [IP] [DETAILS]`

---

## Ejemplo de Cliente Completo

```java
// Conectar
ServerConnection conn = new ServerConnection(true); // SSL
conn.connect("localhost", 5000);

// Login
Message loginMsg = new Message("LOGIN");
loginMsg.put("username", "usuario1");
loginMsg.put("password", "contraseña123");
conn.send(loginMsg);

Message response = conn.receive();
String token = response.getPayload().get("token").getAsString();

// Listar subastas
Message listMsg = new Message("LIST_AUCTIONS");
listMsg.setToken(token);
listMsg.put("status", "ACTIVE");
conn.send(listMsg);

response = conn.receive();
JsonArray auctions = response.getPayload()
    .get("auctions").getAsJsonArray();

// Realizar puja
Message bidMsg = new Message("PLACE_BID");
bidMsg.setToken(token);
bidMsg.put("auctionId", "a1b2c3d4...");
bidMsg.put("amount", 1000.00);
conn.send(bidMsg);

response = conn.receive();
if (response.isSuccess()) {
    System.out.println("Puja aceptada!");
}

// Logout
Message logoutMsg = new Message("LOGOUT");
logoutMsg.setToken(token);
conn.send(logoutMsg);
conn.disconnect();
```
