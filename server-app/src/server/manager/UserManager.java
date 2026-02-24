package server.manager;

import server.model.User;
import server.persistence.Database;
import server.security.CryptoUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Gestor de usuarios simplificado.
 */
public class UserManager {

    private final ConcurrentHashMap<String, User> users;
    private Database database;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    private static final int MIN_PASSWORD_LENGTH = 3;
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 20;

    public UserManager() {
        this.users = new ConcurrentHashMap<>();
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public void loadFromDatabase() {
        if (database == null) {
            createDefaultUser();
            return;
        }

        List<User> dbUsers = database.getAllUsers();
        for (User user : dbUsers) {
            users.put(user.getUsername().toLowerCase(), user);
        }

        System.out.println("[UserManager] Cargados " + dbUsers.size() + " usuarios desde la BD");

        if (!users.containsKey("admin")) {
            createDefaultUser();
        }
    }

    private void createDefaultUser() {
        if (users.containsKey("admin")) {
            return;
        }
        String salt = CryptoUtils.generateSalt();
        String hash = CryptoUtils.hashPassword("admin123", salt);
        User admin = new User("admin", hash, salt, "admin@netauction.com");
        users.put("admin", admin);

        if (database != null) {
            database.insertUser(admin);
        }

        System.out.println("[UserManager] Usuario por defecto creado: admin (password: admin123)");
    }

    public RegistrationResult register(String username, String password, String email) {
        if (username == null || username.trim().isEmpty()) {
            return new RegistrationResult(false, "El nombre de usuario es obligatorio");
        }
        username = username.trim().toLowerCase();

        if (username.length() < MIN_USERNAME_LENGTH) {
            return new RegistrationResult(false,
                "El nombre de usuario debe tener al menos " + MIN_USERNAME_LENGTH + " caracteres");
        }
        if (username.length() > MAX_USERNAME_LENGTH) {
            return new RegistrationResult(false,
                "El nombre de usuario no puede tener mas de " + MAX_USERNAME_LENGTH + " caracteres");
        }
        if (!username.matches("^[a-z0-9_]+$")) {
            return new RegistrationResult(false,
                "El nombre de usuario solo puede contener letras, numeros y guiones bajos");
        }

        if (users.containsKey(username)) {
            return new RegistrationResult(false, "El nombre de usuario ya esta en uso");
        }

        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return new RegistrationResult(false,
                "La contrasena debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }

        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            return new RegistrationResult(false, "El correo electronico no es valido");
        }

        String salt = CryptoUtils.generateSalt();
        String hash = CryptoUtils.hashPassword(password, salt);
        User user = new User(username, hash, salt, email);

        User existing = users.putIfAbsent(username, user);
        if (existing != null) {
            return new RegistrationResult(false, "El nombre de usuario ya esta en uso");
        }

        if (database != null) {
            database.insertUser(user);
        }

        System.out.println("[UserManager] Usuario registrado: " + username);
        return new RegistrationResult(true, "Usuario registrado correctamente");
    }

    public AuthenticationResult authenticate(String username, String password) {
        if (username == null || password == null) {
            return new AuthenticationResult(false, "Credenciales incompletas", null);
        }

        username = username.trim().toLowerCase();
        User user = users.get(username);

        if (user == null) {
            return new AuthenticationResult(false, "Usuario o contrasena incorrectos", null);
        }

        if (user.isBlocked()) {
            return new AuthenticationResult(false,
                "La cuenta esta bloqueada. Contacte con el administrador.", null);
        }

        if (!CryptoUtils.verifyPassword(password, user.getSalt(), user.getPasswordHash())) {
            return new AuthenticationResult(false, "Usuario o contrasena incorrectos", null);
        }

        System.out.println("[UserManager] Usuario autenticado: " + username);
        return new AuthenticationResult(true, "Autenticacion exitosa", user);
    }

    public User getUser(String username) {
        if (username == null) {
            return null;
        }
        return users.get(username.toLowerCase());
    }

    public boolean userExists(String username) {
        return username != null && users.containsKey(username.toLowerCase());
    }

    public boolean setUserBlocked(String username, boolean blocked) {
        User user = getUser(username);
        if (user == null) {
            return false;
        }
        user.setBlocked(blocked);

        if (database != null) {
            database.updateUser(user);
        }

        System.out.println("[UserManager] Usuario " + username +
            (blocked ? " bloqueado" : " desbloqueado"));
        return true;
    }

    public Collection<User> getAllUsers() {
        return users.values();
    }

    public void saveUser(User user) {
        if (user != null && user.getUsername() != null) {
            users.put(user.getUsername().toLowerCase(), user);
        }
    }

    public int getUserCount() {
        return users.size();
    }

    public static class RegistrationResult {
        private final boolean success;
        private final String message;

        public RegistrationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class AuthenticationResult {
        private final boolean success;
        private final String message;
        private final User user;

        public AuthenticationResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public User getUser() {
            return user;
        }
    }
}

