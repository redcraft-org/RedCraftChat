package org.redcraft.redcraftchat.models.database;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "rcc_player_mails")
public class PlayerMailDatabase extends DatabaseModel {
    @Id
    @GeneratedValue
    public long id;

    @Column(name = "sender_uuid")
    public String senderUuid;

    @Column(name = "recipient_uuid")
    public String recipientUuid;

    @Column(name = "message")
    public String message;

    @Column(name = "original_language")
    public String originalLanguage;

    @Column(name = "sent_at")
    public String sentAt;

    @Column(name = "read_at", nullable = true)
    public String readAt;

}
