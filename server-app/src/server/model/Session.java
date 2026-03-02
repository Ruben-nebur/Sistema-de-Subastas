package server.model;

/**
 * Modelo que representa una sesion de usuario.
 * Implementacion simplificada sin expiracion automatica.
 *
 * @author NetAuction Team
 * @version 1.0
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

    /**
     * Obtiene el token de la sesion.
     *
     * @return token UUID
     */
    public String getToken() {
        return token;
    }

    /**
     * Establece el token de la sesion.
     *
     * @param token token UUID
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Obtiene el nombre de usuario asociado a la sesion.
     *
     * @return nombre de usuario
     */
    public String getUsername() {
        return username;
    }

    /**
     * Establece el nombre de usuario asociado a la sesion.
     *
     * @param username nombre de usuario
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Obtiene el timestamp de creacion de la sesion.
     *
     * @return timestamp en milisegundos
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Establece el timestamp de creacion de la sesion.
     *
     * @param createdAt timestamp en milisegundos
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Verifica si la sesion ha expirado.
     * En la version simplificada siempre devuelve false.
     *
     * @return false siempre (sin expiracion automatica)
     */
    public boolean isExpired() {
        return false;
    }

    /**
     * Verifica si la sesion es valida.
     * En la version simplificada siempre devuelve true.
     *
     * @return true siempre (sin expiracion automatica)
     */
    public boolean isValid() {
        return true;
    }

    /**
     * Renueva la sesion.
     * En la version simplificada no realiza ninguna accion.
     */
    public void renew() {
        // Sin expiracion automatica.
    }

    /**
     * Obtiene los segundos restantes de la sesion.
     * En la version simplificada devuelve -1 (sin limite).
     *
     * @return -1 siempre (sin expiracion automatica)
     */
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
