package common;

/**
 * Constantes globales del sistema NetAuction.
 * Define valores de configuración, nombres de acciones del protocolo y límites del sistema.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public final class Constants {

    // ==================== CONFIGURACIÓN DE RED ====================

    /** Puerto por defecto del servidor */
    public static final int SERVER_PORT = 9999;

    /** Host por defecto del servidor */
    public static final String SERVER_HOST = "localhost";

    /** Tamaño del pool de hilos para atender clientes */
    public static final int THREAD_POOL_SIZE = 50;

    // ==================== CONFIGURACIÓN DE SESIÓN ====================

    /** Duración del token de sesión en milisegundos (30 minutos) */
    public static final long SESSION_DURATION_MS = 30 * 60 * 1000;

    /** Número máximo de intentos de login fallidos antes del bloqueo */
    public static final int MAX_LOGIN_ATTEMPTS = 3;

    /** Duración del bloqueo de cuenta en milisegundos (5 minutos) */
    public static final long ACCOUNT_LOCK_DURATION_MS = 5 * 60 * 1000;

    // ==================== CONFIGURACIÓN DE SUBASTAS ====================

    /** Intervalo de revisión de subastas expiradas en milisegundos */
    public static final long AUCTION_CHECK_INTERVAL_MS = 1000;

    /** Duración mínima de una subasta en minutos */
    public static final int MIN_AUCTION_DURATION_MINUTES = 1;

    /** Duración máxima de una subasta en minutos */
    public static final int MAX_AUCTION_DURATION_MINUTES = 10080; // 7 días

    /** Precio mínimo de salida para una subasta */
    public static final double MIN_START_PRICE = 0.01;

    // ==================== ACCIONES DEL PROTOCOLO (CLIENTE → SERVIDOR) ====================

    /** Acción de registro de usuario */
    public static final String ACTION_REGISTER = "REGISTER";

    /** Acción de inicio de sesión */
    public static final String ACTION_LOGIN = "LOGIN";

    /** Acción de cierre de sesión */
    public static final String ACTION_LOGOUT = "LOGOUT";

    /** Acción de crear subasta */
    public static final String ACTION_CREATE_AUCTION = "CREATE_AUCTION";

    /** Acción de listar subastas activas */
    public static final String ACTION_LIST_AUCTIONS = "LIST_AUCTIONS";

    /** Acción de obtener detalle de subasta */
    public static final String ACTION_AUCTION_DETAIL = "AUCTION_DETAIL";

    /** Acción de realizar puja */
    public static final String ACTION_BID = "BID";

    /** Acción de obtener historial del usuario */
    public static final String ACTION_MY_HISTORY = "MY_HISTORY";

    /** Acción de cancelar subasta (ADMIN) */
    public static final String ACTION_CANCEL_AUCTION = "CANCEL_AUCTION";

    /** Acción de bloquear/desbloquear usuario (ADMIN) */
    public static final String ACTION_BLOCK_USER = "BLOCK_USER";

    /** Acción de ver logs de auditoría (ADMIN) */
    public static final String ACTION_VIEW_LOGS = "VIEW_LOGS";

    // ==================== RESPUESTAS DEL SERVIDOR ====================

    /** Sufijo para respuestas del servidor */
    public static final String RESPONSE_SUFFIX = "_RESPONSE";

    // ==================== NOTIFICACIONES PUSH (SERVIDOR → CLIENTE) ====================

    /** Notificación de nueva puja */
    public static final String NOTIFY_NEW_BID = "NEW_BID";

    /** Notificación de puja superada */
    public static final String NOTIFY_OUTBID = "OUTBID";

    /** Notificación de subasta cerrada */
    public static final String NOTIFY_AUCTION_CLOSED = "AUCTION_CLOSED";

    // ==================== ESTADOS ====================

    /** Estado de respuesta exitosa */
    public static final String STATUS_OK = "OK";

    /** Estado de respuesta con error */
    public static final String STATUS_ERROR = "ERROR";

    /** Estado de subasta activa */
    public static final String AUCTION_STATUS_ACTIVE = "ACTIVE";

    /** Estado de subasta finalizada */
    public static final String AUCTION_STATUS_FINISHED = "FINISHED";

    /** Estado de subasta cancelada */
    public static final String AUCTION_STATUS_CANCELLED = "CANCELLED";

    // ==================== ROLES ====================

    /** Rol de usuario normal */
    public static final String ROLE_USER = "USER";

    /** Rol de administrador */
    public static final String ROLE_ADMIN = "ADMIN";

    // ==================== RUTAS DE ARCHIVOS ====================

    /** Ruta de la base de datos SQLite */
    public static final String DATABASE_PATH = "data/netauction.db";

    /** Ruta del archivo de log de auditoría */
    public static final String AUDIT_LOG_PATH = "logs/audit.log";

    /** Ruta del keystore del servidor */
    public static final String SERVER_KEYSTORE_PATH = "certs/server.keystore";

    /** Ruta del truststore del cliente */
    public static final String CLIENT_TRUSTSTORE_PATH = "certs/client.truststore";

    /** Contraseña por defecto de los keystores */
    public static final String KEYSTORE_PASSWORD = "netauction123";

    // ==================== CONSTRUCTOR PRIVADO ====================

    /**
     * Constructor privado para evitar instanciación.
     */
    private Constants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }
}
