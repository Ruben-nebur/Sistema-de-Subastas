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
 */
public class NetAuctionClient {

    private ServerConnection connection;
    private ServerListener serverListener;
    private ExecutorService listenerExecutor;
    private String sessionToken;
    private String currentUser;
    private BufferedReader consoleReader;
    private volatile boolean running;

    public NetAuctionClient() {
        this(false);
    }

    public NetAuctionClient(boolean sslEnabled) {
        this.connection = new ServerConnection(sslEnabled);
        this.consoleReader = new BufferedReader(new InputStreamReader(System.in));
        this.running = true;
    }

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

    private void printHelp() {
        System.out.println("COMANDOS DISPONIBLES");
        System.out.println("  register <user> <password> <email>");
        System.out.println("  login <user> <password>");
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

    private void printStatus() {
        if (sessionToken != null) {
            System.out.println("Usuario: " + currentUser);
            System.out.println("Token:   " + sessionToken.substring(0, 8) + "...");
            System.out.println("Estado:  Conectado y autenticado");
        } else {
            System.out.println("Estado: Conectado pero NO autenticado");
        }
    }

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

        if (response != null && response.isSuccess()) {
            sessionToken = response.getDataString("token");
            currentUser = response.getDataString("username");
            System.out.println("Sesion iniciada como: " + currentUser);
        }
    }

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

    private void printFormattedResponse(Message response) {
        String status = response.getDataString("status");
        String message = response.getDataString("message");

        if (Constants.STATUS_OK.equals(status)) {
            System.out.println("OK " + message);
        } else {
            System.out.println("ERROR " + message);
        }
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 3) + "...";
    }

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

    public static void main(String[] args) {
        String host = Constants.SERVER_HOST;
        int port = Constants.SERVER_PORT;
        boolean ssl = false;

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

