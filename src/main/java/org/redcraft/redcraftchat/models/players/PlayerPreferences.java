package org.redcraft.redcraftchat.models.players;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PlayerPreferences {

    public String internalUuid;

    public String mainLanguage;

    public String email;

    public List<String> languages;

    public UUID minecraftUuid;
    public String lastKnownMinecraftName;
    public String previousKnownMinecraftName;

    public String discordId;
    public String lastKnownDiscordName;
    public String previousKnownDiscordName;

    public boolean commandSpyEnabled;

    public PlayerPreferences() {
        languages = new ArrayList<String>();
    }

    public PlayerPreferences(ProxiedPlayer player) {
        mainLanguage = PlayerPreferencesManager.extractPlayerLanguage(player);

        languages = new ArrayList<String>();
        languages.add(mainLanguage);

        minecraftUuid = player.getUniqueId();
        lastKnownMinecraftName = player.getName();
    }

    public PlayerPreferences(User user) {
        mainLanguage = Config.defaultLocale;

        languages = new ArrayList<String>();
        languages.add(mainLanguage);

        discordId = user.getId();
        lastKnownDiscordName = user.getName();
    }
}
