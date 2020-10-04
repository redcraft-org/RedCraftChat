package org.redcraft.redcraftchat.models.database;

import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

import org.redcraft.redcraftchat.database.DatabaseManager;

@Table(name = "rcc_player_preferences")
public class PlayerPreferences extends DatabaseModel {
    @Id
    @Column(name = "id")
    public UUID uuid;

    @Column(name = "discord_id", unique = true)
    public String discordId;

    @Column(name = "last_known_name")
    public String lastKnownName;

    @Column(name = "previous_known_name")
    public String previousKnownName;

    @Column(name = "command_spy_enabled")
    public Boolean commandSpyEnabled;

    public PlayerPreferences(UUID uuid) {
        this.uuid = uuid;
    }

    public List<PlayerLanguage> getLanguages() {
        return DatabaseManager.getDatabase().where("player_uuid=?", this.uuid).results(PlayerLanguage.class);
    }
}
