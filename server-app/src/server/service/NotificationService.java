package server.service;

import common.Constants;
import common.Message;

import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de notificaciones push en tiempo real.
 * Mantiene un registro de clientes conectados y permite enviar notificaciones.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class NotificationService {

    /** Mapa de clientes conectados (username -> PrintWriter) */
    private final ConcurrentHashMap<String, PrintWriter> clients;

    /**
     * Constructor del servicio de notificaciones.
     */
    public NotificationService() {
        this.clients = new ConcurrentHashMap<>();
        System.out.println("[NotificationService] Iniciado");
    }

    /**
     * Registra un cliente para recibir notificaciones.
     *
     * @param username nombre de usuario
     * @param writer PrintWriter del cliente
     */
    public void registerClient(String username, PrintWriter writer) {
        if (username != null && writer != null) {
            clients.put(username, writer);
            System.out.println("[NotificationService] Cliente registrado: " + username);
        }
    }

    /**
     * Desregistra un cliente.
     *
     * @param username nombre de usuario
     */
    public void unregisterClient(String username) {
        if (username != null) {
            PrintWriter removed = clients.remove(username);
            if (removed != null) {
                System.out.println("[NotificationService] Cliente desregistrado: " + username);
            }
        }
    }

    /**
     * Verifica si un cliente está registrado.
     *
     * @param username nombre de usuario
     * @return true si está registrado
     */
    public boolean isClientRegistered(String username) {
        return username != null && clients.containsKey(username);
    }

    /**
     * Obtiene el número de clientes conectados.
     *
     * @return cantidad de clientes
     */
    public int getConnectedClientCount() {
        return clients.size();
    }

    /**
     * Envía un mensaje a un cliente específico.
     *
     * @param username nombre de usuario
     * @param message mensaje a enviar
     */
    public void sendToClient(String username, Message message) {
        PrintWriter writer = clients.get(username);
        if (writer != null) {
            synchronized (writer) {
                try {
                    writer.println(message.toJson());
                    System.out.println("[NotificationService] Notificación enviada a " + username + ": " + message.getAction());
                } catch (Exception e) {
                    System.err.println("[NotificationService] Error enviando a " + username + ": " + e.getMessage());
                    // No eliminar el cliente aquí, lo hará el ClientHandler cuando detecte la desconexión
                }
            }
        }
    }

    /**
     * Envía un mensaje a todos los clientes conectados.
     *
     * @param message mensaje a enviar
     */
    public void broadcast(Message message) {
        for (String username : clients.keySet()) {
            sendToClient(username, message);
        }
    }

    /**
     * Envía un mensaje a todos los clientes excepto al especificado.
     *
     * @param message mensaje a enviar
     * @param excludeUsername usuario a excluir
     */
    public void broadcastExcept(Message message, String excludeUsername) {
        for (String username : clients.keySet()) {
            if (!username.equals(excludeUsername)) {
                sendToClient(username, message);
            }
        }
    }

    // ==================== NOTIFICACIONES ESPECÍFICAS ====================

    /**
     * Notifica una nueva puja a todos los clientes.
     *
     * @param auctionId ID de la subasta
     * @param auctionTitle título de la subasta
     * @param amount cantidad de la puja
     * @param bidder nombre del pujador
     */
    public void notifyNewBid(String auctionId, String auctionTitle, double amount, String bidder) {
        Message notification = Message.createNotification(Constants.NOTIFY_NEW_BID);
        notification.addData("auctionId", auctionId);
        notification.addData("auctionTitle", auctionTitle);
        notification.addData("amount", amount);
        notification.addData("bidder", bidder);

        broadcast(notification);
    }

    /**
     * Notifica a un usuario que ha sido superado en una puja.
     *
     * @param previousBidder usuario que fue superado
     * @param auctionId ID de la subasta
     * @param auctionTitle título de la subasta
     * @param newAmount nueva cantidad de la puja
     * @param newBidder nuevo pujador
     */
    public void notifyOutbid(String previousBidder, String auctionId, String auctionTitle,
                            double newAmount, String newBidder) {
        Message notification = Message.createNotification(Constants.NOTIFY_OUTBID);
        notification.addData("auctionId", auctionId);
        notification.addData("auctionTitle", auctionTitle);
        notification.addData("newAmount", newAmount);
        notification.addData("newBidder", newBidder);

        sendToClient(previousBidder, notification);
    }

    /**
     * Notifica el cierre de una subasta.
     *
     * @param auctionId ID de la subasta
     * @param auctionTitle título de la subasta
     * @param winner ganador (null si quedó desierta)
     * @param finalPrice precio final
     * @param seller vendedor de la subasta
     */
    public void notifyAuctionClosed(String auctionId, String auctionTitle, String winner,
                                     double finalPrice, String seller) {
        boolean isDesierta = winner == null || winner.isEmpty();

        Message notification = Message.createNotification(Constants.NOTIFY_AUCTION_CLOSED);
        notification.addData("auctionId", auctionId);
        notification.addData("auctionTitle", auctionTitle);
        notification.addData("winner", winner != null ? winner : "");
        notification.addData("finalPrice", finalPrice);
        notification.addData("isDesierta", isDesierta);

        // Notificar al vendedor
        sendToClient(seller, notification);

        // Notificar al ganador si existe
        if (!isDesierta) {
            sendToClient(winner, notification);
        }
    }

    /**
     * Limpia todos los clientes registrados.
     */
    public void clear() {
        clients.clear();
        System.out.println("[NotificationService] Todos los clientes desregistrados");
    }
}
