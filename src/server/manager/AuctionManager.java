package server.manager;

import server.model.Auction;
import server.model.Bid;
import server.persistence.Database;
import server.security.CryptoUtils;
import common.Constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Gestor de subastas del sistema.
 * Maneja creación, listado, pujas y cierre de subastas.
 * Thread-safe mediante ConcurrentHashMap y locks internos de Auction.
 * Soporta persistencia opcional con SQLite.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class AuctionManager {

    /** Almacén de subastas (id -> Auction) */
    private final ConcurrentHashMap<String, Auction> auctions;

    /** Base de datos para persistencia (opcional) */
    private Database database;

    /**
     * Constructor del gestor de subastas.
     */
    public AuctionManager() {
        this.auctions = new ConcurrentHashMap<>();
        System.out.println("[AuctionManager] Iniciado");
    }

    /**
     * Establece la base de datos para persistencia.
     *
     * @param database instancia de base de datos
     */
    public void setDatabase(Database database) {
        this.database = database;
    }

    /**
     * Carga subastas desde la base de datos.
     */
    public void loadFromDatabase() {
        if (database == null) {
            return;
        }

        List<Auction> dbAuctions = database.getAllAuctions();
        for (Auction auction : dbAuctions) {
            auction.initializeLock();
            auctions.put(auction.getId(), auction);
        }

        System.out.println("[AuctionManager] Cargadas " + dbAuctions.size() + " subastas desde la BD");
    }

    /**
     * Crea una nueva subasta.
     *
     * @param title título de la subasta
     * @param description descripción del artículo
     * @param seller username del vendedor
     * @param startPrice precio de salida
     * @param durationMinutes duración en minutos
     * @return resultado de la creación
     */
    public CreateAuctionResult createAuction(String title, String description,
            String seller, double startPrice, int durationMinutes) {

        // Validaciones
        if (title == null || title.trim().isEmpty()) {
            return new CreateAuctionResult(false, "El título es obligatorio", null);
        }
        title = title.trim();

        if (title.length() > 100) {
            return new CreateAuctionResult(false,
                "El título no puede tener más de 100 caracteres", null);
        }

        if (description != null && description.length() > 1000) {
            return new CreateAuctionResult(false,
                "La descripción no puede tener más de 1000 caracteres", null);
        }

        if (startPrice < Constants.MIN_START_PRICE) {
            return new CreateAuctionResult(false,
                "El precio de salida debe ser al menos " + Constants.MIN_START_PRICE, null);
        }

        if (durationMinutes < Constants.MIN_AUCTION_DURATION_MINUTES) {
            return new CreateAuctionResult(false,
                "La duración mínima es " + Constants.MIN_AUCTION_DURATION_MINUTES + " minutos", null);
        }

        if (durationMinutes > Constants.MAX_AUCTION_DURATION_MINUTES) {
            return new CreateAuctionResult(false,
                "La duración máxima es " + Constants.MAX_AUCTION_DURATION_MINUTES + " minutos", null);
        }

        // Crear subasta
        String id = CryptoUtils.generateAuctionId();
        Auction auction = new Auction(id, title, description != null ? description.trim() : "",
            seller, startPrice, durationMinutes);

        auctions.put(id, auction);

        // Persistir en BD si está disponible
        if (database != null) {
            database.insertAuction(auction);
        }

        System.out.println("[AuctionManager] Subasta creada: " + id + " por " + seller);
        return new CreateAuctionResult(true, "Subasta creada correctamente", auction);
    }

    /**
     * Obtiene una subasta por su ID.
     *
     * @param auctionId ID de la subasta
     * @return subasta o null si no existe
     */
    public Auction getAuction(String auctionId) {
        if (auctionId == null) {
            return null;
        }
        Auction auction = auctions.get(auctionId);
        if (auction != null) {
            auction.initializeLock();
        }
        return auction;
    }

    /**
     * Lista todas las subastas activas.
     *
     * @return lista de subastas activas ordenadas por tiempo restante
     */
    public List<Auction> getActiveAuctions() {
        return auctions.values().stream()
            .filter(Auction::isActive)
            .sorted((a, b) -> Long.compare(a.getEndTime(), b.getEndTime()))
            .peek(Auction::initializeLock)
            .collect(Collectors.toList());
    }

    /**
     * Lista todas las subastas finalizadas.
     *
     * @return lista de subastas finalizadas
     */
    public List<Auction> getFinishedAuctions() {
        return auctions.values().stream()
            .filter(a -> Constants.AUCTION_STATUS_FINISHED.equals(a.getStatus()))
            .sorted((a, b) -> Long.compare(b.getEndTime(), a.getEndTime()))
            .peek(Auction::initializeLock)
            .collect(Collectors.toList());
    }

    /**
     * Lista subastas creadas por un usuario.
     *
     * @param seller username del vendedor
     * @return lista de subastas del vendedor
     */
    public List<Auction> getAuctionsBySeller(String seller) {
        return auctions.values().stream()
            .filter(a -> seller.equals(a.getSeller()))
            .sorted((a, b) -> Long.compare(b.getStartTime(), a.getStartTime()))
            .peek(Auction::initializeLock)
            .collect(Collectors.toList());
    }

    /**
     * Lista subastas donde un usuario ha pujado.
     *
     * @param bidder username del pujador
     * @return lista de subastas donde ha pujado
     */
    public List<Auction> getAuctionsByBidder(String bidder) {
        return auctions.values().stream()
            .filter(a -> a.getBids().stream().anyMatch(b -> bidder.equals(b.getBidder())))
            .sorted((a, b) -> Long.compare(b.getStartTime(), a.getStartTime()))
            .peek(Auction::initializeLock)
            .collect(Collectors.toList());
    }

    /**
     * Lista subastas ganadas por un usuario.
     *
     * @param winner username del ganador
     * @return lista de subastas ganadas
     */
    public List<Auction> getAuctionsWonBy(String winner) {
        return auctions.values().stream()
            .filter(a -> Constants.AUCTION_STATUS_FINISHED.equals(a.getStatus()))
            .filter(a -> winner.equals(a.getCurrentWinner()))
            .sorted((a, b) -> Long.compare(b.getEndTime(), a.getEndTime()))
            .peek(Auction::initializeLock)
            .collect(Collectors.toList());
    }

    /**
     * Realiza una puja en una subasta.
     *
     * @param auctionId ID de la subasta
     * @param bidder username del pujador
     * @param amount cantidad de la puja
     * @return resultado de la puja
     */
    public BidResult placeBid(String auctionId, String bidder, double amount) {
        Auction auction = getAuction(auctionId);
        if (auction == null) {
            return new BidResult(false, "La subasta no existe", null, null);
        }

        auction.initializeLock();
        Auction.BidResult result = auction.placeBid(bidder, amount);

        if (result.isSuccess()) {
            // Persistir la puja y actualizar subasta en BD
            if (database != null) {
                Bid bid = new Bid(auctionId, bidder, amount);
                database.insertBid(bid);
                database.updateAuction(auction);
            }

            System.out.println("[AuctionManager] Puja registrada: " + amount +
                " de " + bidder + " en " + auctionId);
        }

        return new BidResult(
            result.isSuccess(),
            result.getMessage(),
            result.getPreviousBidder(),
            auction
        );
    }

    /**
     * Obtiene las subastas que han expirado pero no se han cerrado.
     *
     * @return lista de subastas expiradas activas
     */
    public List<Auction> getExpiredAuctions() {
        return auctions.values().stream()
            .filter(a -> Constants.AUCTION_STATUS_ACTIVE.equals(a.getStatus()))
            .filter(Auction::hasExpired)
            .peek(Auction::initializeLock)
            .collect(Collectors.toList());
    }

    /**
     * Cierra una subasta.
     *
     * @param auctionId ID de la subasta
     * @return true si se cerró correctamente
     */
    public boolean closeAuction(String auctionId) {
        Auction auction = getAuction(auctionId);
        if (auction == null) {
            return false;
        }
        auction.initializeLock();
        auction.close();

        // Persistir en BD
        if (database != null) {
            database.updateAuction(auction);
        }

        System.out.println("[AuctionManager] Subasta cerrada: " + auctionId +
            (auction.isDeserted() ? " (desierta)" : " (ganador: " + auction.getCurrentWinner() + ")"));
        return true;
    }

    /**
     * Cancela una subasta (solo ADMIN).
     *
     * @param auctionId ID de la subasta
     * @return resultado de la cancelación
     */
    public CancelResult cancelAuction(String auctionId) {
        Auction auction = getAuction(auctionId);
        if (auction == null) {
            return new CancelResult(false, "La subasta no existe");
        }

        if (!Constants.AUCTION_STATUS_ACTIVE.equals(auction.getStatus())) {
            return new CancelResult(false, "Solo se pueden cancelar subastas activas");
        }

        auction.initializeLock();
        auction.cancel();

        // Persistir en BD
        if (database != null) {
            database.updateAuction(auction);
        }

        System.out.println("[AuctionManager] Subasta cancelada: " + auctionId);
        return new CancelResult(true, "Subasta cancelada correctamente");
    }

    /**
     * Obtiene todas las subastas.
     *
     * @return colección de todas las subastas
     */
    public Collection<Auction> getAllAuctions() {
        return auctions.values();
    }

    /**
     * Guarda una subasta (para persistencia).
     *
     * @param auction subasta a guardar
     */
    public void saveAuction(Auction auction) {
        if (auction != null && auction.getId() != null) {
            auction.initializeLock();
            auctions.put(auction.getId(), auction);
        }
    }

    /**
     * Obtiene el número total de subastas.
     *
     * @return cantidad de subastas
     */
    public int getAuctionCount() {
        return auctions.size();
    }

    /**
     * Obtiene el número de subastas activas.
     *
     * @return cantidad de subastas activas
     */
    public int getActiveAuctionCount() {
        return (int) auctions.values().stream()
            .filter(Auction::isActive)
            .count();
    }

    // ==================== CLASES DE RESULTADO ====================

    /**
     * Resultado de crear una subasta.
     */
    public static class CreateAuctionResult {
        private final boolean success;
        private final String message;
        private final Auction auction;

        public CreateAuctionResult(boolean success, String message, Auction auction) {
            this.success = success;
            this.message = message;
            this.auction = auction;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Auction getAuction() {
            return auction;
        }
    }

    /**
     * Resultado de una puja.
     */
    public static class BidResult {
        private final boolean success;
        private final String message;
        private final String previousBidder;
        private final Auction auction;

        public BidResult(boolean success, String message, String previousBidder, Auction auction) {
            this.success = success;
            this.message = message;
            this.previousBidder = previousBidder;
            this.auction = auction;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getPreviousBidder() {
            return previousBidder;
        }

        public Auction getAuction() {
            return auction;
        }
    }

    /**
     * Resultado de cancelar una subasta.
     */
    public static class CancelResult {
        private final boolean success;
        private final String message;

        public CancelResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
