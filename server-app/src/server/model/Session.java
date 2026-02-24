package server.model;

/**
 * Modelo que representa una sesion de usuario.
 */
public class Session {

    /** Token UUID de la sesion */
    private String token;

    /** Username del usuario */
    private String username;

    /** Timestamp de creacion */
    private long createdAt;

    /**
     * Constructor por defecto.
     */
    public Session() {
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Constructor completo.
     *
     * @param token token UUID
     * @param username username del usuario
     */
    public Session(String token, String username) {
        this();
        this.token = token;
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * En la version simplificada no hay expiracion automatica.
     */
    public boolean isExpired() {
        return false;
    }

    public boolean isValid() {
        return true;
    }

    public void renew() {
        // Sin expiracion automatica.
    }

    public long getRemainingSeconds() {
        return -1;
    }

    @Override
    public String toString() {
        return "Session{" +
                "token='" + token + '\'' +
                ", username='" + username + '\'' +
                ", expired=" + isExpired() +
                '}';
    }
}

