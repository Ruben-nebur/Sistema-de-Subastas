package client;

import common.Constants;
import common.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Cliente de consola para NetAuction.
 * Permite probar la comunicación con el servidor de forma interactiva.
 * Soporta notificaciones push en tiempo real.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class NetAuctionClient {

    /** Conexión con el servidor */
    private ServerConnection connection;

    /** Listener para mensajes del servidor */
    private ServerListener serverListener;

    /** Executor para el listener */
    private ExecutorService listenerExecutor;

    /** Token de sesión actual */
    private String sessionToken;

    /** Username del usuario logueado */
    private String currentUser;

    /** Rol del usuario */
    private String currentRole;

    /** Reader para entrada de consola */
    private BufferedReader consoleReader;

    /** Indica si el cliente está ejecutándose */
    private volatile boolean running;

    /** Indica si SSL está habilitado */
    private boolean sslEnabled;

    /**
     * Constructor del cliente.
     */
    public NetAuctionClient() {
        this(false);
    }

    /**
     * Constructor del cliente con opción SSL.
     *
     * @param sslEnabled habilitar SSL/TLS
     */
    public NetAuctionClient(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
        this.connection = new ServerConnection(sslEnabled);
        this.consoleReader = new BufferedReader(new InputStreamReader(System.in));
        this.running = true;
    }

    /**
     * Inicia el cliente de consola.
     *
     * @param host dirección del servidor
     * @param port puerto del servidor
     */
    public void start(String host, int port) {
        try {
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║           NetAuction Client v1.0                         ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            System.out.println("[CLIENT] Conectando a " + host + ":" + port + "...");

            connection.connect(host, port);

            // Iniciar listener en hilo separado
            serverListener = new ServerListener(connection.getReader());
            listenerExecutor = Executors.newSingleThreadExecutor();
            listenerExecutor.submit(serverListener);

            System.out.println("[CLIENT] ¡Conectado exitosamente!");
            System.out.println("[CLIENT] Escuchando notificaciones en tiempo real...\n");
            printHelp();

            // Bucle principal de comandos
            while (running && connection.isConnected() && serverListener.isRunning()) {
                System.out.print("\n> ");
                String input = consoleReader.readLine();

                if (input == null || input.trim().isEmpty()) {
                    continue;
                }

                processCommand(input.trim());
            }

        } catch (IOException e) {
            System.err.println("[CLIENT] Error de conexión: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /**
     * Procesa un comando del usuario.
     *
     * @param input comando introducido
     */
    private void processCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        try {
            switch (command) {
                case "help":
                case "?":
                    printHelp();
                    break;

                case "register":
                    handleRegister(args);
                    break;

                case "login":
                    handleLogin(args);
                    break;

                case "logout":
                    handleLogout();
                    break;

                case "create":
                    handleCreateAuction(args);
                    break;

                case "list":
                    handleListAuctions();
                    break;

                case "detail":
                    handleAuctionDetail(args);
                    break;

                case "bid":
                    handleBid(args);
                    break;

                case "history":
                    handleHistory();
                    break;

                case "cancel":
                    handleCancelAuction(args);
                    break;

                case "block":
                    handleBlockUser(args);
                    break;

                case "unblock":
                    handleUnblockUser(args);
                    break;

                case "logs":
                    handleViewLogs();
                    break;

                case "status":
                    printStatus();
                    break;

                case "raw":
                    handleRawMessage(args);
                    break;

                case "exit":
                case "quit":
                    running = false;
                    System.out.println("[CLIENT] Saliendo...");
                    break;

                default:
                    System.out.println("[CLIENT] Comando desconocido: " + command);
                    System.out.println("         Usa 'help' para ver los comandos disponibles.");
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Error de comunicación: " + e.getMessage());
        }
    }

    /**
     * Muestra la ayuda de comandos.
     */
    private void printHelp() {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("                       COMANDOS DISPONIBLES");
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("  AUTENTICACIÓN:");
        System.out.println("    register <user> <password> <email>  - Registrar nuevo usuario");
        System.out.println("    login <user> <password>             - Iniciar sesión");
        System.out.println("    logout                              - Cerrar sesión");
        System.out.println("");
        System.out.println("  SUBASTAS:");
        System.out.println("    create <titulo> <precio> <minutos>  - Crear subasta");
        System.out.println("    list                                - Listar subastas activas");
        System.out.println("    detail <auctionId>                  - Ver detalle de subasta");
        System.out.println("    bid <auctionId> <cantidad>          - Realizar puja");
        System.out.println("    history                             - Ver mi historial");
        System.out.println("");
        System.out.println("  ADMINISTRACIÓN (solo ADMIN):");
        System.out.println("    cancel <auctionId>                  - Cancelar subasta");
        System.out.println("    block <username>                    - Bloquear usuario");
        System.out.println("    unblock <username>                  - Desbloquear usuario");
        System.out.println("    logs                                - Ver logs de auditoría");
        System.out.println("");
        System.out.println("  OTROS:");
        System.out.println("    status                              - Ver estado de la sesión");
        System.out.println("    raw <json>                          - Enviar JSON directo");
        System.out.println("    help                                - Mostrar esta ayuda");
        System.out.println("    exit                                - Salir del cliente");
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }

    /**
     * Muestra el estado actual de la sesión.
     */
    private void printStatus() {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("                       ESTADO DE LA SESIÓN");
        System.out.println("═══════════════════════════════════════════════════════════════════");
        if (sessionToken != null) {
            System.out.println("  Usuario:  " + currentUser);
            System.out.println("  Rol:      " + currentRole);
            System.out.println("  Token:    " + sessionToken.substring(0, 8) + "...");
            System.out.println("  Estado:   Conectado y autenticado");
        } else {
            System.out.println("  Estado:   Conectado pero NO autenticado");
            System.out.println("  Use 'login' o 'register' para autenticarse");
        }
        System.out.println("═══════════════════════════════════════════════════════════════════");
    }

    /**
     * Maneja el comando register.
     */
    private void handleRegister(String args) throws IOException {
        String[] parts = args.split("\\s+");
        if (parts.length < 3) {
            System.out.println("[CLIENT] Uso: register <user> <password> <email>");
            return;
        }

        Message request = new Message(Constants.ACTION_REGISTER);
        request.addData("user", parts[0]);
        request.addData("password", parts[1]);
        request.addData("email", parts[2]);

        sendAndReceive(request);
    }

    /**
     * Maneja el comando login.
     */
    private void handleLogin(String args) throws IOException {
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            System.out.println("[CLIENT] Uso: login <user> <password>");
            return;
        }

        Message request = new Message(Constants.ACTION_LOGIN);
        request.addData("user", parts[0]);
        request.addData("password", parts[1]);

        Message response = sendAndReceive(request);

        // Guardar token si el login fue exitoso
        if (response != null && response.isSuccess()) {
            sessionToken = response.getDataString("token");
            currentUser = response.getDataString("username");
            currentRole = response.getDataString("role");
            System.out.println("    Sesión iniciada como: " + currentUser + " (" + currentRole + ")");
        }
    }

    /**
     * Maneja el comando logout.
     */
    private void handleLogout() throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] No hay sesión activa");
            return;
        }

        Message request = new Message(Constants.ACTION_LOGOUT);
        request.setToken(sessionToken);

        sendAndReceive(request);

        sessionToken = null;
        currentUser = null;
        currentRole = null;
    }

    /**
     * Maneja el comando create (crear subasta).
     */
    private void handleCreateAuction(String args) throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] Debe iniciar sesión primero");
            return;
        }

        String[] parts = args.split("\\s+");
        if (parts.length < 3) {
            System.out.println("[CLIENT] Uso: create <titulo> <precio> <minutos>");
            return;
        }

        try {
            Message request = new Message(Constants.ACTION_CREATE_AUCTION);
            request.setToken(sessionToken);
            request.addData("title", parts[0]);
            request.addData("description", parts.length > 3 ? parts[3] : "Artículo en subasta");
            request.addData("startPrice", Double.parseDouble(parts[1]));
            request.addData("durationMinutes", Integer.parseInt(parts[2]));

            sendAndReceive(request);
        } catch (NumberFormatException e) {
            System.out.println("[CLIENT] Error: precio y duración deben ser números");
        }
    }

    /**
     * Maneja el comando list (listar subastas).
     */
    private void handleListAuctions() throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] Debe iniciar sesión primero");
            return;
        }

        Message request = new Message(Constants.ACTION_LIST_AUCTIONS);
        request.setToken(sessionToken);

        Message response = sendAndReceive(request);

        if (response != null && response.isSuccess()) {
            var auctions = response.getData().getAsJsonArray("auctions");
            if (auctions != null && auctions.size() > 0) {
                System.out.println("\n    ╔═══════════════════════════════════════════════════════════╗");
                System.out.println("    ║                   SUBASTAS ACTIVAS                        ║");
                System.out.println("    ╠═══════════════════════════════════════════════════════════╣");
                for (int i = 0; i < auctions.size(); i++) {
                    var auction = auctions.get(i).getAsJsonObject();
                    System.out.printf("    ║  ID: %-20s Precio: %-10.2f  ║%n",
                        auction.get("id").getAsString(),
                        auction.get("currentPrice").getAsDouble());
                    System.out.printf("    ║  Título: %-15s Tiempo: %-10s  ║%n",
                        truncate(auction.get("title").getAsString(), 15),
                        auction.get("remainingTime").getAsString());
                    System.out.printf("    ║  Vendedor: %-13s Pujas: %-11d  ║%n",
                        truncate(auction.get("seller").getAsString(), 13),
                        auction.get("bidCount").getAsInt());
                    if (i < auctions.size() - 1) {
                        System.out.println("    ╠───────────────────────────────────────────────────────────╣");
                    }
                }
                System.out.println("    ╚═══════════════════════════════════════════════════════════╝");
            }
        }
    }

    /**
     * Maneja el comando detail (detalle de subasta).
     */
    private void handleAuctionDetail(String args) throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] Debe iniciar sesión primero");
            return;
        }

        if (args.isEmpty()) {
            System.out.println("[CLIENT] Uso: detail <auctionId>");
            return;
        }

        Message request = new Message(Constants.ACTION_AUCTION_DETAIL);
        request.setToken(sessionToken);
        request.addData("auctionId", args);

        Message response = sendAndReceive(request);

        if (response != null && response.isSuccess()) {
            System.out.println("\n    ╔═══════════════════════════════════════════════════════════╗");
            System.out.println("    ║                   DETALLE DE SUBASTA                      ║");
            System.out.println("    ╠═══════════════════════════════════════════════════════════╣");
            System.out.printf("    ║  ID: %-50s  ║%n", response.getDataString("id"));
            System.out.printf("    ║  Título: %-46s  ║%n", truncate(response.getDataString("title"), 46));
            System.out.printf("    ║  Vendedor: %-44s  ║%n", response.getDataString("seller"));
            System.out.printf("    ║  Estado: %-46s  ║%n", response.getDataString("status"));
            System.out.println("    ╠───────────────────────────────────────────────────────────╣");
            System.out.printf("    ║  Precio inicial: %-38.2f  ║%n", response.getDataDouble("startPrice", 0));
            System.out.printf("    ║  Precio actual:  %-38.2f  ║%n", response.getDataDouble("currentPrice", 0));
            System.out.printf("    ║  Líder actual:   %-38s  ║%n",
                response.getDataString("currentWinner") != null ? response.getDataString("currentWinner") : "Sin pujas");
            System.out.printf("    ║  Número de pujas: %-37d  ║%n", response.getDataInt("bidCount", 0));
            System.out.printf("    ║  Tiempo restante: %-37s  ║%n", response.getDataString("remainingTime"));
            System.out.println("    ╚═══════════════════════════════════════════════════════════╝");
        }
    }

    /**
     * Maneja el comando bid (realizar puja).
     */
    private void handleBid(String args) throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] Debe iniciar sesión primero");
            return;
        }

        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            System.out.println("[CLIENT] Uso: bid <auctionId> <cantidad>");
            return;
        }

        try {
            Message request = new Message(Constants.ACTION_BID);
            request.setToken(sessionToken);
            request.addData("auctionId", parts[0]);
            request.addData("amount", Double.parseDouble(parts[1]));

            sendAndReceive(request);
        } catch (NumberFormatException e) {
            System.out.println("[CLIENT] Error: la cantidad debe ser un número");
        }
    }

    /**
     * Maneja el comando history (mi historial).
     */
    private void handleHistory() throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] Debe iniciar sesión primero");
            return;
        }

        Message request = new Message(Constants.ACTION_MY_HISTORY);
        request.setToken(sessionToken);

        Message response = sendAndReceive(request);

        if (response != null && response.isSuccess()) {
            System.out.println("\n    Mis subastas creadas: " + response.getDataInt("myAuctionsCount", 0));
            System.out.println("    Subastas donde he pujado: " + response.getDataInt("biddedCount", 0));
            System.out.println("    Subastas ganadas: " + response.getDataInt("wonCount", 0));
        }
    }

    /**
     * Maneja el comando cancel (cancelar subasta - ADMIN).
     */
    private void handleCancelAuction(String args) throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] Debe iniciar sesión primero");
            return;
        }

        if (args.isEmpty()) {
            System.out.println("[CLIENT] Uso: cancel <auctionId>");
            return;
        }

        Message request = new Message(Constants.ACTION_CANCEL_AUCTION);
        request.setToken(sessionToken);
        request.addData("auctionId", args);

        sendAndReceive(request);
    }

    /**
     * Maneja el comando block (bloquear usuario - ADMIN).
     */
    private void handleBlockUser(String args) throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] Debe iniciar sesión primero");
            return;
        }

        if (args.isEmpty()) {
            System.out.println("[CLIENT] Uso: block <username>");
            return;
        }

        Message request = new Message(Constants.ACTION_BLOCK_USER);
        request.setToken(sessionToken);
        request.addData("username", args);
        request.addData("blocked", true);

        sendAndReceive(request);
    }

    /**
     * Maneja el comando unblock (desbloquear usuario - ADMIN).
     */
    private void handleUnblockUser(String args) throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] Debe iniciar sesión primero");
            return;
        }

        if (args.isEmpty()) {
            System.out.println("[CLIENT] Uso: unblock <username>");
            return;
        }

        Message request = new Message(Constants.ACTION_BLOCK_USER);
        request.setToken(sessionToken);
        request.addData("username", args);
        request.addData("blocked", false);

        sendAndReceive(request);
    }

    /**
     * Maneja el comando logs (ver logs - ADMIN).
     */
    private void handleViewLogs() throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] Debe iniciar sesión primero");
            return;
        }

        Message request = new Message(Constants.ACTION_VIEW_LOGS);
        request.setToken(sessionToken);

        Message response = sendAndReceive(request);

        if (response != null && response.isSuccess()) {
            var logs = response.getData().getAsJsonArray("logs");
            if (logs != null) {
                System.out.println("\n    ══════ LOGS DE AUDITORÍA (" + logs.size() + " entradas) ══════");
                int shown = Math.min(20, logs.size());
                for (int i = 0; i < shown; i++) {
                    System.out.println("    " + logs.get(i).getAsString());
                }
                if (logs.size() > 20) {
                    System.out.println("    ... y " + (logs.size() - 20) + " entradas más");
                }
            }
        }
    }

    /**
     * Maneja el envío de JSON crudo.
     */
    private void handleRawMessage(String json) throws IOException {
        if (json.isEmpty()) {
            System.out.println("[CLIENT] Uso: raw <json>");
            return;
        }

        try {
            Message request = Message.fromJson(json);
            if (sessionToken != null && request.getToken() == null) {
                request.setToken(sessionToken);
            }
            sendAndReceive(request);
        } catch (Exception e) {
            System.err.println("[CLIENT] JSON inválido: " + e.getMessage());
        }
    }

    /**
     * Envía un mensaje y espera la respuesta.
     *
     * @param request mensaje a enviar
     * @return respuesta del servidor
     */
    private Message sendAndReceive(Message request) throws IOException {
        System.out.println("[CLIENT] Enviando: " + request.getAction());

        connection.send(request);
        Message response = serverListener.getNextResponse(10000); // 10 segundos timeout

        if (response != null) {
            printFormattedResponse(response);
        } else {
            System.out.println("[CLIENT] Timeout esperando respuesta del servidor");
            running = false;
        }

        return response;
    }

    /**
     * Imprime la respuesta de forma formateada.
     */
    private void printFormattedResponse(Message response) {
        String status = response.getDataString("status");
        String message = response.getDataString("message");

        if (Constants.STATUS_OK.equals(status)) {
            System.out.println("    ✓ " + message);
        } else {
            System.out.println("    ✗ " + message);
        }
    }

    /**
     * Trunca una cadena a un tamaño máximo.
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }

    /**
     * Desconecta del servidor.
     */
    private void disconnect() {
        // Detener listener
        if (serverListener != null) {
            serverListener.stop();
        }

        // Apagar executor
        if (listenerExecutor != null) {
            listenerExecutor.shutdown();
            try {
                if (!listenerExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    listenerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                listenerExecutor.shutdownNow();
            }
        }

        // Cerrar conexión
        if (connection != null) {
            connection.disconnect();
            System.out.println("[CLIENT] Desconectado del servidor.");
        }
    }

    /**
     * Punto de entrada principal.
     *
     * @param args argumentos de línea de comandos (opcional: host puerto --ssl)
     */
    public static void main(String[] args) {
        String host = Constants.SERVER_HOST;
        int port = Constants.SERVER_PORT;
        boolean ssl = false;

        // Procesar argumentos
        for (int i = 0; i < args.length; i++) {
            if ("--ssl".equalsIgnoreCase(args[i]) || "-ssl".equalsIgnoreCase(args[i])) {
                ssl = true;
            } else if (i == 0 && !args[i].startsWith("-")) {
                host = args[i];
            } else if (i == 1 && !args[i].startsWith("-")) {
                try {
                    port = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    System.err.println("Puerto inválido: " + args[i] + ". Usando puerto por defecto.");
                }
            }
        }

        NetAuctionClient client = new NetAuctionClient(ssl);
        client.start(host, port);
    }
}
