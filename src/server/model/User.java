package server.model;

import common.Constants;

/**
 * Modelo que representa un usuario del sistema.
 * Almacena información de autenticación y estado de la cuenta.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class User {

    /** Nombre de usuario único */
    private String username;

    /** Hash de la contraseña (SHA-256) */
    private String passwordHash;

    /** Salt usado para el hash */
    private String salt;

    /** Correo electrónico */
    private String email;

    /** Rol del usuario (USER o ADMIN) */
    private String role;

    /** Indica si la cuenta está bloqueada */
    private boolean blocked;

    /** Número de intentos de login fallidos */
    private int failedAttempts;

    /** Timestamp hasta cuando está bloqueada la cuenta (null si no está bloqueada) */
    private Long blockedUntil;

    /** Timestamp de creación de la cuenta */
    private long createdAt;

    /**
     * Constructor por defecto.
     */
    public User() {
        this.role = Constants.ROLE_USER;
        this.blocked = false;
        this.failedAttempts = 0;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Constructor completo para registro.
     *
     * @param username nombre de usuario
     * @param passwordHash hash de la contraseña
     * @param salt salt usado
     * @param email correo electrónico
     */
    public User(String username, String passwordHash, String salt, String email) {
        this();
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.email = email;
    }

    // ==================== GETTERS Y SETTERS ====================

    /**
     * @return nombre de usuario
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username nombre de usuario
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return hash de la contraseña
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * @param passwordHash hash de la contraseña
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * @return salt usado para el hash
     */
    public String getSalt() {
        return salt;
    }

    /**
     * @param salt salt a usar
     */
    public void setSalt(String salt) {
        this.salt = salt;
    }

    /**
     * @return correo electrónico
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email correo electrónico
     */
    public void setEmail(String email) {
        this.email = email;
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
     * @return true si la cuenta está bloqueada
     */
    public boolean isBlocked() {
        return blocked;
    }

    /**
     * @param blocked estado de bloqueo
     */
    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    /**
     * @return número de intentos fallidos
     */
    public int getFailedAttempts() {
        return failedAttempts;
    }

    /**
     * @param failedAttempts número de intentos fallidos
     */
    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    /**
     * @return timestamp de bloqueo o null
     */
    public Long getBlockedUntil() {
        return blockedUntil;
    }

    /**
     * @param blockedUntil timestamp de bloqueo
     */
    public void setBlockedUntil(Long blockedUntil) {
        this.blockedUntil = blockedUntil;
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
     * Verifica si el usuario es administrador.
     *
     * @return true si es ADMIN
     */
    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equals(role);
    }

    /**
     * Verifica si la cuenta está temporalmente bloqueada.
     *
     * @return true si está bloqueada y el tiempo no ha expirado
     */
    public boolean isTemporarilyLocked() {
        if (blockedUntil == null) {
            return false;
        }
        if (System.currentTimeMillis() >= blockedUntil) {
            // El bloqueo ha expirado
            blockedUntil = null;
            failedAttempts = 0;
            return false;
        }
        return true;
    }

    /**
     * Incrementa los intentos fallidos y bloquea si es necesario.
     */
    public void incrementFailedAttempts() {
        failedAttempts++;
        if (failedAttempts >= Constants.MAX_LOGIN_ATTEMPTS) {
            blockedUntil = System.currentTimeMillis() + Constants.ACCOUNT_LOCK_DURATION_MS;
        }
    }

    /**
     * Resetea los intentos fallidos tras un login exitoso.
     */
    public void resetFailedAttempts() {
        failedAttempts = 0;
        blockedUntil = null;
    }

    /**
     * Calcula los segundos restantes de bloqueo.
     *
     * @return segundos restantes o 0 si no está bloqueada
     */
    public long getRemainingLockSeconds() {
        if (blockedUntil == null) {
            return 0;
        }
        long remaining = (blockedUntil - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", blocked=" + blocked +
                '}';
    }
}
