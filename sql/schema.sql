-- Schema de base de datos para NetAuction
-- SQLite

-- Tabla de usuarios
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
);

-- Tabla de subastas
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
);

-- Tabla de pujas
CREATE TABLE IF NOT EXISTS bids (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    auction_id      TEXT NOT NULL,
    bidder          TEXT NOT NULL,
    amount          REAL NOT NULL,
    timestamp       INTEGER NOT NULL,
    FOREIGN KEY (auction_id) REFERENCES auctions(id),
    FOREIGN KEY (bidder) REFERENCES users(username)
);

-- Índices para mejorar rendimiento
CREATE INDEX IF NOT EXISTS idx_auctions_status ON auctions(status);
CREATE INDEX IF NOT EXISTS idx_auctions_seller ON auctions(seller);
CREATE INDEX IF NOT EXISTS idx_auctions_end_time ON auctions(end_time);
CREATE INDEX IF NOT EXISTS idx_bids_auction ON bids(auction_id);
CREATE INDEX IF NOT EXISTS idx_bids_bidder ON bids(bidder);
