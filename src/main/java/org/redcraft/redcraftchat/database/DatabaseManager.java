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
            RedCraftChat.getInstance().getLogger().info("Connected to database!");
        } else {
            RedCraftChat.getInstance().getLogger().warn("Database is unreachable, features that need it will fail until it is back");
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
