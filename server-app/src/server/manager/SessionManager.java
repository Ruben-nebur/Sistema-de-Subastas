package server.manager;

import server.model.Session;
import server.security.CryptoUtils;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de sesiones simplificado: sin expiracion automatica.
 */
public class SessionManager {

    private final ConcurrentHashMap<String, Session> sessions;
    private final ConcurrentHashMap<String, String> userSessions;

    public SessionManager() {
        this.sessions = new ConcurrentHashMap<>();
        this.userSessions = new ConcurrentHashMap<>();
        System.out.println("[SessionManager] Iniciado");
    }

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

    public Session validateSession(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return sessions.get(token);
    }

    public boolean invalidateSession(String token) {
        Session session = sessions.remove(token);
        if (session != null) {
            userSessions.remove(session.getUsername());
            System.out.println("[SessionManager] Sesion invalidada para: " + session.getUsername());
            return true;
        }
        return false;
    }

    public boolean invalidateUserSession(String username) {
        String token = userSessions.remove(username);
        if (token != null) {
            sessions.remove(token);
            System.out.println("[SessionManager] Sesion de usuario invalidada: " + username);
            return true;
        }
        return false;
    }

    public Session getSessionByUsername(String username) {
        String token = userSessions.get(username);
        if (token == null) {
            return null;
        }
        return sessions.get(token);
    }

    public boolean hasActiveSession(String username) {
        return getSessionByUsername(username) != null;
    }

    public String getTokenByUsername(String username) {
        Session session = getSessionByUsername(username);
        return session != null ? session.getToken() : null;
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    public Collection<Session> getAllSessions() {
        return sessions.values();
    }

    public void shutdown() {
        sessions.clear();
        userSessions.clear();
        System.out.println("[SessionManager] Apagado completado");
    }
}

