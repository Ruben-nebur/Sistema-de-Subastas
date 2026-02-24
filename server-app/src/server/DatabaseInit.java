package server;

import server.manager.UserManager;
import server.persistence.Database;

/**
 * Inicializa la base de datos y crea el usuario por defecto si no existe.
 */
public final class DatabaseInit {

    private DatabaseInit() {
    }

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
