package org.redcraft.redcraftchat.messaging.announcements.providers;

import com.dieselpoint.norm.Database;

import java.util.ArrayList;
import java.util.List;

import org.redcraft.redcraftchat.database.DatabaseManager;
import org.redcraft.redcraftchat.models.database.ScheduledAnnouncementDatabase;

public class DatabaseAnnouncementsProvider implements AnnouncementsProvider {

    Database db;

    public DatabaseAnnouncementsProvider() {
        db = DatabaseManager.getDatabase();
    }

    public List<String> getAnnouncements() {
        return transform(db.where("enabled=?", 1).results(ScheduledAnnouncementDatabase.class));
    }

    public List<String> transform(List<ScheduledAnnouncementDatabase> results) {
        List<String> announcements = new ArrayList<String>();

        for (ScheduledAnnouncementDatabase announcement : results) {
            announcements.add(announcement.message);
        }

        return announcements;
    }
}
