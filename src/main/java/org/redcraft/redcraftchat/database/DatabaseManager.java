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

        // Probe the database before norm spins up a connection pool,
        // a Hikari pool that fails to start dumps a stack trace in the log
        try (Connection probe = DriverManager.getConnection(Config.databaseUri, Config.databaseUsername, Config.databasePassword)) {
            RedCraftChat.getInstance().getLogger().debug("Database probe succeeded");
        } catch (SQLException ex) {
            RedCraftChat.getInstance().getLogger().warn("Database is unreachable, features that need it will fail until it is back: {}", ex.getMessage());
            return;
        }

        List<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(PlayerPreferencesDatabase.class);
        classes.add(PlayerMailDatabase.class);
        classes.add(ScheduledAnnouncementDatabase.class);
        classes.add(SupportedLocaleDatabase.class);

        if (createStructure(classes)) {
            migrate();
            RedCraftChat.getInstance().getLogger().info("Connected to database!");
        } else {
            RedCraftChat.getInstance().getLogger().warn("Database is unreachable, features that need it will fail until it is back");
        }
    }

    /**
     * Idempotent migrations for live tables. createStructure only ever runs
     * "create table if not exists", so an existing install never picks up a
     * new column from it, and norm writes every mapped field in every UPDATE:
     * a column missing from the table breaks all writes for that model. Each
     * migration must therefore run before the first write after an upgrade.
     */
    private static void migrate() {
        ensureColumn("rcc_player_preferences", "`language_selector_confirmed` tinyint(1) NOT NULL DEFAULT 0");
    }

    private static void ensureColumn(String table, String columnDefinition) {
        try {
            database.sql("ALTER TABLE `" + table + "` ADD COLUMN " + columnDefinition).execute();
            RedCraftChat.getInstance().getLogger().info("Added column to {}: {}", table, columnDefinition);
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            // MySQL says "Duplicate column name", SQLite "duplicate column
            // name", both covered; anything else is a real failure worth
            // shouting about since every preference write depends on it
            if (!message.contains("duplicate column")) {
                RedCraftChat.getInstance().getLogger().error("Could not add column to {}: {}", table, ex.getMessage());
            }
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

    public static Database getDatabase() {
        return database;
    }

}
