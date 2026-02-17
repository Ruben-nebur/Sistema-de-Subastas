package server;

import common.Constants;
import common.Message;
import server.manager.UserManager;
import server.manager.SessionManager;
import server.manager.AuctionManager;
import server.model.User;
import server.model.Session;
import server.model.Auction;
import server.model.Bid;
import server.service.NotificationService;
import server.util.AuditLogger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Procesador de protocolo que enruta los mensajes a los managers correspondientes.
 * Maneja la lógica de todas las acciones del protocolo NetAuction.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class ProtocolHandler {

    /** Gestor de usuarios */
    private final UserManager userManager;

    /** Gestor de sesiones */
    private final SessionManager sessionManager;

    /** Gestor de subastas */
    private final AuctionManager auctionManager;

    /** Servicio de notificaciones */
    private NotificationService notificationService;

    /** Logger de auditoría */
    private AuditLogger auditLogger;

    /**
     * Constructor del procesador de protocolo.
     *
     * @param userManager gestor de usuarios
     * @param sessionManager gestor de sesiones
     * @param auctionManager gestor de subastas
     */
    public ProtocolHandler(UserManager userManager, SessionManager sessionManager,
                           AuctionManager auctionManager) {
        this.userManager = userManager;
        this.sessionManager = sessionManager;
        this.auctionManager = auctionManager;
    }

    /**
     * Establece el servicio de notificaciones.
     *
     * @param notificationService servicio de notificaciones
     */
    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Establece el logger de auditoría.
     *
     * @param auditLogger logger de auditoría
     */
    public void setAuditLogger(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    /**
     * Procesa un mensaje del cliente y devuelve la respuesta apropiada.
     *
     * @param request mensaje de solicitud del cliente
     * @param clientHandler manejador del cliente que envía el mensaje
     * @return mensaje de respuesta
     */
    public Message handleMessage(Message request, ClientHandler clientHandler) {
        String action = request.getAction();

        if (action == null || action.isEmpty()) {
            return Message.createErrorResponse("UNKNOWN", "Acción no especificada");
        }

        // Log de la acción
        log(action, clientHandler.getAuthenticatedUser(), clientHandler.getClientAddress(), "Solicitud recibida");

        // Enrutar según la acción
        switch (action) {
            case Constants.ACTION_REGISTER:
                return handleRegister(request, clientHandler);

            case Constants.ACTION_LOGIN:
                return handleLogin(request, clientHandler);

            case Constants.ACTION_LOGOUT:
                return handleLogout(request, clientHandler);

            case Constants.ACTION_CREATE_AUCTION:
                return handleCreateAuction(request, clientHandler);

            case Constants.ACTION_LIST_AUCTIONS:
                return handleListAuctions(request, clientHandler);

            case Constants.ACTION_AUCTION_DETAIL:
                return handleAuctionDetail(request, clientHandler);

            case Constants.ACTION_BID:
                return handleBid(request, clientHandler);

            case Constants.ACTION_MY_HISTORY:
                return handleMyHistory(request, clientHandler);

            case Constants.ACTION_CANCEL_AUCTION:
                return handleCancelAuction(request, clientHandler);

            case Constants.ACTION_BLOCK_USER:
                return handleBlockUser(request, clientHandler);

            case Constants.ACTION_VIEW_LOGS:
                return handleViewLogs(request, clientHandler);

            default:
                return Message.createErrorResponse(action, "Acción desconocida: " + action);
        }
    }

    /**
     * Valida el token de sesión y devuelve la sesión si es válida.
     *
     * @param request mensaje con el token
     * @return sesión válida o null
     */
    private Session validateToken(Message request) {
        String token = request.getToken();
        if (token == null || token.isEmpty()) {
            return null;
        }
        return sessionManager.validateSession(token);
    }

    /**
     * Log de auditoría.
     */
    private void log(String action, String user, String ip, String details) {
        if (auditLogger != null) {
            auditLogger.log(action, user, ip, details);
        }
    }

    // ==================== HANDLERS ====================

    /**
     * Maneja el registro de usuario.
     */
    private Message handleRegister(Message request, ClientHandler clientHandler) {
        String username = request.getDataString("user");
        String password = request.getDataString("password");
        String email = request.getDataString("email");

        UserManager.RegistrationResult result = userManager.register(username, password, email);

        if (result.isSuccess()) {
            log(Constants.ACTION_REGISTER, username, clientHandler.getClientAddress(), "Registro exitoso");
            return Message.createSuccessResponse(Constants.ACTION_REGISTER, result.getMessage());
        } else {
            log(Constants.ACTION_REGISTER, username, clientHandler.getClientAddress(), "Registro fallido: " + result.getMessage());
            return Message.createErrorResponse(Constants.ACTION_REGISTER, result.getMessage());
        }
    }

    /**
     * Maneja el inicio de sesión.
     */
    private Message handleLogin(Message request, ClientHandler clientHandler) {
        String username = request.getDataString("user");
        String password = request.getDataString("password");

        UserManager.AuthenticationResult result = userManager.authenticate(username, password);

        if (result.isSuccess()) {
            User user = result.getUser();
            Session session = sessionManager.createSession(user.getUsername(), user.getRole());

            // Registrar en servicio de notificaciones
            if (notificationService != null) {
                notificationService.registerClient(user.getUsername(), clientHandler.getOut());
            }

            // Establecer usuario autenticado en el handler
            clientHandler.setAuthenticatedUser(user.getUsername());

            log(Constants.ACTION_LOGIN, username, clientHandler.getClientAddress(), "Login exitoso");

            Message response = Message.createSuccessResponse(Constants.ACTION_LOGIN, "Bienvenido, " + username);
            response.addData("token", session.getToken());
            response.addData("role", user.getRole());
            response.addData("username", user.getUsername());
            return response;
        } else {
            log(Constants.ACTION_LOGIN, username, clientHandler.getClientAddress(), "Login fallido: " + result.getMessage());
            return Message.createErrorResponse(Constants.ACTION_LOGIN, result.getMessage());
        }
    }

    /**
     * Maneja el cierre de sesión.
     */
    private Message handleLogout(Message request, ClientHandler clientHandler) {
        Session session = validateToken(request);
        if (session == null) {
            return Message.createErrorResponse(Constants.ACTION_LOGOUT, "Sesión no válida");
        }

        String username = session.getUsername();

        // Invalidar sesión
        sessionManager.invalidateSession(session.getToken());

        // Desregistrar de notificaciones
        if (notificationService != null) {
            notificationService.unregisterClient(username);
        }

        clientHandler.setAuthenticatedUser(null);

        log(Constants.ACTION_LOGOUT, username, clientHandler.getClientAddress(), "Logout exitoso");
        return Message.createSuccessResponse(Constants.ACTION_LOGOUT, "Sesión cerrada correctamente");
    }

    /**
     * Maneja la creación de subasta.
     */
    private Message handleCreateAuction(Message request, ClientHandler clientHandler) {
        Session session = validateToken(request);
        if (session == null) {
            return Message.createErrorResponse(Constants.ACTION_CREATE_AUCTION, "Sesión no válida o expirada");
        }

        String title = request.getDataString("title");
        String description = request.getDataString("description");
        double startPrice = request.getDataDouble("startPrice", 0.0);
        int durationMinutes = request.getDataInt("durationMinutes", 5);

        AuctionManager.CreateAuctionResult result = auctionManager.createAuction(
            title, description, session.getUsername(), startPrice, durationMinutes
        );

        if (result.isSuccess()) {
            Auction auction = result.getAuction();
            log(Constants.ACTION_CREATE_AUCTION, session.getUsername(), clientHandler.getClientAddress(),
                "Subasta creada: " + auction.getId());

            Message response = Message.createSuccessResponse(Constants.ACTION_CREATE_AUCTION, result.getMessage());
            response.addData("auctionId", auction.getId());
            response.addData("endTime", auction.getEndTime());
            return response;
        } else {
            return Message.createErrorResponse(Constants.ACTION_CREATE_AUCTION, result.getMessage());
        }
    }

    /**
     * Maneja el listado de subastas activas.
     */
    private Message handleListAuctions(Message request, ClientHandler clientHandler) {
        Session session = validateToken(request);
        if (session == null) {
            return Message.createErrorResponse(Constants.ACTION_LIST_AUCTIONS, "Sesión no válida o expirada");
        }

        List<Auction> auctions = auctionManager.getActiveAuctions();

        JsonArray auctionsArray = new JsonArray();
        for (Auction auction : auctions) {
            JsonObject auctionJson = new JsonObject();
            auctionJson.addProperty("id", auction.getId());
            auctionJson.addProperty("title", auction.getTitle());
            auctionJson.addProperty("currentPrice", auction.getCurrentPrice());
            auctionJson.addProperty("remainingTime", auction.getRemainingTimeFormatted());
            auctionJson.addProperty("remainingSeconds", auction.getRemainingSeconds());
            auctionJson.addProperty("bidCount", auction.getBidCount());
            auctionJson.addProperty("seller", auction.getSeller());
            auctionsArray.add(auctionJson);
        }

        Message response = Message.createSuccessResponse(Constants.ACTION_LIST_AUCTIONS,
            "Se encontraron " + auctions.size() + " subastas activas");
        response.getData().add("auctions", auctionsArray);
        response.addData("count", auctions.size());
        return response;
    }

    /**
     * Maneja la obtención de detalle de subasta.
     */
    private Message handleAuctionDetail(Message request, ClientHandler clientHandler) {
        Session session = validateToken(request);
        if (session == null) {
            return Message.createErrorResponse(Constants.ACTION_AUCTION_DETAIL, "Sesión no válida o expirada");
        }

        String auctionId = request.getDataString("auctionId");
        if (auctionId == null || auctionId.isEmpty()) {
            return Message.createErrorResponse(Constants.ACTION_AUCTION_DETAIL, "ID de subasta requerido");
        }

        Auction auction = auctionManager.getAuction(auctionId);
        if (auction == null) {
            return Message.createErrorResponse(Constants.ACTION_AUCTION_DETAIL, "Subasta no encontrada");
        }

        Message response = Message.createSuccessResponse(Constants.ACTION_AUCTION_DETAIL, "Detalle de subasta");
        response.addData("id", auction.getId());
        response.addData("title", auction.getTitle());
        response.addData("description", auction.getDescription());
        response.addData("seller", auction.getSeller());
        response.addData("startPrice", auction.getStartPrice());
        response.addData("currentPrice", auction.getCurrentPrice());
        response.addData("currentWinner", auction.getCurrentWinner());
        response.addData("startTime", auction.getStartTime());
        response.addData("endTime", auction.getEndTime());
        response.addData("remainingTime", auction.getRemainingTimeFormatted());
        response.addData("remainingSeconds", auction.getRemainingSeconds());
        response.addData("status", auction.getStatus());
        response.addData("bidCount", auction.getBidCount());

        // Historial de pujas
        JsonArray bidsArray = new JsonArray();
        List<Bid> bids = auction.getBids();
        for (int i = bids.size() - 1; i >= 0 && i >= bids.size() - 10; i--) {
            Bid bid = bids.get(i);
            JsonObject bidJson = new JsonObject();
            bidJson.addProperty("bidder", bid.getBidder());
            bidJson.addProperty("amount", bid.getAmount());
            bidJson.addProperty("timestamp", bid.getTimestamp());
            bidsArray.add(bidJson);
        }
        response.getData().add("recentBids", bidsArray);

        return response;
    }

    /**
     * Maneja una puja.
     */
    private Message handleBid(Message request, ClientHandler clientHandler) {
        Session session = validateToken(request);
        if (session == null) {
            return Message.createErrorResponse(Constants.ACTION_BID, "Sesión no válida o expirada");
        }

        String auctionId = request.getDataString("auctionId");
        double amount = request.getDataDouble("amount", 0.0);

        if (auctionId == null || auctionId.isEmpty()) {
            return Message.createErrorResponse(Constants.ACTION_BID, "ID de subasta requerido");
        }

        AuctionManager.BidResult result = auctionManager.placeBid(auctionId, session.getUsername(), amount);

        if (result.isSuccess()) {
            Auction auction = result.getAuction();

            log(Constants.ACTION_BID, session.getUsername(), clientHandler.getClientAddress(),
                "Puja de " + amount + " en " + auctionId);

            // Enviar notificaciones
            if (notificationService != null) {
                // NEW_BID a todos los conectados
                notificationService.notifyNewBid(auctionId, auction.getTitle(), amount, session.getUsername());

                // OUTBID al pujador anterior
                if (result.getPreviousBidder() != null) {
                    notificationService.notifyOutbid(result.getPreviousBidder(), auctionId,
                        auction.getTitle(), amount, session.getUsername());
                }
            }

            Message response = Message.createSuccessResponse(Constants.ACTION_BID, result.getMessage());
            response.addData("auctionId", auctionId);
            response.addData("amount", amount);
            response.addData("newPrice", auction.getCurrentPrice());
            return response;
        } else {
            return Message.createErrorResponse(Constants.ACTION_BID, result.getMessage());
        }
    }

    /**
     * Maneja la obtención del historial del usuario.
     */
    private Message handleMyHistory(Message request, ClientHandler clientHandler) {
        Session session = validateToken(request);
        if (session == null) {
            return Message.createErrorResponse(Constants.ACTION_MY_HISTORY, "Sesión no válida o expirada");
        }

        String username = session.getUsername();

        // Subastas creadas
        List<Auction> myAuctions = auctionManager.getAuctionsBySeller(username);
        JsonArray myAuctionsArray = new JsonArray();
        for (Auction a : myAuctions) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", a.getId());
            obj.addProperty("title", a.getTitle());
            obj.addProperty("currentPrice", a.getCurrentPrice());
            obj.addProperty("status", a.getStatus());
            obj.addProperty("bidCount", a.getBidCount());
            myAuctionsArray.add(obj);
        }

        // Subastas donde ha pujado
        List<Auction> biddedAuctions = auctionManager.getAuctionsByBidder(username);
        JsonArray biddedArray = new JsonArray();
        for (Auction a : biddedAuctions) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", a.getId());
            obj.addProperty("title", a.getTitle());
            obj.addProperty("currentPrice", a.getCurrentPrice());
            obj.addProperty("status", a.getStatus());
            obj.addProperty("isWinning", username.equals(a.getCurrentWinner()));
            biddedArray.add(obj);
        }

        // Subastas ganadas
        List<Auction> wonAuctions = auctionManager.getAuctionsWonBy(username);
        JsonArray wonArray = new JsonArray();
        for (Auction a : wonAuctions) {
            JsonObject obj = new JsonObject();
            obj.addProperty("id", a.getId());
            obj.addProperty("title", a.getTitle());
            obj.addProperty("finalPrice", a.getCurrentPrice());
            obj.addProperty("seller", a.getSeller());
            wonArray.add(obj);
        }

        Message response = Message.createSuccessResponse(Constants.ACTION_MY_HISTORY, "Historial del usuario");
        response.getData().add("myAuctions", myAuctionsArray);
        response.getData().add("biddedAuctions", biddedArray);
        response.getData().add("wonAuctions", wonArray);
        response.addData("myAuctionsCount", myAuctions.size());
        response.addData("biddedCount", biddedAuctions.size());
        response.addData("wonCount", wonAuctions.size());

        return response;
    }

    /**
     * Maneja la cancelación de subasta (ADMIN).
     */
    private Message handleCancelAuction(Message request, ClientHandler clientHandler) {
        Session session = validateToken(request);
        if (session == null) {
            return Message.createErrorResponse(Constants.ACTION_CANCEL_AUCTION, "Sesión no válida o expirada");
        }

        if (!session.isAdmin()) {
            return Message.createErrorResponse(Constants.ACTION_CANCEL_AUCTION, "Permiso denegado. Se requiere rol ADMIN");
        }

        String auctionId = request.getDataString("auctionId");
        if (auctionId == null || auctionId.isEmpty()) {
            return Message.createErrorResponse(Constants.ACTION_CANCEL_AUCTION, "ID de subasta requerido");
        }

        AuctionManager.CancelResult result = auctionManager.cancelAuction(auctionId);

        if (result.isSuccess()) {
            log(Constants.ACTION_CANCEL_AUCTION, session.getUsername(), clientHandler.getClientAddress(),
                "Subasta cancelada: " + auctionId);
            return Message.createSuccessResponse(Constants.ACTION_CANCEL_AUCTION, result.getMessage());
        } else {
            return Message.createErrorResponse(Constants.ACTION_CANCEL_AUCTION, result.getMessage());
        }
    }

    /**
     * Maneja el bloqueo/desbloqueo de usuario (ADMIN).
     */
    private Message handleBlockUser(Message request, ClientHandler clientHandler) {
        Session session = validateToken(request);
        if (session == null) {
            return Message.createErrorResponse(Constants.ACTION_BLOCK_USER, "Sesión no válida o expirada");
        }

        if (!session.isAdmin()) {
            return Message.createErrorResponse(Constants.ACTION_BLOCK_USER, "Permiso denegado. Se requiere rol ADMIN");
        }

        String username = request.getDataString("username");
        boolean blocked = request.getDataBoolean("blocked", true);

        if (username == null || username.isEmpty()) {
            return Message.createErrorResponse(Constants.ACTION_BLOCK_USER, "Username requerido");
        }

        // No permitir bloquearse a sí mismo
        if (username.equalsIgnoreCase(session.getUsername())) {
            return Message.createErrorResponse(Constants.ACTION_BLOCK_USER, "No puedes bloquearte a ti mismo");
        }

        boolean success = userManager.setUserBlocked(username, blocked);

        if (success) {
            // Si se bloquea, invalidar su sesión
            if (blocked) {
                sessionManager.invalidateUserSession(username);
                if (notificationService != null) {
                    notificationService.unregisterClient(username);
                }
            }

            log(Constants.ACTION_BLOCK_USER, session.getUsername(), clientHandler.getClientAddress(),
                "Usuario " + username + (blocked ? " bloqueado" : " desbloqueado"));

            return Message.createSuccessResponse(Constants.ACTION_BLOCK_USER,
                "Usuario " + username + (blocked ? " bloqueado" : " desbloqueado") + " correctamente");
        } else {
            return Message.createErrorResponse(Constants.ACTION_BLOCK_USER, "Usuario no encontrado");
        }
    }

    /**
     * Maneja la visualización de logs (ADMIN).
     */
    private Message handleViewLogs(Message request, ClientHandler clientHandler) {
        Session session = validateToken(request);
        if (session == null) {
            return Message.createErrorResponse(Constants.ACTION_VIEW_LOGS, "Sesión no válida o expirada");
        }

        if (!session.isAdmin()) {
            return Message.createErrorResponse(Constants.ACTION_VIEW_LOGS, "Permiso denegado. Se requiere rol ADMIN");
        }

        Message response = Message.createSuccessResponse(Constants.ACTION_VIEW_LOGS, "Logs de auditoría");

        if (auditLogger != null) {
            List<String> logs = auditLogger.getRecentLogs(100);
            JsonArray logsArray = new JsonArray();
            for (String logEntry : logs) {
                logsArray.add(logEntry);
            }
            response.getData().add("logs", logsArray);
            response.addData("count", logs.size());
        } else {
            response.addData("count", 0);
        }

        return response;
    }
}
