package org.redcraft.redcraftchat.models.database;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Table(name = "rcc_player_preferences")
public class PlayerPreferencesDatabase extends DatabaseModel {
    @Transient
    public String sqlCreationQuery = "create table `rcc_player_preferences` (`id` bigint(20) NOT NULL AUTO_INCREMENT, `minecraft_uuid` varchar(255) DEFAULT NULL, `last_known_minecraft_name` varchar(255) DEFAULT NULL, `previous_known_minecraft_name` varchar(255) DEFAULT NULL, `discord_id` varchar(255) DEFAULT NULL, `last_known_discord_name` varchar(255) DEFAULT NULL, `previous_known_discord_name` varchar(255) DEFAULT NULL, `languages` text DEFAULT NULL, `main_language` varchar(255) DEFAULT NULL, `command_spy_enabled` tinyint(1) NOT NULL DEFAULT 0, `language_selector_confirmed` tinyint(1) NOT NULL DEFAULT 0, `last_server` varchar(255) DEFAULT NULL, `login_server` varchar(255) DEFAULT NULL, PRIMARY KEY (`id`), UNIQUE KEY `minecraft_uuid` (`minecraft_uuid`), UNIQUE KEY `discord_id` (`discord_id`));";

    @Id
    @GeneratedValue
    public long id;

    @Column(name = "minecraft_uuid", nullable = true, unique = true)
    public String minecraftUuid;
    @Column(name = "last_known_minecraft_name")
    public String lastKnownMinecraftName;
    @Column(name = "previous_known_minecraft_name")
    public String previousKnownMinecraftName;

    @Column(name = "discord_id", nullable = true, unique = true)
    public String discordId;
    @Column(name = "last_known_discord_name")
    public String lastKnownDiscordName;
    @Column(name = "previous_known_discord_name")
    public String previousKnownDiscordName;

    @Column(name = "languages")
    public String languages;

    @Column(name = "main_language")
    public String mainLanguage;

    @Column(name = "command_spy_enabled", nullable = false)
    public boolean commandSpyEnabled = false;

    @Column(name = "language_selector_confirmed", nullable = false)
    public boolean languageSelectorConfirmed = false;

    /**
     * The last backend server this player was on that is not the one they are
     * on now, so the selector can offer to send them back to it.
     */
    @Column(name = "last_server")
    public String lastServer;

    /**
     * Where to put this player when they log in. Null means wherever the
     * proxy would have sent them, a sentinel means whichever server they were
     * on last, and anything else is a server id.
     */
    @Column(name = "login_server")
    public String loginServer;
}
