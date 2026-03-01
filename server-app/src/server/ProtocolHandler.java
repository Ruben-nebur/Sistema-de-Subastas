package server;

import common.Constants;
import common.Message;
import server.manager.AuctionManager;
import server.manager.SessionManager;
import server.manager.UserManager;
import server.model.Auction;
import server.model.Bid;
import server.model.Session;
import server.model.User;
import server.service.NotificationService;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Procesador de protocolo simplificado.
 */
public class ProtocolHandler {

    private final UserManager userManager;
    private final SessionManager sessionManager;
    private final AuctionManager auctionManager;
    private NotificationService notificationService;

    public ProtocolHandler(UserManager userManager, SessionManager sessionManager,
                           AuctionManager auctionManager) {
        this.userManager = userManager;
        this.sessionManager = sessionManager;
        this.auctionManager = auctionManager;
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public Message handleMessage(Message request, ClientHandler clientHandler) {
        String action = request.getAction();

        if (action == null || action.isEmpty()) {
            return Message.createErrorResponse("UNKNOWN", "Accion no especificada");
        }

        switch (action) {
            case Constants.ACTION_REGISTER:
                return handleRegister(request);
            case Constants.ACTION_LOGIN:
                return handleLogin(request, clientHandler);
            case Constants.ACTION_LOGOUT:
                return handleLogout(request, clientHandler);
            case Constants.ACTION_CREATE_AUCTION:
                return handleCreateAuction(request);
            case Constants.ACTION_LIST_AUCTIONS:
                return handleListAuctions(request);
            case Constants.ACTION_AUCTION_DETAIL:
                return handleAuctionDetail(request);
            case Constants.ACTION_BID:
                return handleBid(request, clientHandler);
            case Constants.ACTION_CANCEL_AUCTION:
                return handleCancelAuction(request);
            case Constants.ACTION_BLOCK_USER:
                return handleBlockUser(request);
            default:
                return Message.createErrorResponse(action, "Accion desconocida: " + action);
        }
    }

    public void handleClientDisconnect(ClientHandler clientHandler) {
        if (clientHandler == null) {
            return;
        }

        String username = clientHandler.getAuthenticatedUser();
        if (username == null || username.isEmpty()) {
            return;
        }

        sessionManager.invalidateUserSession(username);
        if (notificationService != null) {
            notificationService.unregisterClient(username);
        }
        clientHandler.setAuthenticatedUser(null);
    }

    private Session validateToken(Message request) {
        String token = request.getToken();
        if (token == null || token.isEmpty()) {
            return null;
        }
        return sessionManager.validateSession(token);
    }

    private Message handleRegister(Message request) {
        String username = request.getDataString("user");
        String password = request.getDataString("password");
        String email = request.getDataString("email");

        UserManager.RegistrationResult result = userManager.register(username, password, email);

        if (result.isSuccess()) {
            return Message.createSuccessResponse(Constants.ACTION_REGISTER, result.getMessage());
        }
        return Message.createErrorResponse(Constants.ACTION_REGISTER, result.getMessage());
    }

    private Message handleLogin(Message request, ClientHandler clientHandler) {
        String username = request.getDataString("user");
        String password = request.getDataString("password");

        UserManager.AuthenticationResult result = userManager.authenticate(username, password);

        if (result.isSuccess()) {
            User user = result.getUser();
            Session session = sessionManager.createSession(user.getUsername());

            if (notificationService != null) {
                notificationService.registerClient(user.getUsername(), clientHandler.getOut());
            }

            clientHandler.setAuthenticatedUser(user.getUsername());

            Message response = Message.createSuccessResponse(Constants.ACTION_LOGIN, "Bienvenido, " + username);
            response.addData("token", session.getToken());
            response.addData("username", user.getUsername());
            return response;
        }

        return Message.createErrorResponse(Constants.ACTION_LOGIN, result.getMessage());
    }

    private Message handleLogout(Message request, ClientHandler clientHandler) {
        Session session = validateToken(request);
        if (session == null) {
            return Message.createErrorResponse(Constants.ACTION_LOGOUT, "Sesion no valida");
        }

        String username = session.getUsername();

        sessionManager.invalidateSession(session.getToken());

        if (notificationService != null) {
            notificationService.unregisterClient(username);
        }

        clientHandler.setAuthenticatedUser(null);

        return Message.createSuccessResponse(Constants.ACTION_LOGOUT, "Sesion cerrada correctamente");
    }

    private Message handleCreateAuction(Message request) {
        Session session = validateToken(request);
        if (session == null) {
            return Message.createErrorResponse(Constants.ACTION_CREATE_AUCTION, "Sesion no valida");
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
            Message response = Message.createSuccessResponse(Constants.ACTION_CREATE_AUCTION, result.getMessage());
            response.addData("auctionId", auction.getId());
            response.addData("endTime", auction.getEndTime());
            return response;
        }
        return Message.createErrorResponse(Constants.ACTION_CREATE_AUCTION, result.getMessage());
    }

    private Message handleListAuctions(Message request) {
        Session session = validateToken(request);
        if (session == null) {
            return Message.createErrorResponse(Constants.ACTION_LIST_AUCTIONS, "Sesion no valida");
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

    private Message handleAuctionDetail(Message request) {
        Session session = validateToken(request);
        if (session == null) {
            return Message.createErrorResponse(Constants.ACTION_AUCTION_DETAIL, "Sesion no valida");
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

    private Message handleBid(Message request, ClientHandler clientHandler) {
        Session session = validateToken(request);
        if (session == null) {
            return Message.createErrorResponse(Constants.ACTION_BID, "Sesion no valida");
        }

        String auctionId = request.getDataString("auctionId");
        double amount = request.getDataDouble("amount", 0.0);

        if (auctionId == null || auctionId.isEmpty()) {
            return Message.createErrorResponse(Constants.ACTION_BID, "ID de subasta requerido");
        }

        AuctionManager.BidResult result = auctionManager.placeBid(auctionId, session.getUsername(), amount);

        if (result.isSuccess()) {
            Auction auction = result.getAuction();

            if (notificationService != null) {
                notificationService.notifyNewBid(auctionId, auction.getTitle(), amount, session.getUsername());

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
        }

        return Message.createErrorResponse(Constants.ACTION_BID, result.getMessage());
    }

    private Message handleCancelAuction(Message request) {
        Session session = validateToken(request);
        if (session == null) {
            return Message.createErrorResponse(Constants.ACTION_CANCEL_AUCTION, "Sesion no valida");
        }

        String auctionId = request.getDataString("auctionId");
        if (auctionId == null || auctionId.isEmpty()) {
            return Message.createErrorResponse(Constants.ACTION_CANCEL_AUCTION, "ID de subasta requerido");
        }

        AuctionManager.CancelResult result = auctionManager.cancelAuction(auctionId);

        if (result.isSuccess()) {
            return Message.createSuccessResponse(Constants.ACTION_CANCEL_AUCTION, result.getMessage());
        }
        return Message.createErrorResponse(Constants.ACTION_CANCEL_AUCTION, result.getMessage());
    }

    private Message handleBlockUser(Message request) {
        Session session = validateToken(request);
        if (session == null) {
            return Message.createErrorResponse(Constants.ACTION_BLOCK_USER, "Sesion no valida");
        }

        String username = request.getDataString("username");
        boolean blocked = request.getDataBoolean("blocked", true);

        if (username == null || username.isEmpty()) {
            return Message.createErrorResponse(Constants.ACTION_BLOCK_USER, "Username requerido");
        }

        if (username.equalsIgnoreCase(session.getUsername())) {
            return Message.createErrorResponse(Constants.ACTION_BLOCK_USER, "No puedes bloquearte a ti mismo");
        }

        boolean success = userManager.setUserBlocked(username, blocked);

        if (success) {
            if (blocked) {
                sessionManager.invalidateUserSession(username);
                if (notificationService != null) {
                    notificationService.unregisterClient(username);
                }
            }

            return Message.createSuccessResponse(Constants.ACTION_BLOCK_USER,
                "Usuario " + username + (blocked ? " bloqueado" : " desbloqueado") + " correctamente");
        }

        return Message.createErrorResponse(Constants.ACTION_BLOCK_USER, "Usuario no encontrado");
    }
}

