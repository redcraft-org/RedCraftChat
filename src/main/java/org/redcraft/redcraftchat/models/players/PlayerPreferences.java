package org.redcraft.redcraftchat.models.players;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.velocitypowered.api.proxy.Player;

import net.dv8tion.jda.api.entities.User;

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

    /**
     * True once the player has explicitly chosen or confirmed a language in
     * any selector. Auto-detection at account creation never sets it, which
     * is what makes the first-join prompt reach everyone exactly once.
     */
    public boolean languageSelectorConfirmed;

    public boolean commandSpyEnabled;

    /** Where to offer sending them back to. Null until they have moved once. */
    public String lastServer;

    /** Where to send them on login. See LoginServer for what the values mean. */
    public String loginServer;

    public PlayerPreferences() {
        languages = new ArrayList<String>();
    }

    public PlayerPreferences(Player player) {
        mainLanguage = PlayerPreferencesManager.extractPlayerLanguage(player);

        languages = new ArrayList<String>();
        languages.add(mainLanguage);

        minecraftUuid = player.getUniqueId();
        lastKnownMinecraftName = player.getUsername();
    }

    public PlayerPreferences(User user) {
        mainLanguage = Config.defaultLocale;

        languages = new ArrayList<String>();
        languages.add(mainLanguage);

        discordId = user.getId();
        lastKnownDiscordName = user.getName();
    }
}
