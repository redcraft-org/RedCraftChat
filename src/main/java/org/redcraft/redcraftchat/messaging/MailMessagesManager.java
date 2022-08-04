package org.redcraft.redcraftchat.messaging;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.detection.DetectionManager;
import org.redcraft.redcraftchat.messaging.mail.providers.DatabaseMailProvider;
import org.redcraft.redcraftchat.messaging.mail.providers.MailProvider;
import org.redcraft.redcraftchat.messaging.mail.providers.RedCraftApiMailProvider;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.players.PlayerMail;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.google.common.reflect.TypeToken;

import net.md_5.bungee.api.connection.ProxiedPlayer;

public class MailMessagesManager {

    private MailMessagesManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    @SuppressWarnings("unchecked")
    public static List<PlayerMail> getPlayerMail(ProxiedPlayer player, boolean unreadOnly) {
        String cacheKey = player.getUniqueId().toString() + ":" + unreadOnly;

        List<PlayerMail> mails = (List<PlayerMail>) CacheManager.get(CacheCategory.PLAYER_MAILS, cacheKey, new TypeToken<List<PlayerMail>>() {}.getType());

        if (mails != null) {
            return mails;
        }

        mails = getMailProvider().getMails(player.getUniqueId(), unreadOnly);

        CacheManager.put(CacheCategory.PLAYER_MAILS, cacheKey, mails);

        return mails;
    }

    public static List<PlayerMail> getPlayerMail(ProxiedPlayer player) {
        return getPlayerMail(player, false);
    }

    public static void sendMail(ProxiedPlayer sender, UUID recipient, String message) {
        PlayerMail mail = new PlayerMail();
        mail.senderUuid = sender.getUniqueId();
        mail.recipientUuid = recipient;
        mail.message = message;
        mail.originalLanguage = DetectionManager.getLanguage(message);
        if (mail.originalLanguage == null) {
            mail.originalLanguage = PlayerPreferencesManager.getMainPlayerLanguage(sender);
        }
        if (mail.originalLanguage == null) {
            mail.originalLanguage = Config.defaultLocale.split("-")[0];
        }
        mail.sentAt = LocalDateTime.now();

        getMailProvider().sendMail(mail);

        voidCache(sender.getUniqueId());
    }

    public static void markMailAsRead(PlayerMail mail) {
        mail.readAt = LocalDateTime.now();
        getMailProvider().updateMail(mail);

        voidCache(mail.recipientUuid);
    }

    public static void markAllMailAsRead(ProxiedPlayer player) {
        for (PlayerMail mail : getPlayerMail(player, true)) {
            markMailAsRead(mail);
        }
    }

    public static String getMailSenderDisplayName(PlayerMail mail) {
        PlayerPreferences sender = null;
        try {
            sender = PlayerPreferencesManager.getPlayerPreferences(mail.senderUuid);
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (sender == null || sender.lastKnownMinecraftName == null) {
            return "Unknown";
        }

        return sender.lastKnownMinecraftName;
    }

    private static void voidCache(UUID playerUuid) {
        CacheManager.delete(CacheCategory.PLAYER_MAILS, playerUuid.toString() + ":false");
        CacheManager.delete(CacheCategory.PLAYER_MAILS, playerUuid.toString() + ":true");
    }

    public static MailProvider getMailProvider() {
        switch (Config.mailProvider) {
            case "database":
                return new DatabaseMailProvider();

            case "api":
                return new RedCraftApiMailProvider();

            default:
                throw new IllegalStateException("Unknown mail provider: " + Config.mailProvider);
        }
    }
}
