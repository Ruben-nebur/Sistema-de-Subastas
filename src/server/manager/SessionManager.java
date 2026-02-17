package server.manager;

import server.model.Session;
import server.security.CryptoUtils;
import common.Constants;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gestor de sesiones del sistema.
 * Maneja creación, validación y limpieza de tokens de sesión.
 * Thread-safe mediante ConcurrentHashMap.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class SessionManager {

    /** Almacén de sesiones (token -> Session) */
    private final ConcurrentHashMap<String, Session> sessions;

    /** Índice de sesiones por usuario (username -> token) */
    private final ConcurrentHashMap<String, String> userSessions;

    /** Planificador para limpieza de sesiones expiradas */
    private final ScheduledExecutorService cleanupScheduler;

    /** Intervalo de limpieza en minutos */
    private static final int CLEANUP_INTERVAL_MINUTES = 5;

    /**
     * Constructor del gestor de sesiones.
     */
    public SessionManager() {
        this.sessions = new ConcurrentHashMap<>();
        this.userSessions = new ConcurrentHashMap<>();

        // Programar limpieza periódica de sesiones expiradas
        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SessionCleanup");
            t.setDaemon(true);
            return t;
        });
        this.cleanupScheduler.scheduleAtFixedRate(
            this::cleanupExpiredSessions,
            CLEANUP_INTERVAL_MINUTES,
            CLEANUP_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );

        System.out.println("[SessionManager] Iniciado con limpieza cada " +
            CLEANUP_INTERVAL_MINUTES + " minutos");
    }

    /**
     * Crea una nueva sesión para un usuario.
     * Si el usuario ya tiene una sesión activa, la invalida.
     *
     * @param username nombre de usuario
     * @param role rol del usuario
     * @return nueva sesión creada
     */
    public Session createSession(String username, String role) {
        // Invalidar sesión anterior si existe
        String oldToken = userSessions.get(username);
        if (oldToken != null) {
            sessions.remove(oldToken);
        }

        // Crear nueva sesión
        String token = CryptoUtils.generateToken();
        Session session = new Session(token, username, role);

        // Almacenar
        sessions.put(token, session);
        userSessions.put(username, token);

        System.out.println("[SessionManager] Sesión creada para: " + username);
        return session;
    }

    /**
     * Valida un token de sesión.
     *
     * @param token token a validar
     * @return sesión si es válida, null si no existe o ha expirado
     */
    public Session validateSession(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        Session session = sessions.get(token);
        if (session == null) {
            return null;
        }

        if (session.isExpired()) {
            // Limpiar sesión expirada
            invalidateSession(token);
            return null;
        }

        // Renovar sesión (extender tiempo de expiración)
        session.renew();
        return session;
    }

    /**
     * Invalida una sesión.
     *
     * @param token token de la sesión a invalidar
     * @return true si se invalidó, false si no existía
     */
    public boolean invalidateSession(String token) {
        Session session = sessions.remove(token);
        if (session != null) {
            userSessions.remove(session.getUsername());
            System.out.println("[SessionManager] Sesión invalidada para: " + session.getUsername());
            return true;
        }
        return false;
    }

    /**
     * Invalida la sesión de un usuario por su nombre.
     *
     * @param username nombre de usuario
     * @return true si se invalidó, false si no tenía sesión
     */
    public boolean invalidateUserSession(String username) {
        String token = userSessions.remove(username);
        if (token != null) {
            sessions.remove(token);
            System.out.println("[SessionManager] Sesión de usuario invalidada: " + username);
            return true;
        }
        return false;
    }

    /**
     * Obtiene la sesión de un usuario.
     *
     * @param username nombre de usuario
     * @return sesión o null si no tiene sesión activa
     */
    public Session getSessionByUsername(String username) {
        String token = userSessions.get(username);
        if (token == null) {
            return null;
        }
        Session session = sessions.get(token);
        if (session != null && session.isExpired()) {
            invalidateSession(token);
            return null;
        }
        return session;
    }

    /**
     * Verifica si un usuario tiene sesión activa.
     *
     * @param username nombre de usuario
     * @return true si tiene sesión válida
     */
    public boolean hasActiveSession(String username) {
        return getSessionByUsername(username) != null;
    }

    /**
     * Obtiene el token de sesión de un usuario.
     *
     * @param username nombre de usuario
     * @return token o null si no tiene sesión
     */
    public String getTokenByUsername(String username) {
        Session session = getSessionByUsername(username);
        return session != null ? session.getToken() : null;
    }

    /**
     * Limpia todas las sesiones expiradas.
     */
    private void cleanupExpiredSessions() {
        int removed = 0;
        for (Map.Entry<String, Session> entry : sessions.entrySet()) {
            if (entry.getValue().isExpired()) {
                invalidateSession(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            System.out.println("[SessionManager] Limpiadas " + removed + " sesiones expiradas");
        }
    }

    /**
     * Obtiene el número de sesiones activas.
     *
     * @return cantidad de sesiones
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * Obtiene todas las sesiones activas.
     *
     * @return colección de sesiones
     */
    public Collection<Session> getAllSessions() {
        return sessions.values();
    }

    /**
     * Apaga el gestor de sesiones de forma ordenada.
     */
    public void shutdown() {
        cleanupScheduler.shutdown();
        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        sessions.clear();
        userSessions.clear();
        System.out.println("[SessionManager] Apagado completado");
    }
}
