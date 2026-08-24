package org.redcraft.redcraftchat.api;

import java.util.List;
import java.util.UUID;

/**
 * A player as the json api reports them.
 *
 * uuid, name and displayName are what the BungeeCord version served and the
 * website already reads them. languages is new, the plugin knows which
 * languages a player speaks so it may as well say so.
 */
public class PlayerInfo {

    public UUID uuid;
    public String name;
    public String displayName;
    public String mainLanguage;
    public List<String> languages;

    public PlayerInfo(UUID uuid, String name, String displayName, String mainLanguage, List<String> languages) {
        this.uuid = uuid;
        this.name = name;
        this.displayName = displayName;
        this.mainLanguage = mainLanguage;
        this.languages = languages;
    }
}
