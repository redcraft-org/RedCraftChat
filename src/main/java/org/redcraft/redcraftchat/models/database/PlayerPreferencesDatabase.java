package org.redcraft.redcraftchat.models.database;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Table(name = "rcc_player_preferences")
public class PlayerPreferencesDatabase extends DatabaseModel {
    @Transient
    public String sqlCreationQuery = "create table `rcc_player_preferences` (`id` bigint(20) NOT NULL AUTO_INCREMENT, `minecraft_uuid` varchar(255) NOT NULL, `last_known_minecraft_name` varchar(255) DEFAULT NULL, `previous_known_minecraft_name` varchar(255) DEFAULT NULL, `discord_id` varchar(255) DEFAULT NULL, `last_known_discord_name` varchar(255) DEFAULT NULL, `previous_known_discord_name` varchar(255) DEFAULT NULL, `languages` text DEFAULT NULL, `main_language` varchar(255) DEFAULT NULL, `command_spy_enabled` tinyint(1) NOT NULL DEFAULT 0, PRIMARY KEY (`id`), UNIQUE KEY `minecraft_uuid` (`minecraft_uuid`), UNIQUE KEY `discord_id` (`discord_id`));";

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

    public PlayerPreferencesDatabase() {
    }

    public PlayerPreferencesDatabase(UUID playerUniqueId) {
        this.playerUniqueId = playerUniqueId.toString();
    }
}
