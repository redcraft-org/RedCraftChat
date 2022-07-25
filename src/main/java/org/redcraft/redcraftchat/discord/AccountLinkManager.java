package org.redcraft.redcraftchat.discord;

import java.io.IOException;
import java.util.UUID;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.models.caching.AccountLinkCode;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class AccountLinkManager {

    public static AccountLinkCode getLinkCode(ProxiedPlayer player) {
        String uniqueId = player.getUniqueId().toString();
        AccountLinkCode code = getLinkCode(uniqueId);

        if (code.minecraftUuid == null) {
            code.minecraftUuid = uniqueId;
            CacheManager.put(CacheCategory.ACCOUNT_LINK_CODE_USER_ID, uniqueId, code);
            CacheManager.put(CacheCategory.ACCOUNT_LINK_CODE, code.token, code);
        }

        return code;
    }

    public static AccountLinkCode getLinkCode(User user) {
        String uniqueId = user.getId();
        AccountLinkCode code = getLinkCode(uniqueId);

        if (code.discordId == null) {
            code.discordId = uniqueId;
            CacheManager.put(CacheCategory.ACCOUNT_LINK_CODE_USER_ID, uniqueId, code);
            CacheManager.put(CacheCategory.ACCOUNT_LINK_CODE, code.token, code);
        }

        return code;
    }

    public static AccountLinkCode verifyLinkCode(String token) {
        return (AccountLinkCode) CacheManager.get(CacheCategory.ACCOUNT_LINK_CODE, token, AccountLinkCode.class);
    }

    public static boolean linkAccounts(PlayerPreferences preferences, String token) throws IOException, InterruptedException {
        return linkAccounts(preferences, token, null);
    }

    public static boolean linkAccounts(PlayerPreferences preferences, String token, User user) throws IOException, InterruptedException {
        AccountLinkCode code = verifyLinkCode(token);
        if (code == null) {
            return false;
        }
        if ((preferences == null || preferences.minecraftUuid == null) && code.minecraftUuid != null) {
            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(UUID.fromString(code.minecraftUuid));
            preferences = PlayerPreferencesManager.getPlayerPreferences(player);
        }
        if (preferences == null || preferences.minecraftUuid == null) {
            return false;
        }
        if (user == null && code.discordId != null) {
            user = DiscordClient.getClient().getUserById(code.discordId);
        }
        preferences.discordId = user.getId();
        preferences.lastKnownDiscordName = user.getName() + "#" + user.getDiscriminator();
        PlayerPreferencesManager.updatePlayerPreferences(preferences);
        AccountLinkManager.voidLinkCode(code);

        RedCraftChat.getInstance().getLogger().info("Linked accounts for " + preferences.minecraftUuid + " and " + user.getId());

        return true;
    }

    public static void unLinkAccounts(PlayerPreferences preferences) throws IOException, InterruptedException {
        String discordId = preferences.discordId;
        String minecraftUuid = preferences.minecraftUuid.toString();

        preferences.discordId = null;
        preferences.lastKnownDiscordName = null;
        preferences.previousKnownDiscordName = null;
        PlayerPreferencesManager.updatePlayerPreferences(preferences);

        // Remove player preferences manually as we removed the Discord id from the player preferences
        if (discordId != null) {
            CacheManager.delete(CacheCategory.PLAYER_PREFERENCES, discordId);
        }
        if (minecraftUuid != null) {
            CacheManager.delete(CacheCategory.PLAYER_PREFERENCES, minecraftUuid.toString());
        }

        RedCraftChat.getInstance().getLogger().info("Unlinked accounts for " + minecraftUuid + " and " + discordId);
    }

    public static void voidLinkCode(AccountLinkCode code) {
        CacheManager.delete(CacheCategory.ACCOUNT_LINK_CODE, code.token);
        if (code.minecraftUuid != null) {
            CacheManager.delete(CacheCategory.ACCOUNT_LINK_CODE_USER_ID, code.minecraftUuid);
        }
        if (code.discordId != null) {
            CacheManager.delete(CacheCategory.ACCOUNT_LINK_CODE_USER_ID, code.discordId);
        }
    }

    private static AccountLinkCode getLinkCode(String uniqueId) {
        AccountLinkCode code = (AccountLinkCode) CacheManager.get(CacheCategory.ACCOUNT_LINK_CODE_USER_ID, uniqueId, AccountLinkCode.class);

        if (code != null) {
            return code;
        }

        return new AccountLinkCode();
    }
}