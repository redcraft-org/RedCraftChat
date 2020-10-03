package org.redcraft.redcraftbungeechat.database;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.dieselpoint.norm.Database;

import org.redcraft.redcraftbungeechat.Config;
import org.redcraft.redcraftbungeechat.RedCraftBungeeChat;
import org.redcraft.redcraftbungeechat.models.database.PlayerLanguage;
import org.redcraft.redcraftbungeechat.models.database.PlayerPreferences;
import org.redcraft.redcraftbungeechat.models.database.SupportedLanguage;

public class DatabaseManager {
    private static Database database;

    public static void connect() {
        database = new Database();
        database.setJdbcUrl(getUri());

        database.setUser(Config.databaseUsername);
        database.setPassword(Config.databasePassword);

        List<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(PlayerPreferences.class);
        classes.add(PlayerLanguage.class);
        classes.add(SupportedLanguage.class);
        createStructure(classes);

        RedCraftBungeeChat.getInstance().getLogger().info("Connected to database!");
    }

    public static void createStructure(List<Class<?>> classes) {
        Iterator<Class<?>> it = classes.iterator();
        while (it.hasNext()) {
            Class<?> classToCreate = it.next();
            try {
                database.createTable(classToCreate);
            } catch (Exception ex) {
                // Ignore, table already exists
            }
        }
    }

    public static Database getDatabase() {
        return database;
    }

    private static String getUri() {
        String pluginConfigPath = RedCraftBungeeChat.getInstance().getDataFolder().getAbsolutePath();
        return Config.databaseUri.replace("%plugin_config_path%", pluginConfigPath);
    }

}
