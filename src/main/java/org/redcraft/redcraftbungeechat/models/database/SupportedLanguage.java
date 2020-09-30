package org.redcraft.redcraftbungeechat.models.database;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "rcbc_supported_languages")
public class SupportedLanguage extends DatabaseModel {
    @Id
    @Column(name = "language_iso")
    public String languageIso;

    @Column(name = "english_name")
    public String englishName;

    @Column(name = "original_name")
    public String originalName;
}