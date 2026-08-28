package org.redcraft.redcraftchat.messaging;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.detection.DetectionManager;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.messaging.mail.providers.DatabaseMailProvider;
import org.redcraft.redcraftchat.messaging.mail.providers.MailProvider;
import org.redcraft.redcraftchat.messaging.mail.providers.RedCraftApiMailProvider;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.players.PlayerMail;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.google.common.reflect.TypeToken;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.format.NamedTextColor;

import org.redcraft.redcraftchat.helpers.LegacyText;

public class MailMessagesManager {

    /**
     * When a mail arrived, written yyyy/MM/dd HH:mm.
     *
     * Big end first, so there is no reading it as a month-first date and no
     * wondering which of two numbers is the day. The year is always there
     * rather than only on old mails: players come back after years away, and
     * a bare day and month says nothing about which year it belongs to.
     *
     * Numeric on purpose. A date written this way means the same thing in
     * every language, so showing it costs no translation.
     */
    public static String formatSentAt(LocalDateTime at) {
        // yyyy, never YYYY: the capital is the week-based year, which reports
        // the next year for the last days of December
        return at == null ? null : SENT_AT.format(at);
    }

    private static final DateTimeFormatter SENT_AT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private MailMessagesManager() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    @SuppressWarnings("unchecked")
    public static List<PlayerMail> getPlayerMail(Player player, boolean unreadOnly) {
        String cacheKey = player.getUniqueId().toString() + ":" + unreadOnly;

        List<PlayerMail> mails = (List<PlayerMail>) CacheManager.get(CacheCategory.PLAYER_MAILS, cacheKey, new TypeToken<List<PlayerMail>>() {}.getType());

        if (mails != null) {
            return mails;
        }

        mails = getMailProvider().getMails(player.getUniqueId(), unreadOnly);

        CacheManager.put(CacheCategory.PLAYER_MAILS, cacheKey, mails);

        return mails;
    }

    public static List<PlayerMail> getPlayerMail(Player player) {
        return getPlayerMail(player, false);
    }

    public static void sendMail(Player sender, UUID recipient, String message) {
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
        voidCache(recipient);

        Player recipientPlayer = RedCraftChat.getInstance().getProxy().getPlayer(recipient).orElse(null);
        if (recipientPlayer != null) {
            String receivedMessage = "You just received a new mail, type the following command to see your mails:";
            BasicMessageFormatter.sendInternalMessage(recipientPlayer, receivedMessage, LegacyText.GOLD + "/mail list", NamedTextColor.LIGHT_PURPLE);
        }
    }

    public static void markMailAsRead(PlayerMail mail) {
        mail.readAt = LocalDateTime.now();
        getMailProvider().updateMail(mail);

        voidCache(mail.recipientUuid);
    }

    public static void markAllMailAsRead(Player player) {
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
