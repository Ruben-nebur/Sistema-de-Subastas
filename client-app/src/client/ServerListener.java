package client;

import common.Constants;
import common.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Hilo que escucha mensajes del servidor de forma asíncrona.
 * Distingue entre respuestas y notificaciones push.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class ServerListener implements Runnable {

    /** Reader para leer del servidor */
    private final BufferedReader reader;

    /** Cola para respuestas (esperadas por el cliente) */
    private final BlockingQueue<Message> responseQueue;

    /** Callback para notificaciones push */
    private Consumer<Message> notificationCallback;

    /** Indica si el listener está activo */
    private volatile boolean running;

    /**
     * Constructor del listener.
     *
     * @param reader BufferedReader del socket
     */
    public ServerListener(BufferedReader reader) {
        this.reader = reader;
        this.responseQueue = new LinkedBlockingQueue<>();
        this.running = true;
    }

    /**
     * Establece el callback para notificaciones.
     *
     * @param callback función a ejecutar cuando llegue una notificación
     */
    public void setNotificationCallback(Consumer<Message> callback) {
        this.notificationCallback = callback;
    }

    /**
     * Obtiene la siguiente respuesta de la cola (bloqueante).
     *
     * @return mensaje de respuesta o null si se interrumpe
     */
    public Message getNextResponse() {
        try {
            return responseQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Obtiene la siguiente respuesta con timeout.
     *
     * @param timeoutMs tiempo máximo de espera en milisegundos
     * @return mensaje de respuesta o null si expira el timeout
     */
    public Message getNextResponse(long timeoutMs) {
        try {
            return responseQueue.poll(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Elimina respuestas pendientes para evitar reutilizar mensajes antiguos.
     */
    public void clearResponses() {
        responseQueue.clear();
    }

    /**
     * Detiene el listener.
     */
    public void stop() {
        running = false;
    }

    /**
     * Verifica si el listener está activo.
     *
     * @return true si está ejecutándose
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Bucle principal de escucha.
     */
    @Override
    public void run() {
        try {
            String line;
            while (running && (line = reader.readLine()) != null) {
                try {
                    Message message = Message.fromJson(line);
                    processMessage(message);
                } catch (Exception e) {
                    System.err.println("[LISTENER] Error parseando mensaje: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[LISTENER] Conexión perdida: " + e.getMessage());
            }
        } finally {
            running = false;
        }
    }

    /**
     * Procesa un mensaje recibido, distinguiendo entre respuesta y notificación.
     *
     * @param message mensaje recibido
     */
    private void processMessage(Message message) {
        if (message.isNotification()) {
            // Es una notificación push
            handleNotification(message);
        } else {
            // Es una respuesta a una acción
            responseQueue.offer(message);
        }
    }

    /**
     * Maneja una notificación push.
     *
     * @param notification mensaje de notificación
     */
    private void handleNotification(Message notification) {
        String action = notification.getAction();

        // Imprimir notificación en consola
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        System.out.println("║                    NOTIFICACIÓN                          ║");
        System.out.println("╠══════════════════════════════════════════════════════════╣");

        switch (action) {
            case Constants.NOTIFY_NEW_BID:
                String bidder = notification.getDataString("bidder");
                double amount = notification.getDataDouble("amount", 0);
                String auctionTitle = notification.getDataString("auctionTitle");
                System.out.println("║  Nueva puja en: " + auctionTitle);
                System.out.println("║  Pujador: " + bidder + " - Cantidad: " + amount);
                break;

            case Constants.NOTIFY_OUTBID:
                String newBidder = notification.getDataString("newBidder");
                double newAmount = notification.getDataDouble("newAmount", 0);
                String title = notification.getDataString("auctionTitle");
                System.out.println("║  ¡Has sido superado en: " + title + "!");
                System.out.println("║  Nuevo líder: " + newBidder + " con " + newAmount);
                break;

            case Constants.NOTIFY_AUCTION_CLOSED:
                String closedTitle = notification.getDataString("auctionTitle");
                String winner = notification.getDataString("winner");
                double finalPrice = notification.getDataDouble("finalPrice", 0);
                boolean isDesierta = notification.getDataBoolean("isDesierta", false);
                System.out.println("║  Subasta finalizada: " + closedTitle);
                if (isDesierta) {
                    System.out.println("║  La subasta quedó desierta (sin pujas)");
                } else {
                    System.out.println("║  Ganador: " + winner + " - Precio: " + finalPrice);
                }
                break;

            default:
                System.out.println("║  Tipo: " + action);
        }

        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.print("> ");

        // Ejecutar callback si existe
        if (notificationCallback != null) {
            try {
                notificationCallback.accept(notification);
            } catch (Exception e) {
                System.err.println("[LISTENER] Error en callback: " + e.getMessage());
            }
        }
    }
}
