package org.redcraft.redcraftchat.messaging.mail.providers;

import java.util.List;
import java.util.UUID;

import org.redcraft.redcraftchat.models.players.PlayerMail;

public interface MailProvider {

    List<PlayerMail> getMails(UUID uniqueId, boolean unreadOnly);
    public void sendMail(PlayerMail mail);
    public void updateMail(PlayerMail mail);
}
