package server.manager;

import server.model.User;
import server.persistence.Database;
import server.security.CryptoUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Gestor de usuarios del sistema NetAuction.
 * Administra el registro, autenticacion, bloqueo y persistencia de usuarios.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class UserManager {

    /** Mapa concurrente de usuarios indexado por username en minusculas */
    private final ConcurrentHashMap<String, User> users;

    /** Conexion a la base de datos para persistencia */
    private Database database;

    /** Patron de validacion de email */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    /** Longitud minima de la contrasena */
    private static final int MIN_PASSWORD_LENGTH = 3;

    /** Longitud minima del nombre de usuario */
    private static final int MIN_USERNAME_LENGTH = 3;

    /** Longitud maxima del nombre de usuario */
    private static final int MAX_USERNAME_LENGTH = 20;

    /**
     * Constructor por defecto.
     * Inicializa el mapa de usuarios vacio.
     */
    public UserManager() {
        this.users = new ConcurrentHashMap<>();
    }

    /**
     * Establece la conexion a la base de datos para persistencia.
     *
     * @param database instancia de la base de datos
     */
    public void setDatabase(Database database) {
        this.database = database;
    }

    /**
     * Carga los usuarios desde la base de datos.
     * Si no hay base de datos disponible o no existe el usuario admin, crea el usuario por defecto.
     */
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

    /**
     * Crea el usuario administrador por defecto si no existe.
     */
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

    /**
     * Registra un nuevo usuario en el sistema.
     * Valida el username, password y email antes de crear el usuario.
     *
     * @param username nombre de usuario
     * @param password contrasena en texto plano
     * @param email correo electronico
     * @return resultado del registro con indicador de exito y mensaje
     */
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

    /**
     * Autentica un usuario verificando sus credenciales.
     *
     * @param username nombre de usuario
     * @param password contrasena en texto plano
     * @return resultado de la autenticacion con indicador de exito, mensaje y usuario
     */
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

    /**
     * Obtiene un usuario por su nombre de usuario.
     *
     * @param username nombre de usuario a buscar
     * @return usuario encontrado o null si no existe
     */
    public User getUser(String username) {
        if (username == null) {
            return null;
        }
        return users.get(username.toLowerCase());
    }

    /**
     * Verifica si un usuario existe en el sistema.
     *
     * @param username nombre de usuario a verificar
     * @return true si el usuario existe
     */
    public boolean userExists(String username) {
        return username != null && users.containsKey(username.toLowerCase());
    }

    /**
     * Establece el estado de bloqueo de un usuario.
     *
     * @param username nombre de usuario a bloquear/desbloquear
     * @param blocked true para bloquear, false para desbloquear
     * @return true si el usuario fue encontrado y actualizado
     */
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

    /**
     * Obtiene todos los usuarios registrados.
     *
     * @return coleccion de todos los usuarios
     */
    public Collection<User> getAllUsers() {
        return users.values();
    }

    /**
     * Guarda o actualiza un usuario en el mapa en memoria.
     *
     * @param user usuario a guardar
     */
    public void saveUser(User user) {
        if (user != null && user.getUsername() != null) {
            users.put(user.getUsername().toLowerCase(), user);
        }
    }

    /**
     * Obtiene el numero total de usuarios registrados.
     *
     * @return cantidad de usuarios
     */
    public int getUserCount() {
        return users.size();
    }

    /**
     * Resultado de una operacion de registro de usuario.
     *
     * @author NetAuction Team
     * @version 1.0
     */
    public static class RegistrationResult {

        /** Indica si el registro fue exitoso */
        private final boolean success;

        /** Mensaje descriptivo del resultado */
        private final String message;

        /**
         * Constructor del resultado de registro.
         *
         * @param success true si fue exitoso
         * @param message mensaje descriptivo
         */
        public RegistrationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        /**
         * Indica si el registro fue exitoso.
         *
         * @return true si fue exitoso
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * Obtiene el mensaje descriptivo del resultado.
         *
         * @return mensaje del resultado
         */
        public String getMessage() {
            return message;
        }
    }

    /**
     * Resultado de una operacion de autenticacion de usuario.
     *
     * @author NetAuction Team
     * @version 1.0
     */
    public static class AuthenticationResult {

        /** Indica si la autenticacion fue exitosa */
        private final boolean success;

        /** Mensaje descriptivo del resultado */
        private final String message;

        /** Usuario autenticado o null si fallo */
        private final User user;

        /**
         * Constructor del resultado de autenticacion.
         *
         * @param success true si fue exitoso
         * @param message mensaje descriptivo
         * @param user usuario autenticado o null
         */
        public AuthenticationResult(boolean success, String message, User user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        /**
         * Indica si la autenticacion fue exitosa.
         *
         * @return true si fue exitosa
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * Obtiene el mensaje descriptivo del resultado.
         *
         * @return mensaje del resultado
         */
        public String getMessage() {
            return message;
        }

        /**
         * Obtiene el usuario autenticado.
         *
         * @return usuario autenticado o null si fallo la autenticacion
         */
        public User getUser() {
            return user;
        }
    }
}
