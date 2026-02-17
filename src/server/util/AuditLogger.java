package server.util;

import common.Constants;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Logger de auditoría que registra operaciones del sistema.
 * Escribe logs a fichero y mantiene un buffer en memoria para consulta rápida.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public class AuditLogger {

    /** Ruta del archivo de log */
    private final String logFilePath;

    /** Buffer en memoria de logs recientes */
    private final ConcurrentLinkedDeque<String> recentLogs;

    /** Tamaño máximo del buffer en memoria */
    private static final int MAX_BUFFER_SIZE = 1000;

    /** Formato de fecha para los logs */
    private final SimpleDateFormat dateFormat;

    /** Writer para el archivo de log */
    private PrintWriter fileWriter;

    /** Object para sincronización de escritura */
    private final Object writeLock = new Object();

    /**
     * Constructor del logger de auditoría.
     *
     * @param logFilePath ruta del archivo de log
     */
    public AuditLogger(String logFilePath) {
        this.logFilePath = logFilePath;
        this.recentLogs = new ConcurrentLinkedDeque<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        initializeLogFile();
    }

    /**
     * Constructor con ruta por defecto.
     */
    public AuditLogger() {
        this(Constants.AUDIT_LOG_PATH);
    }

    /**
     * Inicializa el archivo de log.
     */
    private void initializeLogFile() {
        try {
            // Crear directorio si no existe
            Path logPath = Paths.get(logFilePath);
            Path parentDir = logPath.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            // Abrir archivo en modo append
            fileWriter = new PrintWriter(new FileWriter(logFilePath, true), true);

            // Log de inicio
            log("SYSTEM", "SYSTEM", "localhost", "AuditLogger iniciado");

            System.out.println("[AuditLogger] Iniciado. Archivo: " + logFilePath);

        } catch (IOException e) {
            System.err.println("[AuditLogger] Error inicializando archivo de log: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Registra una entrada de log.
     *
     * @param action acción realizada
     * @param user usuario que realizó la acción
     * @param ip dirección IP del usuario
     * @param details detalles adicionales
     */
    public void log(String action, String user, String ip, String details) {
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format("[%s] [%s] [%s] [%s] %s",
            timestamp,
            action != null ? action : "UNKNOWN",
            user != null ? user : "ANONYMOUS",
            ip != null ? ip : "UNKNOWN",
            details != null ? details : ""
        );

        // Añadir al buffer en memoria
        recentLogs.addLast(logEntry);
        while (recentLogs.size() > MAX_BUFFER_SIZE) {
            recentLogs.pollFirst();
        }

        // Escribir a archivo
        writeToFile(logEntry);
    }

    /**
     * Escribe una entrada al archivo de log de forma sincronizada.
     *
     * @param logEntry entrada a escribir
     */
    private void writeToFile(String logEntry) {
        synchronized (writeLock) {
            if (fileWriter != null) {
                try {
                    fileWriter.println(logEntry);
                    fileWriter.flush();
                } catch (Exception e) {
                    System.err.println("[AuditLogger] Error escribiendo log: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Obtiene los logs más recientes del buffer en memoria.
     *
     * @param count número de entradas a obtener
     * @return lista de entradas de log
     */
    public List<String> getRecentLogs(int count) {
        List<String> result = new ArrayList<>();
        Iterator<String> it = recentLogs.descendingIterator();

        while (it.hasNext() && result.size() < count) {
            result.add(it.next());
        }

        return result;
    }

    /**
     * Obtiene todos los logs del buffer en memoria.
     *
     * @return lista de todas las entradas en memoria
     */
    public List<String> getAllRecentLogs() {
        return new ArrayList<>(recentLogs);
    }

    /**
     * Lee logs desde el archivo.
     *
     * @param lines número de líneas a leer desde el final
     * @return lista de entradas de log
     */
    public List<String> readLogsFromFile(int lines) {
        List<String> result = new ArrayList<>();
        try {
            List<String> allLines = Files.readAllLines(Paths.get(logFilePath));
            int start = Math.max(0, allLines.size() - lines);
            for (int i = start; i < allLines.size(); i++) {
                result.add(allLines.get(i));
            }
        } catch (IOException e) {
            System.err.println("[AuditLogger] Error leyendo archivo de log: " + e.getMessage());
        }
        return result;
    }

    /**
     * Busca logs que coincidan con un patrón.
     *
     * @param pattern patrón a buscar
     * @param maxResults máximo de resultados
     * @return lista de entradas que coinciden
     */
    public List<String> searchLogs(String pattern, int maxResults) {
        List<String> result = new ArrayList<>();
        String lowerPattern = pattern.toLowerCase();

        for (String entry : recentLogs) {
            if (entry.toLowerCase().contains(lowerPattern)) {
                result.add(entry);
                if (result.size() >= maxResults) {
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Filtra logs por usuario.
     *
     * @param username nombre de usuario
     * @param maxResults máximo de resultados
     * @return lista de entradas del usuario
     */
    public List<String> getLogsByUser(String username, int maxResults) {
        return searchLogs("[" + username + "]", maxResults);
    }

    /**
     * Filtra logs por acción.
     *
     * @param action tipo de acción
     * @param maxResults máximo de resultados
     * @return lista de entradas de la acción
     */
    public List<String> getLogsByAction(String action, int maxResults) {
        return searchLogs("[" + action + "]", maxResults);
    }

    /**
     * Obtiene el número de entradas en el buffer.
     *
     * @return cantidad de entradas
     */
    public int getBufferSize() {
        return recentLogs.size();
    }

    /**
     * Cierra el logger y libera recursos.
     */
    public void close() {
        log("SYSTEM", "SYSTEM", "localhost", "AuditLogger cerrando");

        synchronized (writeLock) {
            if (fileWriter != null) {
                fileWriter.close();
                fileWriter = null;
            }
        }

        System.out.println("[AuditLogger] Cerrado");
    }
}
