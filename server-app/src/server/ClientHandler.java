package server;

import common.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Manejador de conexión de cliente.
 * Cada instancia atiende a un cliente conectado en su propio hilo.
 * Lee mensajes JSON del cliente, los procesa y envía respuestas.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class ClientHandler implements Runnable {

    /** Socket de conexión con el cliente */
    private final Socket clientSocket;

    /** Dirección IP del cliente */
    private final String clientAddress;

    /** Procesador de protocolo para manejar acciones */
    private ProtocolHandler protocolHandler;

    /** Writer para enviar mensajes al cliente */
    private PrintWriter out;

    /** Reader para recibir mensajes del cliente */
    private BufferedReader in;

    /** Indica si el handler está activo */
    private volatile boolean running;

    /** Username del cliente autenticado (null si no está autenticado) */
    private String authenticatedUser;

    /**
     * Constructor del manejador de cliente.
     *
     * @param clientSocket socket de conexión con el cliente
     */
    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.clientAddress = clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort();
        this.running = true;
        this.authenticatedUser = null;
    }

    /**
     * Establece el procesador de protocolo.
     *
     * @param protocolHandler procesador de protocolo
     */
    public void setProtocolHandler(ProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
    }

    /**
     * Obtiene el escritor de salida para enviar mensajes al cliente.
     *
     * @return PrintWriter de salida
     */
    public PrintWriter getOut() {
        return out;
    }

    /**
     * Obtiene el username del usuario autenticado.
     *
     * @return username o null si no está autenticado
     */
    public String getAuthenticatedUser() {
        return authenticatedUser;
    }

    /**
     * Establece el username del usuario autenticado.
     *
     * @param username username del usuario
     */
    public void setAuthenticatedUser(String username) {
        this.authenticatedUser = username;
    }

    /**
     * Obtiene la dirección del cliente.
     *
     * @return dirección IP:puerto del cliente
     */
    public String getClientAddress() {
        return clientAddress;
    }

    /**
     * Bucle principal del manejador.
     * Lee mensajes JSON línea por línea, los procesa y envía respuestas.
     */
    @Override
    public void run() {
        try {
            // Inicializar streams
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            System.out.println("[SERVER] Cliente conectado: " + clientAddress);

            String inputLine;
            while (running && (inputLine = in.readLine()) != null) {
                try {
                    // Parsear mensaje JSON
                    Message request = Message.fromJson(inputLine);
                    System.out.println("[SERVER] Recibido de " + clientAddress + ": " + request.getAction());

                    // Procesar mensaje
                    Message response;
                    if (protocolHandler != null) {
                        response = protocolHandler.handleMessage(request, this);
                    } else {
                        // Modo eco: devolver el mismo mensaje como respuesta
                        response = Message.createSuccessResponse(
                            request.getAction(),
                            "Echo: " + request.getAction()
                        );
                        response.setData(request.getData());
                    }

                    // Enviar respuesta
                    sendMessage(response);

                } catch (com.google.gson.JsonSyntaxException e) {
                    // JSON inválido
                    System.err.println("[SERVER] JSON inválido de " + clientAddress + ": " + e.getMessage());
                    Message errorResponse = Message.createErrorResponse("UNKNOWN", "JSON inválido: " + e.getMessage());
                    sendMessage(errorResponse);
                } catch (Exception e) {
                    // Error inesperado procesando mensaje
                    System.err.println("[SERVER] Error procesando mensaje de " + clientAddress + ": " + e.getMessage());
                    e.printStackTrace();
                    Message errorResponse = Message.createErrorResponse("UNKNOWN", "Error interno del servidor");
                    sendMessage(errorResponse);
                }
            }

        } catch (IOException e) {
            // El cliente se desconectó (posiblemente de forma abrupta)
            System.out.println("[SERVER] Cliente desconectado: " + clientAddress + " (" + e.getMessage() + ")");
        } finally {
            cleanup();
        }
    }

    /**
     * Envía un mensaje al cliente de forma sincronizada.
     *
     * @param message mensaje a enviar
     */
    public synchronized void sendMessage(Message message) {
        if (out != null && !clientSocket.isClosed()) {
            out.println(message.toJson());
            System.out.println("[SERVER] Enviado a " + clientAddress + ": " + message.getAction());
        }
    }

    /**
     * Detiene el manejador y cierra la conexión.
     */
    public void stop() {
        running = false;
        cleanup();
    }

    /**
     * Limpia recursos: cierra streams y socket.
     */
    private void cleanup() {
        running = false;

        // Notificar al servicio de notificaciones si hay usuario autenticado
        // (se implementará en fases posteriores)

        if (protocolHandler != null) {
            protocolHandler.handleClientDisconnect(this);
        }

        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            // Ignorar
        }

        if (out != null) {
            out.close();
        }

        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            // Ignorar
        }

        System.out.println("[SERVER] Recursos liberados para: " + clientAddress);
    }
}
