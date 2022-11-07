package org.redcraft.redcraftchat.messaging.announcements.providers;

import java.net.http.HttpClient;
import java.util.List;

public class RedCraftApiAnnouncementsProvider implements AnnouncementsProvider {

    HttpClient httpClient;

    public RedCraftApiAnnouncementsProvider() {
        httpClient = HttpClient.newHttpClient();
    }

    public List<String> getAnnouncements() {
        // TODO
        return null;
    }
}
