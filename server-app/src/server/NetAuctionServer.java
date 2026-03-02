package server;

import common.Constants;
import server.manager.AuctionManager;
import server.manager.SessionManager;
import server.manager.UserManager;
import server.model.Auction;
import server.persistence.Database;
import server.security.SSLConfig;
import server.service.NotificationService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servidor principal de NetAuction.
 * Gestiona el ciclo de vida del servidor, incluyendo la inicializacion de componentes,
 * aceptacion de conexiones de clientes y cierre ordenado del sistema.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class NetAuctionServer {

    /** Puerto en el que escucha el servidor */
    private final int port;

    /** Socket del servidor para aceptar conexiones */
    private ServerSocket serverSocket;

    /** Pool de hilos para manejar clientes concurrentes */
    private ExecutorService executorService;

    /** Planificador para monitorizar subastas expiradas */
    private ScheduledExecutorService auctionMonitor;

    /** Manejador del protocolo de comunicacion */
    private ProtocolHandler protocolHandler;

    /** Gestor de usuarios */
    private UserManager userManager;

    /** Gestor de sesiones */
    private SessionManager sessionManager;

    /** Gestor de subastas */
    private AuctionManager auctionManager;

    /** Servicio de notificaciones push */
    private NotificationService notificationService;

    /** Conexion a la base de datos */
    private Database database;

    /** Indica si el servidor esta en ejecucion */
    private volatile boolean running;

    /** Indica si SSL/TLS esta habilitado */
    private boolean sslEnabled;

    /**
     * Constructor con puerto por defecto y SSL habilitado.
     *
     * @param port puerto en el que escuchara el servidor
     */
    public NetAuctionServer(int port) {
        this(port, true);
    }

    /**
     * Constructor completo.
     *
     * @param port puerto en el que escuchara el servidor
     * @param sslEnabled true para habilitar SSL/TLS
     */
    public NetAuctionServer(int port, boolean sslEnabled) {
        this.port = port;
        this.running = false;
        this.sslEnabled = sslEnabled;
    }

    /**
     * Inicializa todos los componentes y arranca el servidor.
     * Configura la base de datos, gestores, pool de hilos y comienza a aceptar conexiones.
     *
     * @throws IOException si ocurre un error al crear el socket del servidor
     */
    public void start() throws IOException {
        System.out.println("\n[SERVER] Inicializando componentes...");

        try {
            database = new Database();
            database.initialize();
        } catch (SQLException e) {
            System.err.println("[SERVER] Error inicializando BD: " + e.getMessage());
            System.out.println("[SERVER] Continuando sin persistencia...");
            database = null;
        }

        userManager = new UserManager();
        sessionManager = new SessionManager();
        auctionManager = new AuctionManager();

        if (database != null) {
            userManager.setDatabase(database);
            auctionManager.setDatabase(database);
        }

        userManager.loadFromDatabase();
        auctionManager.loadFromDatabase();

        notificationService = new NotificationService();

        protocolHandler = new ProtocolHandler(userManager, sessionManager, auctionManager);
        protocolHandler.setNotificationService(notificationService);

        executorService = Executors.newFixedThreadPool(Constants.THREAD_POOL_SIZE);
        auctionMonitor = Executors.newSingleThreadScheduledExecutor();
        auctionMonitor.scheduleAtFixedRate(this::closeExpiredAuctionsSafely, 1, 1, TimeUnit.SECONDS);

        if (sslEnabled) {
            try {
                serverSocket = SSLConfig.createServerSocket(port);
                System.out.println("[SERVER] SSL/TLS habilitado");
            } catch (Exception e) {
                throw new IOException("No se pudo inicializar SSL/TLS obligatorio", e);
            }
        } else {
            serverSocket = new ServerSocket(port);
        }
        running = true;

        System.out.println("[SERVER] Puerto: " + port);
        System.out.println("[SERVER] SSL/TLS: " + (sslEnabled ? "HABILITADO" : "DESHABILITADO"));
        System.out.println("[SERVER] Estado: ACTIVO - Esperando conexiones...");

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.setProtocolHandler(protocolHandler);
                executorService.submit(clientHandler);
            } catch (IOException e) {
                if (running) {
                    System.err.println("[SERVER] Error aceptando conexion: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Detiene el servidor de forma ordenada.
     * Cierra el monitor de subastas, sesiones, notificaciones, socket y pool de hilos.
     */
    public void stop() {
        running = false;
        System.out.println("\n[SERVER] Iniciando apagado ordenado...");

        if (auctionMonitor != null) {
            auctionMonitor.shutdownNow();
        }

        if (sessionManager != null) {
            sessionManager.shutdown();
        }

        if (notificationService != null) {
            notificationService.clear();
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Error cerrando socket: " + e.getMessage());
        }

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (database != null) {
            database.close();
        }

        System.out.println("[SERVER] Servidor detenido correctamente.\n");
    }

    /**
     * Cierra las subastas expiradas de forma segura y notifica a los participantes.
     * Este metodo es invocado periodicamente por el planificador.
     */
    private void closeExpiredAuctionsSafely() {
        if (!running || auctionManager == null) {
            return;
        }

        try {
            List<Auction> closedAuctions = auctionManager.closeExpiredAuctions();
            if (notificationService == null) {
                return;
            }

            for (Auction auction : closedAuctions) {
                notificationService.notifyAuctionClosed(
                    auction.getId(),
                    auction.getTitle(),
                    auction.getCurrentWinner(),
                    auction.getCurrentPrice(),
                    auction.getSeller()
                );
            }
        } catch (Exception e) {
            System.err.println("[SERVER] Error cerrando subastas expiradas: " + e.getMessage());
        }
    }

    /**
     * Punto de entrada principal del servidor.
     * Parsea los argumentos de linea de comandos y arranca el servidor.
     *
     * @param args argumentos de linea de comandos (puerto y --ssl)
     */
    public static void main(String[] args) {
        int port = Constants.SERVER_PORT;
        boolean ssl = true;

        for (String arg : args) {
            if ("--ssl".equalsIgnoreCase(arg) || "-ssl".equalsIgnoreCase(arg)) {
                ssl = true;
            } else {
                try {
                    port = Integer.parseInt(arg);
                } catch (NumberFormatException ignored) {
                    // Ignorar argumentos no numericos
                }
            }
        }

        NetAuctionServer server = new NetAuctionServer(port, ssl);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SERVER] Senal de apagado recibida...");
            server.stop();
        }));

        try {
            server.start();
        } catch (IOException e) {
            System.err.println("[SERVER] Error fatal: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
