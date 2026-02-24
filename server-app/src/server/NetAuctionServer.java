package server;

import common.Constants;
import server.manager.AuctionManager;
import server.manager.SessionManager;
import server.manager.UserManager;
import server.persistence.Database;
import server.security.SSLConfig;
import server.service.NotificationService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Servidor principal de NetAuction.
 */
public class NetAuctionServer {

    private final int port;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private ProtocolHandler protocolHandler;
    private UserManager userManager;
    private SessionManager sessionManager;
    private AuctionManager auctionManager;
    private NotificationService notificationService;
    private Database database;
    private volatile boolean running;
    private boolean sslEnabled;

    public NetAuctionServer(int port) {
        this(port, false);
    }

    public NetAuctionServer(int port, boolean sslEnabled) {
        this.port = port;
        this.running = false;
        this.sslEnabled = sslEnabled;
    }

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

        if (sslEnabled) {
            try {
                serverSocket = SSLConfig.createServerSocket(port);
                System.out.println("[SERVER] SSL/TLS habilitado");
            } catch (Exception e) {
                System.err.println("[SERVER] Error habilitando SSL: " + e.getMessage());
                System.out.println("[SERVER] Usando conexion sin cifrar...");
                serverSocket = new ServerSocket(port);
                sslEnabled = false;
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

    public void stop() {
        running = false;
        System.out.println("\n[SERVER] Iniciando apagado ordenado...");

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

    public static void main(String[] args) {
        int port = Constants.SERVER_PORT;
        boolean ssl = false;

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

