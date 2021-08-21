package org.redcraft.redcraftchat.models.database;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "rcc_supported_languages")
public class SupportedLanguage extends DatabaseModel {
    @Id
    @Column(name = "language_iso", unique = true)
    public String languageIso;

    @Column(name = "english_name", unique = true)
    public String englishName;

    @Column(name = "original_name", unique = true)
    public String originalName;
}
