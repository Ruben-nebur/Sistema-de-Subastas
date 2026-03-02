package common;

/**
 * Constantes globales del sistema NetAuction (modulo servidor).
 * Contiene la configuracion de red, subastas, protocolo, estados y rutas de archivos.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public final class Constants {

    // ==================== CONFIGURACION DE RED ====================

    /** Puerto por defecto del servidor */
    public static final int SERVER_PORT = 9999;

    /** Host por defecto del servidor */
    public static final String SERVER_HOST = "localhost";

    /** Tamano del pool de hilos para clientes concurrentes */
    public static final int THREAD_POOL_SIZE = 50;

    // ==================== CONFIGURACION DE SUBASTAS ====================

    /** Duracion minima de una subasta en minutos */
    public static final int MIN_AUCTION_DURATION_MINUTES = 1;

    /** Duracion maxima de una subasta en minutos (7 dias) */
    public static final int MAX_AUCTION_DURATION_MINUTES = 10080;

    /** Precio inicial minimo de una subasta */
    public static final double MIN_START_PRICE = 0.01;

    // ==================== ACCIONES DEL PROTOCOLO ====================

    /** Accion de registro de usuario */
    public static final String ACTION_REGISTER = "REGISTER";

    /** Accion de inicio de sesion */
    public static final String ACTION_LOGIN = "LOGIN";

    /** Accion de cierre de sesion */
    public static final String ACTION_LOGOUT = "LOGOUT";

    /** Accion de creacion de subasta */
    public static final String ACTION_CREATE_AUCTION = "CREATE_AUCTION";

    /** Accion de listado de subastas */
    public static final String ACTION_LIST_AUCTIONS = "LIST_AUCTIONS";

    /** Accion de detalle de subasta */
    public static final String ACTION_AUCTION_DETAIL = "AUCTION_DETAIL";

    /** Accion de puja */
    public static final String ACTION_BID = "BID";

    /** Accion de cancelacion de subasta */
    public static final String ACTION_CANCEL_AUCTION = "CANCEL_AUCTION";

    /** Accion de bloqueo de usuario */
    public static final String ACTION_BLOCK_USER = "BLOCK_USER";

    // ==================== RESPUESTAS DEL SERVIDOR ====================

    /** Sufijo anadido a las acciones para formar el nombre de la respuesta */
    public static final String RESPONSE_SUFFIX = "_RESPONSE";

    // ==================== NOTIFICACIONES PUSH ====================

    /** Notificacion de nueva puja en una subasta */
    public static final String NOTIFY_NEW_BID = "NEW_BID";

    /** Notificacion de puja superada */
    public static final String NOTIFY_OUTBID = "OUTBID";

    /** Notificacion de subasta finalizada */
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

    // ==================== RUTAS DE ARCHIVOS ====================

    /** Ruta de la base de datos SQLite */
    public static final String DATABASE_PATH = "data/netauction.db";

    /** Ruta del keystore de la CA */
    public static final String CA_KEYSTORE_PATH = "certs/ca.p12";

    /** Ruta del certificado de la CA */
    public static final String CA_CERTIFICATE_PATH = "certs/ca.cer";

    /** Ruta del keystore del servidor */
    public static final String SERVER_KEYSTORE_PATH = "certs/servidor.p12";

    /** Ruta del certificado del servidor */
    public static final String SERVER_CERTIFICATE_PATH = "certs/server.cer";

    /** Ruta del truststore del cliente */
    public static final String CLIENT_TRUSTSTORE_PATH = "certs/truststore.p12";

    /** Contrasena de los keystores y truststores */
    public static final String KEYSTORE_PASSWORD = "netauction123";

    /**
     * Constructor privado para evitar instanciacion.
     */
    private Constants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }
}
