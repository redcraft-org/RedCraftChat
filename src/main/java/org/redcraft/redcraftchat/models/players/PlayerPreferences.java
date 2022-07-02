package org.redcraft.redcraftchat.models.players;

import java.util.List;
import java.util.UUID;

public class PlayerPreferences {

    public String mainLanguage;

    public String email;

    public List<String> languages;

    public UUID minecraftUuid;
    public String lastKnownMinecraftName;
    public String previousKnownMinecraftName;

    public long discordId;
    public String lastKnownDiscordName;
    public String previousKnownDiscordName;

    public boolean commandSpyEnabled;

    public PlayerPreferences() {
    }

    public PlayerPreferences(UUID minecraftUuid) {
        this.minecraftUuid = minecraftUuid;
    }
}
