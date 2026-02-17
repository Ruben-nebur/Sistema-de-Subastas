package server.model;

import common.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Modelo que representa una subasta.
 * Incluye sincronización con ReadWriteLock para operaciones de puja.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class Auction {

    /** ID único de la subasta */
    private String id;

    /** Título de la subasta */
    private String title;

    /** Descripción del artículo */
    private String description;

    /** Username del vendedor */
    private String seller;

    /** Precio de salida */
    private double startPrice;

    /** Precio actual (última puja o precio de salida) */
    private double currentPrice;

    /** Username del pujador actual (null si no hay pujas) */
    private String currentWinner;

    /** Timestamp de inicio de la subasta */
    private long startTime;

    /** Timestamp de fin de la subasta */
    private long endTime;

    /** Estado de la subasta (ACTIVE, FINISHED, CANCELLED) */
    private String status;

    /** Lista de pujas */
    private List<Bid> bids;

    /** Lock para sincronización de pujas */
    private transient ReadWriteLock lock;

    /**
     * Constructor por defecto.
     */
    public Auction() {
        this.bids = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();
        this.status = Constants.AUCTION_STATUS_ACTIVE;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * Constructor para crear una nueva subasta.
     *
     * @param id ID único
     * @param title título de la subasta
     * @param description descripción del artículo
     * @param seller username del vendedor
     * @param startPrice precio de salida
     * @param durationMinutes duración en minutos
     */
    public Auction(String id, String title, String description, String seller,
                   double startPrice, int durationMinutes) {
        this();
        this.id = id;
        this.title = title;
        this.description = description;
        this.seller = seller;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.endTime = this.startTime + (durationMinutes * 60 * 1000L);
    }

    // ==================== GETTERS Y SETTERS ====================

    /**
     * @return ID de la subasta
     */
    public String getId() {
        return id;
    }

    /**
     * @param id ID de la subasta
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return título de la subasta
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title título de la subasta
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return descripción del artículo
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description descripción del artículo
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return username del vendedor
     */
    public String getSeller() {
        return seller;
    }

    /**
     * @param seller username del vendedor
     */
    public void setSeller(String seller) {
        this.seller = seller;
    }

    /**
     * @return precio de salida
     */
    public double getStartPrice() {
        return startPrice;
    }

    /**
     * @param startPrice precio de salida
     */
    public void setStartPrice(double startPrice) {
        this.startPrice = startPrice;
    }

    /**
     * @return precio actual
     */
    public double getCurrentPrice() {
        lock.readLock().lock();
        try {
            return currentPrice;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param currentPrice precio actual
     */
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    /**
     * @return username del pujador actual
     */
    public String getCurrentWinner() {
        lock.readLock().lock();
        try {
            return currentWinner;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param currentWinner username del pujador actual
     */
    public void setCurrentWinner(String currentWinner) {
        this.currentWinner = currentWinner;
    }

    /**
     * @return timestamp de inicio
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @param startTime timestamp de inicio
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * @return timestamp de fin
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * @param endTime timestamp de fin
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    /**
     * @return estado de la subasta
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status estado de la subasta
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return lista inmutable de pujas
     */
    public List<Bid> getBids() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(bids));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param bids lista de pujas
     */
    public void setBids(List<Bid> bids) {
        this.bids = bids != null ? new ArrayList<>(bids) : new ArrayList<>();
    }

    // ==================== MÉTODOS DE NEGOCIO ====================

    /**
     * Inicializa el lock después de deserialización.
     */
    public void initializeLock() {
        if (this.lock == null) {
            this.lock = new ReentrantReadWriteLock();
        }
    }

    /**
     * Verifica si la subasta está activa.
     *
     * @return true si está activa y no ha expirado
     */
    public boolean isActive() {
        return Constants.AUCTION_STATUS_ACTIVE.equals(status) &&
               System.currentTimeMillis() < endTime;
    }

    /**
     * Verifica si la subasta ha expirado.
     *
     * @return true si el tiempo ha pasado
     */
    public boolean hasExpired() {
        return System.currentTimeMillis() >= endTime;
    }

    /**
     * Calcula el tiempo restante en segundos.
     *
     * @return segundos restantes o 0 si ha terminado
     */
    public long getRemainingSeconds() {
        long remaining = (endTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    /**
     * Calcula el tiempo restante en formato legible.
     *
     * @return cadena con el tiempo restante (ej: "5m 30s")
     */
    public String getRemainingTimeFormatted() {
        long seconds = getRemainingSeconds();
        if (seconds <= 0) {
            return "Finalizada";
        }
        long minutes = seconds / 60;
        long secs = seconds % 60;
        if (minutes > 0) {
            return minutes + "m " + secs + "s";
        }
        return secs + "s";
    }

    /**
     * @return número de pujas
     */
    public int getBidCount() {
        lock.readLock().lock();
        try {
            return bids.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Verifica si la subasta quedó desierta (sin pujas).
     *
     * @return true si no hubo pujas
     */
    public boolean isDeserted() {
        lock.readLock().lock();
        try {
            return bids.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Realiza una puja de forma atómica y sincronizada.
     *
     * @param bidder username del pujador
     * @param amount cantidad de la puja
     * @return resultado de la puja
     */
    public BidResult placeBid(String bidder, double amount) {
        lock.writeLock().lock();
        try {
            // Validaciones
            if (!isActive()) {
                return new BidResult(false, "La subasta no está activa", null);
            }

            if (seller.equals(bidder)) {
                return new BidResult(false, "No puedes pujar en tu propia subasta", null);
            }

            if (amount <= currentPrice) {
                return new BidResult(false,
                    "La puja debe ser mayor que el precio actual (" + currentPrice + ")",
                    null);
            }

            // Guardar el pujador anterior para notificación OUTBID
            String previousBidder = currentWinner;

            // Registrar la puja
            Bid bid = new Bid(id, bidder, amount);
            bids.add(bid);

            // Actualizar estado
            currentPrice = amount;
            currentWinner = bidder;

            return new BidResult(true, "Puja registrada correctamente", previousBidder);

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Cierra la subasta.
     */
    public void close() {
        lock.writeLock().lock();
        try {
            this.status = Constants.AUCTION_STATUS_FINISHED;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Cancela la subasta.
     */
    public void cancel() {
        lock.writeLock().lock();
        try {
            this.status = Constants.AUCTION_STATUS_CANCELLED;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        return "Auction{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", seller='" + seller + '\'' +
                ", currentPrice=" + currentPrice +
                ", status='" + status + '\'' +
                '}';
    }

    // ==================== CLASE INTERNA PARA RESULTADO DE PUJA ====================

    /**
     * Resultado de una operación de puja.
     */
    public static class BidResult {
        private final boolean success;
        private final String message;
        private final String previousBidder;

        /**
         * Constructor del resultado.
         *
         * @param success si la puja fue exitosa
         * @param message mensaje descriptivo
         * @param previousBidder username del pujador anterior (para OUTBID)
         */
        public BidResult(boolean success, String message, String previousBidder) {
            this.success = success;
            this.message = message;
            this.previousBidder = previousBidder;
        }

        /**
         * @return true si la puja fue exitosa
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * @return mensaje descriptivo
         */
        public String getMessage() {
            return message;
        }

        /**
         * @return username del pujador anterior
         */
        public String getPreviousBidder() {
            return previousBidder;
        }
    }
}
