package server.model;

/**
 * Modelo que representa un usuario del sistema.
 */
public class User {

    private String username;
    private String passwordHash;
    private String salt;
    private String email;
    private boolean blocked;
    private long createdAt;

    public User() {
        this.blocked = false;
        this.createdAt = System.currentTimeMillis();
    }

    public User(String username, String passwordHash, String salt, String email) {
        this();
        this.username = username;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public long getCreatedAt() {
        return createdAt;
    }

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

