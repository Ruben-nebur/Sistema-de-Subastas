package common;

/**
 * Constantes globales del sistema NetAuction.
 */
public final class Constants {

    // ==================== CONFIGURACION DE RED ====================

    public static final int SERVER_PORT = 9999;
    public static final String SERVER_HOST = "localhost";
    public static final int THREAD_POOL_SIZE = 50;

    // ==================== CONFIGURACION DE SUBASTAS ====================

    public static final int MIN_AUCTION_DURATION_MINUTES = 1;
    public static final int MAX_AUCTION_DURATION_MINUTES = 10080;
    public static final double MIN_START_PRICE = 0.01;

    // ==================== ACCIONES DEL PROTOCOLO ====================

    public static final String ACTION_REGISTER = "REGISTER";
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_CREATE_AUCTION = "CREATE_AUCTION";
    public static final String ACTION_LIST_AUCTIONS = "LIST_AUCTIONS";
    public static final String ACTION_AUCTION_DETAIL = "AUCTION_DETAIL";
    public static final String ACTION_BID = "BID";
    public static final String ACTION_CANCEL_AUCTION = "CANCEL_AUCTION";
    public static final String ACTION_BLOCK_USER = "BLOCK_USER";

    // ==================== RESPUESTAS DEL SERVIDOR ====================

    public static final String RESPONSE_SUFFIX = "_RESPONSE";

    // ==================== NOTIFICACIONES PUSH ====================

    public static final String NOTIFY_NEW_BID = "NEW_BID";
    public static final String NOTIFY_OUTBID = "OUTBID";
    public static final String NOTIFY_AUCTION_CLOSED = "AUCTION_CLOSED";

    // ==================== ESTADOS ====================

    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERROR";
    public static final String AUCTION_STATUS_ACTIVE = "ACTIVE";
    public static final String AUCTION_STATUS_FINISHED = "FINISHED";
    public static final String AUCTION_STATUS_CANCELLED = "CANCELLED";

    // ==================== RUTAS DE ARCHIVOS ====================

    public static final String DATABASE_PATH = "data/netauction.db";
    public static final String SERVER_KEYSTORE_PATH = "certs/servidor.p12";
    public static final String SERVER_CERTIFICATE_PATH = "certs/server.cer";
    public static final String CLIENT_TRUSTSTORE_PATH = "certs/truststore.p12";
    public static final String KEYSTORE_PASSWORD = "netauction123";

    private Constants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }
}
