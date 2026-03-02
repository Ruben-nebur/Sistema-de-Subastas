package server;

import server.manager.UserManager;
import server.persistence.Database;

/**
 * Inicializa la base de datos y crea el usuario por defecto si no existe.
 * Utilidad de linea de comandos para preparar la base de datos antes de iniciar el servidor.
 *
 * @author NetAuction Team
 * @version 1.0
 */
public final class DatabaseInit {

    /**
     * Constructor privado para evitar instanciacion.
     */
    private DatabaseInit() {
    }

    /**
     * Punto de entrada principal.
     * Inicializa la base de datos SQLite y carga/crea los usuarios necesarios.
     *
     * @param args argumentos de linea de comandos (no utilizados)
     */
    public static void main(String[] args) {
        Database database = null;
        try {
            database = new Database();
            database.initialize();

            UserManager userManager = new UserManager();
            userManager.setDatabase(database);
            userManager.loadFromDatabase();

            System.out.println("[INITDB] Base de datos inicializada en server-app/data/netauction.db");
        } catch (Exception e) {
            System.err.println("[INITDB] Error: " + e.getMessage());
            System.exit(1);
        } finally {
            if (database != null) {
                database.close();
            }
        }
    }
}
