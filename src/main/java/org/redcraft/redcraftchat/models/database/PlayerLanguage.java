package org.redcraft.redcraftchat.models.database;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.redcraft.redcraftchat.database.DatabaseManager;

@Table(name = "rcbc_player_languages")
public class PlayerLanguage extends DatabaseModel {
    @Id
    @GeneratedValue
    public long id;

    @Column(name = "player_uuid")
    public UUID playerUuid;

    @Column(name = "language_iso")
    public String languageIso;

    @Column(name = "is_main_language")
    public Boolean isMainLanguage;

    public SupportedLanguage getLanguage() {
        return DatabaseManager.getDatabase().where("language_iso=?", this.languageIso).first(SupportedLanguage.class);
    }
}