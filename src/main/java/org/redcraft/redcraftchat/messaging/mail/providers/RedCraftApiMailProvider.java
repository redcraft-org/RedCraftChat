package org.redcraft.redcraftchat.messaging.mail.providers;

import java.net.http.HttpClient;
import java.util.List;
import java.util.UUID;

import org.redcraft.redcraftchat.models.players.PlayerMail;

public class RedCraftApiMailProvider implements MailProvider {

    HttpClient httpClient;

    public RedCraftApiMailProvider() {
        httpClient = HttpClient.newHttpClient();
    }

    public List<PlayerMail> getMails(UUID recipientUuid, boolean unreadOnly) {
        // TODO
        return null;
    }

    public void sendMail(PlayerMail mail) {
        // TODO
    }

    public void updateMail(PlayerMail mail) {
        // TODO
    }
}
