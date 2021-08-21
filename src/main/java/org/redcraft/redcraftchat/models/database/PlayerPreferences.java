package org.redcraft.redcraftchat.models.database;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.redcraft.redcraftchat.database.DatabaseManager;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

@Table(name = "rcc_player_preferences")
public class PlayerPreferences extends DatabaseModel {
    @Transient
    public String sqlCreationQuery = "create table `rcc_player_preferences` (`id` bigint(20) NOT NULL AUTO_INCREMENT,`player_uuid` varchar(255) DEFAULT NULL,`discord_id` varchar(255) DEFAULT NULL,`last_known_name` varchar(255) DEFAULT NULL,`previous_known_name` varchar(255) DEFAULT NULL,`command_spy_enabled` tinyint(1) NOT NULL DEFAULT 0, PRIMARY KEY (`id`), UNIQUE KEY `player_uuid` (`player_uuid`), UNIQUE KEY `discord_id` (`discord_id`));";

    @Id
    @GeneratedValue
    public long id;

    @Column(name = "player_uuid", unique = true)
    public String playerUniqueId;

    @Column(name = "discord_id", unique = true)
    public String discordId;

    @Column(name = "last_known_name")
    public String lastKnownName;

    @Column(name = "previous_known_name")
    public String previousKnownName;

    @Column(name = "command_spy_enabled", nullable = false)
    public Boolean commandSpyEnabled = false;

    public PlayerPreferences() {
    }

    public PlayerPreferences(UUID playerUniqueId) {
        this.playerUniqueId = playerUniqueId.toString();
    }

    @Transient
    public ProxiedPlayer getPlayer() {
        return ProxyServer.getInstance().getPlayer(UUID.fromString(this.playerUniqueId));
    }

    @Transient
    public List<PlayerLanguage> languages() {
        if (this.playerUniqueId != null) {
            return DatabaseManager.getDatabase().where("player_uuid=?", this.playerUniqueId).results(PlayerLanguage.class);
        }

        return new ArrayList<PlayerLanguage>();
    }
}
