package server.manager;

import server.model.Session;
import server.security.CryptoUtils;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de sesiones del sistema NetAuction.
 * Administra la creacion, validacion e invalidacion de sesiones de usuario.
 * Implementacion simplificada sin expiracion automatica.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class SessionManager {

    /** Mapa concurrente de sesiones indexado por token */
    private final ConcurrentHashMap<String, Session> sessions;

    /** Mapa concurrente de tokens indexado por username */
    private final ConcurrentHashMap<String, String> userSessions;

    /**
     * Constructor por defecto.
     * Inicializa los mapas de sesiones vacios.
     */
    public SessionManager() {
        this.sessions = new ConcurrentHashMap<>();
        this.userSessions = new ConcurrentHashMap<>();
        System.out.println("[SessionManager] Iniciado");
    }

    /**
     * Crea una nueva sesion para un usuario.
     * Si el usuario ya tenia una sesion activa, la invalida primero.
     *
     * @param username nombre del usuario
     * @return nueva sesion creada
     */
    public Session createSession(String username) {
        String oldToken = userSessions.get(username);
        if (oldToken != null) {
            sessions.remove(oldToken);
        }

        String token = CryptoUtils.generateToken();
        Session session = new Session(token, username);

        sessions.put(token, session);
        userSessions.put(username, token);

        System.out.println("[SessionManager] Sesion creada para: " + username);
        return session;
    }

    /**
     * Valida un token de sesion.
     *
     * @param token token a validar
     * @return sesion asociada al token o null si no es valido
     */
    public Session validateSession(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return sessions.get(token);
    }

    /**
     * Invalida una sesion por su token.
     *
     * @param token token de la sesion a invalidar
     * @return true si la sesion existia y fue invalidada
     */
    public boolean invalidateSession(String token) {
        Session session = sessions.remove(token);
        if (session != null) {
            userSessions.remove(session.getUsername());
            System.out.println("[SessionManager] Sesion invalidada para: " + session.getUsername());
            return true;
        }
        return false;
    }

    /**
     * Invalida la sesion activa de un usuario por su nombre de usuario.
     *
     * @param username nombre del usuario cuya sesion se invalidara
     * @return true si el usuario tenia una sesion activa y fue invalidada
     */
    public boolean invalidateUserSession(String username) {
        String token = userSessions.remove(username);
        if (token != null) {
            sessions.remove(token);
            System.out.println("[SessionManager] Sesion de usuario invalidada: " + username);
            return true;
        }
        return false;
    }

    /**
     * Obtiene la sesion activa de un usuario por su nombre de usuario.
     *
     * @param username nombre del usuario
     * @return sesion activa o null si no tiene sesion
     */
    public Session getSessionByUsername(String username) {
        String token = userSessions.get(username);
        if (token == null) {
            return null;
        }
        return sessions.get(token);
    }

    /**
     * Verifica si un usuario tiene una sesion activa.
     *
     * @param username nombre del usuario
     * @return true si tiene una sesion activa
     */
    public boolean hasActiveSession(String username) {
        return getSessionByUsername(username) != null;
    }

    /**
     * Obtiene el token de sesion de un usuario.
     *
     * @param username nombre del usuario
     * @return token de sesion o null si no tiene sesion activa
     */
    public String getTokenByUsername(String username) {
        Session session = getSessionByUsername(username);
        return session != null ? session.getToken() : null;
    }

    /**
     * Obtiene el numero de sesiones activas.
     *
     * @return cantidad de sesiones activas
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Obtiene todas las sesiones activas.
     *
     * @return coleccion de sesiones activas
     */
    public Collection<Session> getAllSessions() {
        return sessions.values();
    }

    /**
     * Apaga el gestor de sesiones limpiando todas las sesiones activas.
     */
    public void shutdown() {
        sessions.clear();
        userSessions.clear();
        System.out.println("[SessionManager] Apagado completado");
    }
}
