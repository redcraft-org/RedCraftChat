package org.redcraft.redcraftchat.models.database;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Table(name = "rcc_player_languages")
public class PlayerLanguage extends DatabaseModel {
    @Transient
    public String sqlCreationQuery = "create table `rcc_player_languages` (`id` bigint(20) NOT NULL AUTO_INCREMENT, `player_uuid` varchar(255) NOT NULL, `language_iso` varchar(255) NOT NULL, `is_main_language` tinyint(1) NOT NULL DEFAULT 0, PRIMARY KEY (`id`), UNIQUE KEY `player_uuid` (`player_uuid`));";

    @Id
    @GeneratedValue
    public long id;

    @Column(name = "player_uuid", nullable = false, unique = true)
    public String playerUniqueId;

    @Column(name = "language_iso", nullable = false)
    public String languageIso;

    @Column(name = "is_main_language", nullable = false)
    public boolean isMainLanguage = false;
}
