package client;

import common.Constants;
import common.Message;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Cliente de consola para NetAuction.
 * Permite al usuario interactuar con el servidor de subastas mediante
 * comandos de texto introducidos por la terminal.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class NetAuctionClient {

    /** Conexion con el servidor */
    private ServerConnection connection;

    /** Listener de mensajes del servidor */
    private ServerListener serverListener;

    /** Executor para el hilo del listener */
    private ExecutorService listenerExecutor;

    /** Token de sesion del usuario autenticado */
    private String sessionToken;

    /** Nombre del usuario autenticado */
    private String currentUser;

    /** Reader de la consola para entrada del usuario */
    private BufferedReader consoleReader;

    /** Indica si el cliente esta en ejecucion */
    private volatile boolean running;

    /**
     * Constructor con SSL habilitado por defecto.
     */
    public NetAuctionClient() {
        this(true);
    }

    /**
     * Constructor con configuracion de SSL.
     *
     * @param sslEnabled true para habilitar SSL/TLS
     */
    public NetAuctionClient(boolean sslEnabled) {
        this.connection = new ServerConnection(sslEnabled);
        this.consoleReader = new BufferedReader(new InputStreamReader(System.in));
        this.running = true;
    }

    /**
     * Inicia el cliente conectandose al servidor y procesando comandos del usuario.
     *
     * @param host host del servidor
     * @param port puerto del servidor
     */
    public void start(String host, int port) {
        try {
            System.out.println("[CLIENT] Conectando a " + host + ":" + port + "...");

            connection.connect(host, port);

            serverListener = new ServerListener(connection.getReader());
            listenerExecutor = Executors.newSingleThreadExecutor();
            listenerExecutor.submit(serverListener);

            System.out.println("[CLIENT] Conectado.");
            printHelp();

            while (running && connection.isConnected() && serverListener.isRunning()) {
                System.out.print("\n> ");
                String input = consoleReader.readLine();

                if (input == null || input.trim().isEmpty()) {
                    continue;
                }

                processCommand(input.trim());
            }

        } catch (IOException e) {
            System.err.println("[CLIENT] Error de conexion: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /**
     * Procesa un comando introducido por el usuario y lo enruta al manejador correspondiente.
     *
     * @param input linea de texto introducida por el usuario
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
                case "cancel":
                    handleCancelAuction(args);
                    break;
                case "block":
                    handleBlockUser(args);
                    break;
                case "unblock":
                    handleUnblockUser(args);
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
            System.err.println("[CLIENT] Error de comunicacion: " + e.getMessage());
        }
    }

    /**
     * Muestra la ayuda con los comandos disponibles.
     */
    private void printHelp() {
        System.out.println("COMANDOS DISPONIBLES");
        System.out.println("  register <user> <email>      (pedira la contrasena de forma oculta)");
        System.out.println("  login <user>                (pedira la contrasena de forma oculta)");
        System.out.println("  login <user> <password>     (compatibilidad)");
        System.out.println("  logout");
        System.out.println("  create <titulo> <precio> <minutos>");
        System.out.println("  list");
        System.out.println("  detail <auctionId>");
        System.out.println("  bid <auctionId> <cantidad>");
        System.out.println("  cancel <auctionId>");
        System.out.println("  block <username>");
        System.out.println("  unblock <username>");
        System.out.println("  status");
        System.out.println("  raw <json>");
        System.out.println("  help");
        System.out.println("  exit");
    }

    /**
     * Muestra el estado actual de la conexion y sesion.
     */
    private void printStatus() {
        if (sessionToken != null) {
            System.out.println("Usuario: " + currentUser);
            System.out.println("Token:   " + sessionToken.substring(0, 8) + "...");
            System.out.println("Estado:  Conectado y autenticado");
        } else {
            System.out.println("Estado: Conectado pero NO autenticado");
        }
    }

    /**
     * Maneja el comando de registro de usuario.
     *
     * @param args argumentos del comando (user email)
     * @throws IOException si ocurre un error de comunicacion
     */
    private void handleRegister(String args) throws IOException {
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            System.out.println("[CLIENT] Uso: register <user> <email>");
            return;
        }

        String password = readSecret("Contrasena: ");
        if (password == null || password.isEmpty()) {
            System.out.println("[CLIENT] La contrasena es obligatoria");
            return;
        }

        Message request = new Message(Constants.ACTION_REGISTER);
        request.addData("user", parts[0]);
        request.addData("password", password);
        request.addData("email", parts[1]);

        sendAndReceive(request);
    }

    /**
     * Maneja el comando de inicio de sesion.
     *
     * @param args argumentos del comando (user [password])
     * @throws IOException si ocurre un error de comunicacion
     */
    private void handleLogin(String args) throws IOException {
        String[] parts = args.split("\\s+");
        if (parts.length < 1 || parts[0].isEmpty()) {
            System.out.println("[CLIENT] Uso: login <user>");
            return;
        }

        String password = parts.length >= 2 ? parts[1] : readSecret("Contrasena: ");
        if (password == null || password.isEmpty()) {
            System.out.println("[CLIENT] La contrasena es obligatoria");
            return;
        }

        Message request = new Message(Constants.ACTION_LOGIN);
        request.addData("user", parts[0]);
        request.addData("password", password);

        Message response = sendAndReceive(request);

        if (response != null && response.isSuccess()) {
            sessionToken = response.getDataString("token");
            currentUser = response.getDataString("username");
            System.out.println("Sesion iniciada como: " + currentUser);
        }
    }

    /**
     * Maneja el comando de cierre de sesion.
     *
     * @throws IOException si ocurre un error de comunicacion
     */
    private void handleLogout() throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] No hay sesion activa");
            return;
        }

        Message request = new Message(Constants.ACTION_LOGOUT);
        request.setToken(sessionToken);

        sendAndReceive(request);

        sessionToken = null;
        currentUser = null;
    }

    /**
     * Maneja el comando de creacion de subasta.
     *
     * @param args argumentos del comando (titulo precio minutos [descripcion])
     * @throws IOException si ocurre un error de comunicacion
     */
    private void handleCreateAuction(String args) throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] Debe iniciar sesion primero");
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
            request.addData("description", parts.length > 3 ? parts[3] : "Articulo en subasta");
            request.addData("startPrice", Double.parseDouble(parts[1]));
            request.addData("durationMinutes", Integer.parseInt(parts[2]));

            sendAndReceive(request);
        } catch (NumberFormatException e) {
            System.out.println("[CLIENT] Error: precio y duracion deben ser numeros");
        }
    }

    /**
     * Maneja el comando de listado de subastas activas.
     *
     * @throws IOException si ocurre un error de comunicacion
     */
    private void handleListAuctions() throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] Debe iniciar sesion primero");
            return;
        }

        Message request = new Message(Constants.ACTION_LIST_AUCTIONS);
        request.setToken(sessionToken);

        Message response = sendAndReceive(request);

        if (response != null && response.isSuccess()) {
            var auctions = response.getData().getAsJsonArray("auctions");
            if (auctions != null && auctions.size() > 0) {
                for (int i = 0; i < auctions.size(); i++) {
                    var auction = auctions.get(i).getAsJsonObject();
                    System.out.printf("%s | %s | %.2f | %s | pujas:%d%n",
                        auction.get("id").getAsString(),
                        truncate(auction.get("title").getAsString(), 20),
                        auction.get("currentPrice").getAsDouble(),
                        auction.get("remainingTime").getAsString(),
                        auction.get("bidCount").getAsInt());
                }
            }
        }
    }

    /**
     * Maneja el comando de detalle de una subasta.
     *
     * @param args argumentos del comando (auctionId)
     * @throws IOException si ocurre un error de comunicacion
     */
    private void handleAuctionDetail(String args) throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] Debe iniciar sesion primero");
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
            System.out.println("ID: " + response.getDataString("id"));
            System.out.println("Titulo: " + response.getDataString("title"));
            System.out.println("Vendedor: " + response.getDataString("seller"));
            System.out.println("Estado: " + response.getDataString("status"));
            System.out.println("Precio actual: " + response.getDataDouble("currentPrice", 0));
            System.out.println("Pujas: " + response.getDataInt("bidCount", 0));
            System.out.println("Tiempo restante: " + response.getDataString("remainingTime"));
        }
    }

    /**
     * Maneja el comando de puja en una subasta.
     *
     * @param args argumentos del comando (auctionId cantidad)
     * @throws IOException si ocurre un error de comunicacion
     */
    private void handleBid(String args) throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] Debe iniciar sesion primero");
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
            System.out.println("[CLIENT] Error: la cantidad debe ser un numero");
        }
    }

    /**
     * Maneja el comando de cancelacion de subasta.
     *
     * @param args argumentos del comando (auctionId)
     * @throws IOException si ocurre un error de comunicacion
     */
    private void handleCancelAuction(String args) throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] Debe iniciar sesion primero");
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
     * Maneja el comando de bloqueo de usuario.
     *
     * @param args argumentos del comando (username)
     * @throws IOException si ocurre un error de comunicacion
     */
    private void handleBlockUser(String args) throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] Debe iniciar sesion primero");
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
     * Maneja el comando de desbloqueo de usuario.
     *
     * @param args argumentos del comando (username)
     * @throws IOException si ocurre un error de comunicacion
     */
    private void handleUnblockUser(String args) throws IOException {
        if (sessionToken == null) {
            System.out.println("[CLIENT] Debe iniciar sesion primero");
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
     * Maneja el comando de envio de mensaje JSON en crudo.
     *
     * @param json cadena JSON del mensaje a enviar
     * @throws IOException si ocurre un error de comunicacion
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
            System.err.println("[CLIENT] JSON invalido: " + e.getMessage());
        }
    }

    /**
     * Envia un mensaje al servidor y espera la respuesta.
     *
     * @param request mensaje a enviar
     * @return mensaje de respuesta del servidor o null si hay timeout
     * @throws IOException si ocurre un error de comunicacion
     */
    private Message sendAndReceive(Message request) throws IOException {
        System.out.println("[CLIENT] Enviando: " + request.getAction());

        connection.send(request);
        Message response = serverListener.getNextResponse(10000);

        if (response != null) {
            printFormattedResponse(response);
        } else {
            System.out.println("[CLIENT] Timeout esperando respuesta del servidor");
            running = false;
        }

        return response;
    }

    /**
     * Imprime la respuesta del servidor formateada.
     *
     * @param response mensaje de respuesta a imprimir
     */
    private void printFormattedResponse(Message response) {
        String status = response.getDataString("status");
        String message = response.getDataString("message");

        if (Constants.STATUS_OK.equals(status)) {
            System.out.println("OK " + message);
        } else {
            System.out.println("ERROR " + message);
        }
    }

    /**
     * Trunca una cadena a la longitud maxima especificada.
     *
     * @param str cadena a truncar
     * @param maxLen longitud maxima
     * @return cadena truncada con "..." si excede la longitud
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }

    /**
     * Lee una contrasena de forma segura desde la consola.
     * Usa Console.readPassword si esta disponible para ocultar la entrada.
     *
     * @param prompt texto a mostrar al usuario
     * @return contrasena introducida o null
     * @throws IOException si ocurre un error de lectura
     */
    private String readSecret(String prompt) throws IOException {
        Console console = System.console();
        if (console != null) {
            char[] secret = console.readPassword(prompt);
            return secret != null ? new String(secret) : null;
        }

        System.out.print(prompt);
        return consoleReader.readLine();
    }

    /**
     * Desconecta el cliente del servidor liberando todos los recursos.
     */
    private void disconnect() {
        if (serverListener != null) {
            serverListener.stop();
        }

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

        if (connection != null) {
            connection.disconnect();
            System.out.println("[CLIENT] Desconectado del servidor.");
        }
    }

    /**
     * Punto de entrada principal del cliente de consola.
     * Parsea los argumentos de linea de comandos y arranca el cliente.
     *
     * @param args argumentos de linea de comandos (host, puerto, --ssl)
     */
    public static void main(String[] args) {
        String host = Constants.SERVER_HOST;
        int port = Constants.SERVER_PORT;
        boolean ssl = true;

        for (int i = 0; i < args.length; i++) {
            if ("--ssl".equalsIgnoreCase(args[i]) || "-ssl".equalsIgnoreCase(args[i])) {
                ssl = true;
            } else if (i == 0 && !args[i].startsWith("-")) {
                host = args[i];
            } else if (i == 1 && !args[i].startsWith("-")) {
                try {
                    port = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    System.err.println("Puerto invalido: " + args[i] + ". Usando puerto por defecto.");
                }
            }
        }

        NetAuctionClient client = new NetAuctionClient(ssl);
        client.start(host, port);
    }
}
