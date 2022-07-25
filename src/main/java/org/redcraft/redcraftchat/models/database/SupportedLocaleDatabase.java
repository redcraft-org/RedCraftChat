package org.redcraft.redcraftchat.models.database;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "rcc_supported_locale")
public class SupportedLocaleDatabase extends DatabaseModel {
    @Id
    @GeneratedValue
    public long id;

    @Column(name = "code")
    public String code;

    @Column(name = "name")
    public String name;

}
