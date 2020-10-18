package org.redcraft.redcraftchat.models.database;

import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "rcc_player_mails")
public class PlayerMail extends DatabaseModel {
    @Id
    @GeneratedValue
    public long id;

    @Column(name = "sender_uuid")
    public UUID senderUuid;

    @Column(name = "recipient_uuid")
    public UUID recipientUuid;

    @Column(name = "sent_at")
    public LocalDate sent_at;

    @Column(name = "read_at")
    public LocalDate read_at;

}
