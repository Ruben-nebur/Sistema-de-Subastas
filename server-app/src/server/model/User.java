package server.model;

/**
 * Modelo que representa un usuario del sistema NetAuction.
 * Almacena las credenciales, informacion de contacto y estado del usuario.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class User {

    /** Nombre de usuario unico */
    private String username;

    /** Hash de la contrasena (SHA-256) */
    private String passwordHash;

    /** Salt utilizado para el hash de la contrasena */
    private String salt;

    /** Correo electronico del usuario */
    private String email;

    /** Indica si el usuario esta bloqueado */
    private boolean blocked;

    /** Timestamp de creacion del usuario en milisegundos */
    private long createdAt;

    /**
     * Constructor por defecto.
     * Inicializa el usuario como no bloqueado con la fecha de creacion actual.
     */
    public User() {
        this.blocked = false;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Constructor completo.
     *
     * @param username nombre de usuario
     * @param passwordHash hash de la contrasena
     * @param salt salt del hash
     * @param email correo electronico
     */
    public User(String username, String passwordHash, String salt, String email) {
        this();
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.email = email;
    }

    /**
     * Obtiene el nombre de usuario.
     *
     * @return nombre de usuario
     */
    public String getUsername() {
        return username;
    }

    /**
     * Establece el nombre de usuario.
     *
     * @param username nombre de usuario
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Obtiene el hash de la contrasena.
     *
     * @return hash de la contrasena en Base64
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Establece el hash de la contrasena.
     *
     * @param passwordHash hash de la contrasena en Base64
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Obtiene el salt del hash.
     *
     * @return salt en Base64
     */
    public String getSalt() {
        return salt;
    }

    /**
     * Establece el salt del hash.
     *
     * @param salt salt en Base64
     */
    public void setSalt(String salt) {
        this.salt = salt;
    }

    /**
     * Obtiene el correo electronico.
     *
     * @return correo electronico
     */
    public String getEmail() {
        return email;
    }

    /**
     * Establece el correo electronico.
     *
     * @param email correo electronico
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Indica si el usuario esta bloqueado.
     *
     * @return true si esta bloqueado
     */
    public boolean isBlocked() {
        return blocked;
    }

    /**
     * Establece el estado de bloqueo del usuario.
     *
     * @param blocked true para bloquear
     */
    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    /**
     * Obtiene el timestamp de creacion del usuario.
     *
     * @return timestamp en milisegundos
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Establece el timestamp de creacion del usuario.
     *
     * @param createdAt timestamp en milisegundos
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", blocked=" + blocked +
                '}';
    }
}
