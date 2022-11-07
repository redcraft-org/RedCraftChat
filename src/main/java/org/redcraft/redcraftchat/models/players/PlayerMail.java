package org.redcraft.redcraftchat.models.players;

import java.time.LocalDateTime;
import java.util.UUID;

public class PlayerMail {
    public String internalId;

    public UUID senderUuid;

    public UUID recipientUuid;

    public String message;

    public String originalLanguage;

    public LocalDateTime sentAt;

    public LocalDateTime readAt;

}
