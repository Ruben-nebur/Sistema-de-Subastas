package server.model;

import common.Constants;

/**
 * Modelo que representa una sesión de usuario.
 * Incluye el token UUID y tiempo de expiración.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class Session {

    /** Token UUID de la sesión */
    private String token;

    /** Username del usuario */
    private String username;

    /** Rol del usuario */
    private String role;

    /** Timestamp de expiración */
    private long expirationTime;

    /** Timestamp de creación */
    private long createdAt;

    /**
     * Constructor por defecto.
     */
    public Session() {
        this.createdAt = System.currentTimeMillis();
        this.expirationTime = this.createdAt + Constants.SESSION_DURATION_MS;
    }

    /**
     * Constructor completo.
     *
     * @param token token UUID
     * @param username username del usuario
     * @param role rol del usuario
     */
    public Session(String token, String username, String role) {
        this();
        this.token = token;
        this.username = username;
        this.role = role;
    }

    // ==================== GETTERS Y SETTERS ====================

    /**
     * @return token de sesión
     */
    public String getToken() {
        return token;
    }

    /**
     * @param token token de sesión
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * @return username del usuario
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username username del usuario
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return rol del usuario
     */
    public String getRole() {
        return role;
    }

    /**
     * @param role rol del usuario
     */
    public void setRole(String role) {
        this.role = role;
    }

    /**
     * @return timestamp de expiración
     */
    public long getExpirationTime() {
        return expirationTime;
    }

    /**
     * @param expirationTime timestamp de expiración
     */
    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    /**
     * @return timestamp de creación
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * @param createdAt timestamp de creación
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    // ==================== MÉTODOS DE UTILIDAD ====================

    /**
     * Verifica si la sesión ha expirado.
     *
     * @return true si ha expirado
     */
    public boolean isExpired() {
        return System.currentTimeMillis() >= expirationTime;
    }

    /**
     * Verifica si la sesión es válida (no expirada).
     *
     * @return true si es válida
     */
    public boolean isValid() {
        return !isExpired();
    }

    /**
     * Renueva la sesión extendiendo el tiempo de expiración.
     */
    public void renew() {
        this.expirationTime = System.currentTimeMillis() + Constants.SESSION_DURATION_MS;
    }

    /**
     * Calcula los segundos restantes de la sesión.
     *
     * @return segundos restantes o 0 si ha expirado
     */
    public long getRemainingSeconds() {
        long remaining = (expirationTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    /**
     * Verifica si el usuario tiene rol de administrador.
     *
     * @return true si es ADMIN
     */
    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equals(role);
    }

    @Override
    public String toString() {
        return "Session{" +
                "token='" + token + '\'' +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", expired=" + isExpired() +
                '}';
    }
}
