package server.manager;

import server.model.User;
import server.persistence.Database;
import server.security.CryptoUtils;
import common.Constants;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Gestor de usuarios del sistema.
 * Maneja registro, autenticación y gestión de cuentas.
 * Thread-safe mediante ConcurrentHashMap.
 * Soporta persistencia opcional con SQLite.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class UserManager {

    /** Almacén de usuarios (username -> User) */
    private final ConcurrentHashMap<String, User> users;

    /** Base de datos para persistencia (opcional) */
    private Database database;

    /** Patrón para validar email */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    /** Longitud mínima de contraseña */
    private static final int MIN_PASSWORD_LENGTH = 3;

    /** Longitud mínima de username */
    private static final int MIN_USERNAME_LENGTH = 3;

    /** Longitud máxima de username */
    private static final int MAX_USERNAME_LENGTH = 20;

    /**
     * Constructor del gestor de usuarios.
     */
    public UserManager() {
        this.users = new ConcurrentHashMap<>();
    }

    /**
     * Establece la base de datos para persistencia.
     *
     * @param database instancia de base de datos
     */
    public void setDatabase(Database database) {
        this.database = database;
    }

    /**
     * Carga usuarios desde la base de datos.
     */
    public void loadFromDatabase() {
        if (database == null) {
            createDefaultAdmin();
            return;
        }

        List<User> dbUsers = database.getAllUsers();
        for (User user : dbUsers) {
            users.put(user.getUsername().toLowerCase(), user);
        }

        System.out.println("[UserManager] Cargados " + dbUsers.size() + " usuarios desde la BD");

        // Crear admin por defecto si no existe
        if (!users.containsKey("admin")) {
            createDefaultAdmin();
        }
    }

    /**
     * Crea un usuario administrador por defecto.
     */
    private void createDefaultAdmin() {
        if (users.containsKey("admin")) {
            return;
        }
        String salt = CryptoUtils.generateSalt();
        String hash = CryptoUtils.hashPassword("admin123", salt);
        User admin = new User("admin", hash, salt, "admin@netauction.com");
        admin.setRole(Constants.ROLE_ADMIN);
        users.put("admin", admin);

        // Persistir si hay BD
        if (database != null) {
            database.insertUser(admin);
        }

        System.out.println("[UserManager] Usuario admin creado por defecto (password: admin123)");
    }

    /**
     * Registra un nuevo usuario.
     *
     * @param username nombre de usuario
     * @param password contraseña en texto plano
     * @param email correo electrónico
     * @return resultado del registro
     */
    public RegistrationResult register(String username, String password, String email) {
        // Validar username
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
                "El nombre de usuario no puede tener más de " + MAX_USERNAME_LENGTH + " caracteres");
        }
        if (!username.matches("^[a-z0-9_]+$")) {
            return new RegistrationResult(false,
                "El nombre de usuario solo puede contener letras, números y guiones bajos");
        }

        // Verificar si ya existe
        if (users.containsKey(username)) {
            return new RegistrationResult(false, "El nombre de usuario ya está en uso");
        }

        // Validar contraseña
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return new RegistrationResult(false,
                "La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }

        // Validar email
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            return new RegistrationResult(false, "El correo electrónico no es válido");
        }

        // Crear usuario
        String salt = CryptoUtils.generateSalt();
        String hash = CryptoUtils.hashPassword(password, salt);
        User user = new User(username, hash, salt, email);

        // Guardar (usar putIfAbsent para evitar condiciones de carrera)
        User existing = users.putIfAbsent(username, user);
        if (existing != null) {
            return new RegistrationResult(false, "El nombre de usuario ya está en uso");
        }

        // Persistir en BD si está disponible
        if (database != null) {
            database.insertUser(user);
        }

        System.out.println("[UserManager] Usuario registrado: " + username);
        return new RegistrationResult(true, "Usuario registrado correctamente");
    }

    /**
     * Autentica un usuario.
     *
     * @param username nombre de usuario
     * @param password contraseña en texto plano
     * @return resultado de la autenticación
     */
    public AuthenticationResult authenticate(String username, String password) {
        if (username == null || password == null) {
            return new AuthenticationResult(false, "Credenciales incompletas", null);
        }

        username = username.trim().toLowerCase();
        User user = users.get(username);

        if (user == null) {
            return new AuthenticationResult(false, "Usuario o contraseña incorrectos", null);
        }

        // Verificar si está bloqueado permanentemente
        if (user.isBlocked()) {
            return new AuthenticationResult(false,
                "La cuenta está bloqueada. Contacte con el administrador.", null);
        }

        // Verificar bloqueo temporal
        if (user.isTemporarilyLocked()) {
            long seconds = user.getRemainingLockSeconds();
            return new AuthenticationResult(false,
                "Cuenta bloqueada temporalmente. Espere " + seconds + " segundos.", null);
        }

        // Verificar contraseña
        if (!CryptoUtils.verifyPassword(password, user.getSalt(), user.getPasswordHash())) {
            user.incrementFailedAttempts();

            int remaining = Constants.MAX_LOGIN_ATTEMPTS - user.getFailedAttempts();
            if (remaining > 0) {
                return new AuthenticationResult(false,
                    "Contraseña incorrecta. Intentos restantes: " + remaining, null);
            } else {
                return new AuthenticationResult(false,
                    "Cuenta bloqueada temporalmente por demasiados intentos fallidos.", null);
            }
        }

        // Login exitoso
        user.resetFailedAttempts();
        System.out.println("[UserManager] Usuario autenticado: " + username);
        return new AuthenticationResult(true, "Autenticación exitosa", user);
    }

    /**
     * Obtiene un usuario por su nombre.
     *
     * @param username nombre de usuario
     * @return usuario o null si no existe
     */
    public User getUser(String username) {
        if (username == null) {
            return null;
        }
        return users.get(username.toLowerCase());
    }

    /**
     * Verifica si un usuario existe.
     *
     * @param username nombre de usuario
     * @return true si existe
     */
    public boolean userExists(String username) {
        return username != null && users.containsKey(username.toLowerCase());
    }

    /**
     * Bloquea o desbloquea un usuario.
     *
     * @param username nombre de usuario
     * @param blocked true para bloquear, false para desbloquear
     * @return true si se realizó el cambio
     */
    public boolean setUserBlocked(String username, boolean blocked) {
        User user = getUser(username);
        if (user == null) {
            return false;
        }
        user.setBlocked(blocked);
        if (!blocked) {
            user.resetFailedAttempts();
        }

        // Persistir en BD si está disponible
        if (database != null) {
            database.updateUser(user);
        }

        System.out.println("[UserManager] Usuario " + username +
            (blocked ? " bloqueado" : " desbloqueado"));
        return true;
    }

    /**
     * Obtiene todos los usuarios.
     *
     * @return colección de usuarios
     */
    public Collection<User> getAllUsers() {
        return users.values();
    }

    /**
     * Guarda un usuario (para persistencia).
     *
     * @param user usuario a guardar
     */
    public void saveUser(User user) {
        if (user != null && user.getUsername() != null) {
            users.put(user.getUsername().toLowerCase(), user);
        }
    }

    /**
     * Obtiene el número total de usuarios.
     *
     * @return cantidad de usuarios
     */
    public int getUserCount() {
        return users.size();
    }

    // ==================== CLASES DE RESULTADO ====================

    /**
     * Resultado de una operación de registro.
     */
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

    /**
     * Resultado de una operación de autenticación.
     */
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
