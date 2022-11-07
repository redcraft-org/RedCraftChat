package org.redcraft.redcraftchat.models.database;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

@Table(name = "rcc_scheduled_announcements")
public class ScheduledAnnouncementDatabase extends DatabaseModel {
    @Transient
    public String sqlCreationQuery = "create table `rcc_scheduled_announcements` (`id` bigint(20) NOT NULL AUTO_INCREMENT, `message` varchar(255) NOT NULL, `enabled` tinyint(1) NOT NULL DEFAULT 1, PRIMARY KEY (`id`));";

    @Id
    @GeneratedValue
    public long id;

    @Column(name = "message")
    public String message;

    @Column(name = "enabled")
    public boolean enabled;

}
