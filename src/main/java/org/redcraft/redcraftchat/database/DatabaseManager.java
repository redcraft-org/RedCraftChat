package org.redcraft.redcraftchat.database;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.dieselpoint.norm.Database;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.models.database.PlayerLanguage;
import org.redcraft.redcraftchat.models.database.PlayerMail;
import org.redcraft.redcraftchat.models.database.PlayerPreferences;
import org.redcraft.redcraftchat.models.database.SupportedLanguage;

public class DatabaseManager {
    private static Database database;

    public static void connect() {
        database = new Database();
        database.setJdbcUrl(getUri());

        database.setUser(Config.databaseUsername);
        database.setPassword(Config.databasePassword);

        List<Class<?>> classes = new ArrayList<Class<?>>();
        classes.add(PlayerLanguage.class);
        classes.add(PlayerPreferences.class);
        classes.add(PlayerMail.class);
        classes.add(SupportedLanguage.class);
        createStructure(classes);

        RedCraftChat.getInstance().getLogger().info("Connected to database!");
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
        String pluginConfigPath = RedCraftChat.getInstance().getDataFolder().getAbsolutePath();
        return Config.databaseUri.replace("%plugin_config_path%", pluginConfigPath);
    }

}
