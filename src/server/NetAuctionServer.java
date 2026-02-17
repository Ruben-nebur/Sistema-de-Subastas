package server;

import common.Constants;
import server.manager.UserManager;
import server.manager.SessionManager;
import server.manager.AuctionManager;
import server.persistence.Database;
import server.service.NotificationService;
import server.service.AuctionScheduler;
import server.security.SSLConfig;
import server.util.AuditLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLServerSocket;

/**
 * Servidor principal de NetAuction.
 * Acepta conexiones TCP y asigna cada cliente a un hilo del pool.
 * Gestiona todos los managers y servicios del sistema.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class NetAuctionServer {

    /** Puerto del servidor */
    private final int port;

    /** Socket del servidor */
    private ServerSocket serverSocket;

    /** Pool de hilos para atender clientes */
    private ExecutorService executorService;

    /** Procesador de protocolo compartido */
    private ProtocolHandler protocolHandler;

    /** Gestor de usuarios */
    private UserManager userManager;

    /** Gestor de sesiones */
    private SessionManager sessionManager;

    /** Gestor de subastas */
    private AuctionManager auctionManager;

    /** Servicio de notificaciones */
    private NotificationService notificationService;

    /** Planificador de subastas */
    private AuctionScheduler auctionScheduler;

    /** Logger de auditoría */
    private AuditLogger auditLogger;

    /** Base de datos para persistencia */
    private Database database;

    /** Indica si el servidor está ejecutándose */
    private volatile boolean running;

    /** Indica si SSL está habilitado */
    private boolean sslEnabled;

    /**
     * Constructor del servidor.
     *
     * @param port puerto en el que escuchar
     */
    public NetAuctionServer(int port) {
        this(port, false);
    }

    /**
     * Constructor del servidor con opción SSL.
     *
     * @param port puerto en el que escuchar
     * @param sslEnabled habilitar SSL/TLS
     */
    public NetAuctionServer(int port, boolean sslEnabled) {
        this.port = port;
        this.running = false;
        this.sslEnabled = sslEnabled;
    }

    /**
     * Inicia el servidor.
     * Crea el pool de hilos, inicializa managers y comienza a aceptar conexiones.
     *
     * @throws IOException si no se puede abrir el socket
     */
    public void start() throws IOException {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           NetAuction Server v1.0                         ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Inicializando componentes...                            ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        // Inicializar logger de auditoría
        auditLogger = new AuditLogger();

        // Inicializar base de datos
        try {
            database = new Database();
            database.initialize();
        } catch (SQLException e) {
            System.err.println("[SERVER] Error inicializando BD: " + e.getMessage());
            System.out.println("[SERVER] Continuando sin persistencia...");
            database = null;
        }

        // Inicializar managers
        userManager = new UserManager();
        sessionManager = new SessionManager();
        auctionManager = new AuctionManager();

        // Configurar persistencia si la BD está disponible
        if (database != null) {
            userManager.setDatabase(database);
            auctionManager.setDatabase(database);
        }

        // Cargar datos desde BD
        userManager.loadFromDatabase();
        auctionManager.loadFromDatabase();

        // Inicializar servicio de notificaciones
        notificationService = new NotificationService();

        // Crear procesador de protocolo
        protocolHandler = new ProtocolHandler(userManager, sessionManager, auctionManager);
        protocolHandler.setNotificationService(notificationService);
        protocolHandler.setAuditLogger(auditLogger);

        // Inicializar planificador de subastas
        auctionScheduler = new AuctionScheduler(auctionManager, notificationService, auditLogger);
        auctionScheduler.start();

        // Crear pool de hilos
        executorService = Executors.newFixedThreadPool(Constants.THREAD_POOL_SIZE);

        // Abrir socket del servidor (con o sin SSL)
        if (sslEnabled) {
            try {
                serverSocket = SSLConfig.createServerSocket(port);
                System.out.println("[SERVER] SSL/TLS habilitado");
            } catch (Exception e) {
                System.err.println("[SERVER] Error habilitando SSL: " + e.getMessage());
                System.out.println("[SERVER] Usando conexión sin cifrar...");
                serverSocket = new ServerSocket(port);
                sslEnabled = false;
            }
        } else {
            serverSocket = new ServerSocket(port);
        }
        running = true;

        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║           SERVIDOR INICIADO CORRECTAMENTE                ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Puerto: " + String.format("%-47d", port) + " ║");
        System.out.println("║  SSL/TLS: " + String.format("%-46s", (sslEnabled ? "HABILITADO" : "DESHABILITADO")) + " ║");
        System.out.println("║  Pool de hilos: " + String.format("%-40d", Constants.THREAD_POOL_SIZE) + " ║");
        System.out.println("║  Duración sesión: " + String.format("%-37s", (Constants.SESSION_DURATION_MS / 60000) + " minutos") + " ║");
        System.out.println("║  Intervalo subasta: " + String.format("%-35s", Constants.AUCTION_CHECK_INTERVAL_MS + "ms") + " ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");
        System.out.println("║  Estado: ACTIVO - Esperando conexiones...                ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");

        auditLogger.log("SERVER_START", "SYSTEM", "localhost",
            "Servidor iniciado en puerto " + port);

        // Bucle principal de aceptación de conexiones
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();

                // Crear handler para el cliente
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.setProtocolHandler(protocolHandler);

                // Enviar a pool de hilos
                executorService.submit(clientHandler);

            } catch (IOException e) {
                if (running) {
                    System.err.println("[SERVER] Error aceptando conexión: " + e.getMessage());
                    auditLogger.log("SERVER_ERROR", "SYSTEM", "localhost",
                        "Error aceptando conexión: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Detiene el servidor de forma ordenada.
     */
    public void stop() {
        running = false;
        System.out.println("\n[SERVER] Iniciando apagado ordenado...");

        // Detener planificador de subastas
        if (auctionScheduler != null) {
            auctionScheduler.stop();
        }

        // Detener gestor de sesiones
        if (sessionManager != null) {
            sessionManager.shutdown();
        }

        // Limpiar notificaciones
        if (notificationService != null) {
            notificationService.clear();
        }

        // Cerrar socket del servidor
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Error cerrando socket: " + e.getMessage());
        }

        // Apagar pool de hilos
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

        // Cerrar base de datos
        if (database != null) {
            database.close();
        }

        // Log de apagado y cerrar logger
        if (auditLogger != null) {
            auditLogger.log("SERVER_STOP", "SYSTEM", "localhost", "Servidor detenido");
            auditLogger.close();
        }

        System.out.println("[SERVER] Servidor detenido correctamente.\n");
    }

    /**
     * Obtiene el gestor de usuarios.
     *
     * @return gestor de usuarios
     */
    public UserManager getUserManager() {
        return userManager;
    }

    /**
     * Obtiene el gestor de sesiones.
     *
     * @return gestor de sesiones
     */
    public SessionManager getSessionManager() {
        return sessionManager;
    }

    /**
     * Obtiene el gestor de subastas.
     *
     * @return gestor de subastas
     */
    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    /**
     * Obtiene el servicio de notificaciones.
     *
     * @return servicio de notificaciones
     */
    public NotificationService getNotificationService() {
        return notificationService;
    }

    /**
     * Punto de entrada principal.
     *
     * @param args argumentos de línea de comandos (opcional: puerto, --ssl)
     */
    public static void main(String[] args) {
        int port = Constants.SERVER_PORT;
        boolean ssl = false;

        // Procesar argumentos
        for (String arg : args) {
            if ("--ssl".equalsIgnoreCase(arg) || "-ssl".equalsIgnoreCase(arg)) {
                ssl = true;
            } else {
                try {
                    port = Integer.parseInt(arg);
                } catch (NumberFormatException e) {
                    // Ignorar argumentos no numéricos
                }
            }
        }

        NetAuctionServer server = new NetAuctionServer(port, ssl);

        // Hook para apagado ordenado
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SERVER] Señal de apagado recibida...");
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
