package server.service;

import common.Constants;
import server.manager.AuctionManager;
import server.model.Auction;
import server.util.AuditLogger;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Planificador que revisa y cierra subastas expiradas.
 * Se ejecuta periódicamente para detectar subastas que han terminado.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class AuctionScheduler implements Runnable {

    /** Gestor de subastas */
    private final AuctionManager auctionManager;

    /** Servicio de notificaciones */
    private final NotificationService notificationService;

    /** Logger de auditoría */
    private final AuditLogger auditLogger;

    /** Executor para la planificación */
    private ScheduledExecutorService scheduler;

    /** Indica si el scheduler está activo */
    private volatile boolean running;

    /**
     * Constructor del planificador.
     *
     * @param auctionManager gestor de subastas
     * @param notificationService servicio de notificaciones
     * @param auditLogger logger de auditoría
     */
    public AuctionScheduler(AuctionManager auctionManager, NotificationService notificationService,
                           AuditLogger auditLogger) {
        this.auctionManager = auctionManager;
        this.notificationService = notificationService;
        this.auditLogger = auditLogger;
        this.running = false;
    }

    /**
     * Inicia el planificador.
     */
    public void start() {
        if (running) {
            return;
        }

        running = true;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AuctionScheduler");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(
            this,
            Constants.AUCTION_CHECK_INTERVAL_MS,
            Constants.AUCTION_CHECK_INTERVAL_MS,
            TimeUnit.MILLISECONDS
        );

        System.out.println("[AuctionScheduler] Iniciado. Revisando cada " +
            Constants.AUCTION_CHECK_INTERVAL_MS + "ms");
    }

    /**
     * Detiene el planificador.
     */
    public void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("[AuctionScheduler] Detenido");
    }

    /**
     * Ejecuta una revisión de subastas expiradas.
     */
    @Override
    public void run() {
        if (!running) {
            return;
        }

        try {
            checkExpiredAuctions();
        } catch (Exception e) {
            System.err.println("[AuctionScheduler] Error en revisión: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Revisa y cierra las subastas expiradas.
     */
    private void checkExpiredAuctions() {
        List<Auction> expiredAuctions = auctionManager.getExpiredAuctions();

        for (Auction auction : expiredAuctions) {
            closeAuction(auction);
        }
    }

    /**
     * Cierra una subasta y envía las notificaciones correspondientes.
     *
     * @param auction subasta a cerrar
     */
    private void closeAuction(Auction auction) {
        String auctionId = auction.getId();
        String title = auction.getTitle();
        String seller = auction.getSeller();
        String winner = auction.getCurrentWinner();
        double finalPrice = auction.getCurrentPrice();
        boolean isDesierta = auction.isDeserted();

        // Cerrar la subasta
        auctionManager.closeAuction(auctionId);

        // Registrar en log
        String logMessage = isDesierta ?
            "Subasta cerrada (desierta): " + auctionId :
            "Subasta cerrada: " + auctionId + " - Ganador: " + winner + " - Precio: " + finalPrice;

        System.out.println("[AuctionScheduler] " + logMessage);

        if (auditLogger != null) {
            auditLogger.log("AUCTION_CLOSED", "SYSTEM", "localhost", logMessage);
        }

        // Enviar notificaciones
        if (notificationService != null) {
            notificationService.notifyAuctionClosed(auctionId, title, winner, finalPrice, seller);
        }
    }

    /**
     * Verifica si el scheduler está ejecutándose.
     *
     * @return true si está activo
     */
    public boolean isRunning() {
        return running;
    }
}
