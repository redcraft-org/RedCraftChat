package org.redcraft.redcraftchat.messaging.mail.providers;

import com.dieselpoint.norm.Database;
import com.dieselpoint.norm.Query;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.redcraft.redcraftchat.database.DatabaseManager;
import org.redcraft.redcraftchat.models.database.PlayerMailDatabase;
import org.redcraft.redcraftchat.models.players.PlayerMail;

public class DatabaseMailProvider implements MailProvider {

    Database db;

    public DatabaseMailProvider() {
        db = DatabaseManager.getDatabase();
    }

    public List<PlayerMail> getMails(UUID recipientUuid, boolean unreadOnly) {
        Query query = db.where("recipient_uuid=?", recipientUuid.toString());
        if (unreadOnly) {
            query = query.where("read_at is null");
        }
        return transform(query.results(PlayerMailDatabase.class));
    }

    public void sendMail(PlayerMail mail) {
        PlayerMailDatabase mailToSend = transformToDatabase(mail);
        try {
            db.insert(mailToSend);
        } catch (Exception e) {
            if (!e.getMessage().contains("Could not set value into pojo.")) {
                throw e;
            }
        }
    }

    public void updateMail(PlayerMail mail) {
        db.update(transformToDatabase(mail));
    }

    public List<PlayerMail> transform(List<PlayerMailDatabase> mailDatabase) {
        List<PlayerMail> mail = new ArrayList<PlayerMail>();

        for (PlayerMailDatabase mailDatabaseEntry : mailDatabase) {
            mail.add(transform(mailDatabaseEntry));
        }

        return mail;
    }

    public PlayerMail transform(PlayerMailDatabase mailDatabase) {
        PlayerMail playerMail = new PlayerMail();

        playerMail.internalId = mailDatabase.id == 0 ? null : String.valueOf(mailDatabase.id);
        playerMail.senderUuid = mailDatabase.senderUuid == null ? null : UUID.fromString(mailDatabase.senderUuid);
        playerMail.recipientUuid = mailDatabase.recipientUuid == null ? null : UUID.fromString(mailDatabase.recipientUuid);
        playerMail.message = mailDatabase.message;
        playerMail.originalLanguage = mailDatabase.originalLanguage;
        playerMail.sentAt = mailDatabase.sentAt == null ? null : LocalDateTime.parse(mailDatabase.sentAt);
        playerMail.readAt = mailDatabase.readAt == null ? null : LocalDateTime.parse(mailDatabase.readAt);

        return playerMail;
    }

    public PlayerMailDatabase transformToDatabase(PlayerMail playerMail) {
        PlayerMailDatabase mailDatabase = new PlayerMailDatabase();

        mailDatabase.id = playerMail.internalId == null ? 0 : Long.parseLong(playerMail.internalId);
        mailDatabase.senderUuid = playerMail.senderUuid == null ? null : playerMail.senderUuid.toString();
        mailDatabase.recipientUuid = playerMail.recipientUuid == null ? null : playerMail.recipientUuid.toString();
        mailDatabase.message = playerMail.message;
        mailDatabase.originalLanguage = playerMail.originalLanguage;
        mailDatabase.sentAt = playerMail.sentAt == null ? null : playerMail.sentAt.toString();
        mailDatabase.readAt = playerMail.readAt == null ? null : playerMail.readAt.toString();

        return mailDatabase;
    }
}
