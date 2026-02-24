package server.model;

/**
 * Modelo que representa una puja en una subasta.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class Bid {

    /** ID único de la puja */
    private long id;

    /** ID de la subasta */
    private String auctionId;

    /** Username del pujador */
    private String bidder;

    /** Cantidad pujada */
    private double amount;

    /** Timestamp de la puja */
    private long timestamp;

    /**
     * Constructor por defecto.
     */
    public Bid() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Constructor completo.
     *
     * @param auctionId ID de la subasta
     * @param bidder username del pujador
     * @param amount cantidad pujada
     */
    public Bid(String auctionId, String bidder, double amount) {
        this();
        this.auctionId = auctionId;
        this.bidder = bidder;
        this.amount = amount;
    }

    // ==================== GETTERS Y SETTERS ====================

    /**
     * @return ID de la puja
     */
    public long getId() {
        return id;
    }

    /**
     * @param id ID de la puja
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return ID de la subasta
     */
    public String getAuctionId() {
        return auctionId;
    }

    /**
     * @param auctionId ID de la subasta
     */
    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    /**
     * @return username del pujador
     */
    public String getBidder() {
        return bidder;
    }

    /**
     * @param bidder username del pujador
     */
    public void setBidder(String bidder) {
        this.bidder = bidder;
    }

    /**
     * @return cantidad pujada
     */
    public double getAmount() {
        return amount;
    }

    /**
     * @param amount cantidad pujada
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }

    /**
     * @return timestamp de la puja
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param timestamp timestamp de la puja
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Bid{" +
                "bidder='" + bidder + '\'' +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                '}';
    }
}
