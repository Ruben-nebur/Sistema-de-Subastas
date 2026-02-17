package server.persistence;

import common.Constants;
import server.model.User;
import server.model.Auction;
import server.model.Bid;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase de persistencia que maneja la conexión con SQLite.
 * Proporciona métodos CRUD para usuarios, subastas y pujas.
 * Utiliza PreparedStatement para prevenir SQL injection.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class Database {

    /** URL de conexión JDBC */
    private final String dbUrl;

    /** Conexión a la base de datos */
    private Connection connection;

    /**
     * Constructor con ruta personalizada.
     *
     * @param dbPath ruta del archivo de base de datos
     */
    public Database(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
    }

    /**
     * Constructor con ruta por defecto.
     */
    public Database() {
        this(Constants.DATABASE_PATH);
    }

    /**
     * Inicializa la base de datos: crea directorio, conexión y tablas.
     *
     * @throws SQLException si hay error de conexión
     */
    public void initialize() throws SQLException {
        // Crear directorio si no existe
        File dbFile = new File(Constants.DATABASE_PATH);
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Cargar driver JDBC de SQLite de forma explicita para entornos
        // donde el auto-registro por SPI no se aplica correctamente.
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver SQLite JDBC no encontrado en classpath", e);
        }

        // Establecer conexión
        connection = DriverManager.getConnection(dbUrl);

        // Crear tablas
        createTables();

        System.out.println("[Database] Inicializada en: " + dbUrl);
    }

    /**
     * Crea las tablas si no existen.
     */
    private void createTables() throws SQLException {
        String createUsers = """
            CREATE TABLE IF NOT EXISTS users (
                username        TEXT PRIMARY KEY,
                password_hash   TEXT NOT NULL,
                salt            TEXT NOT NULL,
                email           TEXT NOT NULL,
                role            TEXT NOT NULL DEFAULT 'USER',
                blocked         INTEGER NOT NULL DEFAULT 0,
                failed_attempts INTEGER NOT NULL DEFAULT 0,
                blocked_until   INTEGER DEFAULT NULL,
                created_at      INTEGER NOT NULL
            )
            """;

        String createAuctions = """
            CREATE TABLE IF NOT EXISTS auctions (
                id              TEXT PRIMARY KEY,
                title           TEXT NOT NULL,
                description     TEXT,
                seller          TEXT NOT NULL,
                start_price     REAL NOT NULL,
                current_price   REAL NOT NULL,
                current_winner  TEXT,
                start_time      INTEGER NOT NULL,
                end_time        INTEGER NOT NULL,
                status          TEXT NOT NULL DEFAULT 'ACTIVE',
                FOREIGN KEY (seller) REFERENCES users(username),
                FOREIGN KEY (current_winner) REFERENCES users(username)
            )
            """;

        String createBids = """
            CREATE TABLE IF NOT EXISTS bids (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                auction_id      TEXT NOT NULL,
                bidder          TEXT NOT NULL,
                amount          REAL NOT NULL,
                timestamp       INTEGER NOT NULL,
                FOREIGN KEY (auction_id) REFERENCES auctions(id),
                FOREIGN KEY (bidder) REFERENCES users(username)
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsers);
            stmt.execute(createAuctions);
            stmt.execute(createBids);

            // Crear índices
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_auctions_status ON auctions(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_auctions_seller ON auctions(seller)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bids_auction ON bids(auction_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_bids_bidder ON bids(bidder)");
        }
    }

    /**
     * Obtiene la conexión activa.
     *
     * @return conexión JDBC
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Verifica si la conexión está activa.
     *
     * @return true si está conectado
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // ==================== OPERACIONES DE USUARIOS ====================

    /**
     * Inserta un nuevo usuario.
     *
     * @param user usuario a insertar
     * @return true si se insertó correctamente
     */
    public boolean insertUser(User user) {
        String sql = """
            INSERT INTO users (username, password_hash, salt, email, role, blocked,
                              failed_attempts, blocked_until, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getSalt());
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getRole());
            pstmt.setInt(6, user.isBlocked() ? 1 : 0);
            pstmt.setInt(7, user.getFailedAttempts());
            if (user.getBlockedUntil() != null) {
                pstmt.setLong(8, user.getBlockedUntil());
            } else {
                pstmt.setNull(8, Types.INTEGER);
            }
            pstmt.setLong(9, user.getCreatedAt());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[Database] Error insertando usuario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza un usuario existente.
     *
     * @param user usuario a actualizar
     * @return true si se actualizó correctamente
     */
    public boolean updateUser(User user) {
        String sql = """
            UPDATE users SET password_hash = ?, salt = ?, email = ?, role = ?,
                            blocked = ?, failed_attempts = ?, blocked_until = ?
            WHERE username = ?
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, user.getPasswordHash());
            pstmt.setString(2, user.getSalt());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getRole());
            pstmt.setInt(5, user.isBlocked() ? 1 : 0);
            pstmt.setInt(6, user.getFailedAttempts());
            if (user.getBlockedUntil() != null) {
                pstmt.setLong(7, user.getBlockedUntil());
            } else {
                pstmt.setNull(7, Types.INTEGER);
            }
            pstmt.setString(8, user.getUsername());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[Database] Error actualizando usuario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene un usuario por su nombre.
     *
     * @param username nombre de usuario
     * @return usuario o null si no existe
     */
    public User getUser(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("[Database] Error obteniendo usuario: " + e.getMessage());
        }
        return null;
    }

    /**
     * Obtiene todos los usuarios.
     *
     * @return lista de usuarios
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("[Database] Error obteniendo usuarios: " + e.getMessage());
        }
        return users;
    }

    /**
     * Verifica si existe un usuario.
     *
     * @param username nombre de usuario
     * @return true si existe
     */
    public boolean userExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Mapea un ResultSet a un objeto User.
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setSalt(rs.getString("salt"));
        user.setEmail(rs.getString("email"));
        user.setRole(rs.getString("role"));
        user.setBlocked(rs.getInt("blocked") == 1);
        user.setFailedAttempts(rs.getInt("failed_attempts"));
        long blockedUntil = rs.getLong("blocked_until");
        user.setBlockedUntil(rs.wasNull() ? null : blockedUntil);
        user.setCreatedAt(rs.getLong("created_at"));
        return user;
    }

    // ==================== OPERACIONES DE SUBASTAS ====================

    /**
     * Inserta una nueva subasta.
     *
     * @param auction subasta a insertar
     * @return true si se insertó correctamente
     */
    public boolean insertAuction(Auction auction) {
        String sql = """
            INSERT INTO auctions (id, title, description, seller, start_price,
                                 current_price, current_winner, start_time, end_time, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, auction.getId());
            pstmt.setString(2, auction.getTitle());
            pstmt.setString(3, auction.getDescription());
            pstmt.setString(4, auction.getSeller());
            pstmt.setDouble(5, auction.getStartPrice());
            pstmt.setDouble(6, auction.getCurrentPrice());
            pstmt.setString(7, auction.getCurrentWinner());
            pstmt.setLong(8, auction.getStartTime());
            pstmt.setLong(9, auction.getEndTime());
            pstmt.setString(10, auction.getStatus());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[Database] Error insertando subasta: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza una subasta existente.
     *
     * @param auction subasta a actualizar
     * @return true si se actualizó correctamente
     */
    public boolean updateAuction(Auction auction) {
        String sql = """
            UPDATE auctions SET title = ?, description = ?, current_price = ?,
                               current_winner = ?, status = ?
            WHERE id = ?
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, auction.getTitle());
            pstmt.setString(2, auction.getDescription());
            pstmt.setDouble(3, auction.getCurrentPrice());
            pstmt.setString(4, auction.getCurrentWinner());
            pstmt.setString(5, auction.getStatus());
            pstmt.setString(6, auction.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[Database] Error actualizando subasta: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene una subasta por su ID.
     *
     * @param auctionId ID de la subasta
     * @return subasta o null si no existe
     */
    public Auction getAuction(String auctionId) {
        String sql = "SELECT * FROM auctions WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Auction auction = mapResultSetToAuction(rs);
                // Cargar pujas
                auction.setBids(getBidsByAuction(auctionId));
                return auction;
            }
        } catch (SQLException e) {
            System.err.println("[Database] Error obteniendo subasta: " + e.getMessage());
        }
        return null;
    }

    /**
     * Obtiene todas las subastas.
     *
     * @return lista de subastas
     */
    public List<Auction> getAllAuctions() {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Auction auction = mapResultSetToAuction(rs);
                auction.setBids(getBidsByAuction(auction.getId()));
                auctions.add(auction);
            }
        } catch (SQLException e) {
            System.err.println("[Database] Error obteniendo subastas: " + e.getMessage());
        }
        return auctions;
    }

    /**
     * Obtiene subastas por estado.
     *
     * @param status estado de las subastas
     * @return lista de subastas
     */
    public List<Auction> getAuctionsByStatus(String status) {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM auctions WHERE status = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Auction auction = mapResultSetToAuction(rs);
                auction.setBids(getBidsByAuction(auction.getId()));
                auctions.add(auction);
            }
        } catch (SQLException e) {
            System.err.println("[Database] Error obteniendo subastas por estado: " + e.getMessage());
        }
        return auctions;
    }

    /**
     * Mapea un ResultSet a un objeto Auction.
     */
    private Auction mapResultSetToAuction(ResultSet rs) throws SQLException {
        Auction auction = new Auction();
        auction.setId(rs.getString("id"));
        auction.setTitle(rs.getString("title"));
        auction.setDescription(rs.getString("description"));
        auction.setSeller(rs.getString("seller"));
        auction.setStartPrice(rs.getDouble("start_price"));
        auction.setCurrentPrice(rs.getDouble("current_price"));
        auction.setCurrentWinner(rs.getString("current_winner"));
        auction.setStartTime(rs.getLong("start_time"));
        auction.setEndTime(rs.getLong("end_time"));
        auction.setStatus(rs.getString("status"));
        auction.initializeLock();
        return auction;
    }

    // ==================== OPERACIONES DE PUJAS ====================

    /**
     * Inserta una nueva puja.
     *
     * @param bid puja a insertar
     * @return true si se insertó correctamente
     */
    public boolean insertBid(Bid bid) {
        String sql = "INSERT INTO bids (auction_id, bidder, amount, timestamp) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, bid.getAuctionId());
            pstmt.setString(2, bid.getBidder());
            pstmt.setDouble(3, bid.getAmount());
            pstmt.setLong(4, bid.getTimestamp());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    bid.setId(generatedKeys.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[Database] Error insertando puja: " + e.getMessage());
        }
        return false;
    }

    /**
     * Obtiene las pujas de una subasta.
     *
     * @param auctionId ID de la subasta
     * @return lista de pujas ordenadas por timestamp
     */
    public List<Bid> getBidsByAuction(String auctionId) {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY timestamp ASC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bids.add(mapResultSetToBid(rs));
            }
        } catch (SQLException e) {
            System.err.println("[Database] Error obteniendo pujas: " + e.getMessage());
        }
        return bids;
    }

    /**
     * Obtiene las pujas de un usuario.
     *
     * @param bidder username del pujador
     * @return lista de pujas
     */
    public List<Bid> getBidsByBidder(String bidder) {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT * FROM bids WHERE bidder = ? ORDER BY timestamp DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, bidder);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bids.add(mapResultSetToBid(rs));
            }
        } catch (SQLException e) {
            System.err.println("[Database] Error obteniendo pujas por usuario: " + e.getMessage());
        }
        return bids;
    }

    /**
     * Mapea un ResultSet a un objeto Bid.
     */
    private Bid mapResultSetToBid(ResultSet rs) throws SQLException {
        Bid bid = new Bid();
        bid.setId(rs.getLong("id"));
        bid.setAuctionId(rs.getString("auction_id"));
        bid.setBidder(rs.getString("bidder"));
        bid.setAmount(rs.getDouble("amount"));
        bid.setTimestamp(rs.getLong("timestamp"));
        return bid;
    }

    // ==================== OPERACIONES DE MANTENIMIENTO ====================

    /**
     * Cierra la conexión a la base de datos.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[Database] Conexión cerrada");
            }
        } catch (SQLException e) {
            System.err.println("[Database] Error cerrando conexión: " + e.getMessage());
        }
    }

    /**
     * Ejecuta una transacción.
     *
     * @param transaction operación a ejecutar
     * @return true si la transacción fue exitosa
     */
    public boolean executeTransaction(TransactionOperation transaction) {
        try {
            connection.setAutoCommit(false);
            transaction.execute();
            connection.commit();
            return true;
        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                System.err.println("[Database] Error en rollback: " + rollbackEx.getMessage());
            }
            System.err.println("[Database] Error en transacción: " + e.getMessage());
            return false;
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("[Database] Error restaurando autocommit: " + e.getMessage());
            }
        }
    }

    /**
     * Interface funcional para operaciones transaccionales.
     */
    @FunctionalInterface
    public interface TransactionOperation {
        void execute() throws Exception;
    }
}
