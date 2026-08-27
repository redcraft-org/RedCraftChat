package org.redcraft.redcraftchat.database;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.dieselpoint.norm.Database;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.models.database.PlayerMailDatabase;
import org.redcraft.redcraftchat.models.database.PlayerPreferencesDatabase;
import org.redcraft.redcraftchat.models.database.ScheduledAnnouncementDatabase;
import org.redcraft.redcraftchat.models.database.SupportedLocaleDatabase;

public class DatabaseManager {
    private static Database database;

    // The tables exist and every migration has run. Until then no write is
    // safe: norm puts every mapped field in every UPDATE, so a table that
    // predates a column rejects the whole statement.
    private static volatile boolean schemaReady = false;
    private static long nextSchemaAttempt = 0;

    private static final long SCHEMA_RETRY_INTERVAL_MILLIS = 30_000;

    private DatabaseManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static void connect() {
        try {
            // Velocity does not bundle a JDBC driver and the plugin class loader
            // is not scanned by DriverManager, register the shaded driver by hand
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            RedCraftChat.getInstance().getLogger().warn("MySQL driver not found: {}", ex.getMessage());
        }

        database = new Database();
        database.setJdbcUrl(Config.databaseUri);

        database.setUser(Config.databaseUsername);
        database.setPassword(Config.databasePassword);

        schemaReady = false;
        nextSchemaAttempt = 0;

        if (!ensureSchema()) {
            RedCraftChat.getInstance().getLogger().warn("Database is unreachable, features that need it will fail until it is back");
        }
    }

    /**
     * Creates the tables and runs the migrations, retrying on later calls if
     * the database is down right now. Boot cannot be the only chance: a proxy
     * that starts while MySQL is restarting would otherwise stay unmigrated
     * until somebody restarts it, and every preference write would fail for
     * as long as it ran.
     */
    private static boolean ensureSchema() {
        if (schemaReady) {
            return true;
        }
        synchronized (DatabaseManager.class) {
            if (schemaReady) {
                return true;
            }
            if (database == null) {
                return false;
            }
            // Every failed attempt costs a connection and a log line, so a
            // database that stays down is retried on a timer rather than on
            // every query
            long now = System.currentTimeMillis();
            if (now < nextSchemaAttempt) {
                return false;
            }
            nextSchemaAttempt = now + SCHEMA_RETRY_INTERVAL_MILLIS;

            // Probe the database before norm spins up a connection pool,
            // a Hikari pool that fails to start dumps a stack trace in the log
            try (Connection probe = DriverManager.getConnection(Config.databaseUri, Config.databaseUsername, Config.databasePassword)) {
                RedCraftChat.getInstance().getLogger().debug("Database probe succeeded");
            } catch (SQLException ex) {
                RedCraftChat.getInstance().getLogger().warn("Database is unreachable, features that need it will fail until it is back: {}", ex.getMessage());
                return false;
            }

            List<Class<?>> classes = new ArrayList<Class<?>>();
            classes.add(PlayerPreferencesDatabase.class);
            classes.add(PlayerMailDatabase.class);
            classes.add(ScheduledAnnouncementDatabase.class);
            classes.add(SupportedLocaleDatabase.class);

            if (!createStructure(classes) || !migrate()) {
                return false;
            }

            schemaReady = true;
            RedCraftChat.getInstance().getLogger().info("Connected to database!");
            return true;
        }
    }

    /**
     * Idempotent migrations for live tables. createStructure only ever runs
     * "create table if not exists", so an existing install never picks up a
     * new column from it. A migration that fails leaves the schema not ready,
     * which keeps the retry timer running instead of letting writes through
     * against a table norm cannot satisfy.
     */
    private static boolean migrate() {
        return ensureColumn("rcc_player_preferences", "`language_selector_confirmed` tinyint(1) NOT NULL DEFAULT 0");
    }

    private static boolean ensureColumn(String table, String columnDefinition) {
        try {
            database.sql("ALTER TABLE `" + table + "` ADD COLUMN " + columnDefinition).execute();
            RedCraftChat.getInstance().getLogger().info("Added column to {}: {}", table, columnDefinition);
            return true;
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            // MySQL says "Duplicate column name", SQLite "duplicate column
            // name", both covered: the column is already there, which is
            // exactly what the migration wanted
            if (message.contains("duplicate column")) {
                return true;
            }
            RedCraftChat.getInstance().getLogger().error("Could not add column to {}: {}", table, ex.getMessage());
            return false;
        }
    }

    public static boolean createStructure(List<Class<?>> classes) {
        Iterator<Class<?>> it = classes.iterator();
        while (it.hasNext()) {
            Class<?> classToCreate = it.next();
            String sqlCreationQuery = null;
            try {
                // Try to access custom table SQL creation if exists
                Constructor<?> ctor = classToCreate.getConstructor();
                Object object = ctor.newInstance(new Object[] {});
                sqlCreationQuery = (String) classToCreate.getDeclaredField("sqlCreationQuery").get(object);
            } catch (Exception ex) {
                // Auto generate the query if missing
                sqlCreationQuery = database.getSqlMaker().getCreateTableSql(classToCreate);
            }

            // Patch to avoid exceptions
            sqlCreationQuery = sqlCreationQuery.replace("create table", "create table if not exists");

            try {
                database.sql(sqlCreationQuery).execute();
            } catch (Exception ex) {
                RedCraftChat.getInstance().getLogger().warn("Could not create table for {}: {}", classToCreate.getSimpleName(), ex.getMessage());
                return false;
            }
        }
        return true;
    }

    /**
     * The database handle, with the schema brought up to date first if an
     * earlier attempt could not reach the server. Callers must fetch it per
     * use rather than cache it, otherwise the first caller to run before the
     * database came back would keep querying an unmigrated schema.
     */
    public static Database getDatabase() {
        ensureSchema();
        return database;
    }

}
