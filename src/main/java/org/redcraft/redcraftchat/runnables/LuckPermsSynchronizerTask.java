package org.redcraft.redcraftchat.runnables;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.LegacyText;
import org.redcraft.redcraftchat.players.DisplayNameManager;

import com.velocitypowered.api.proxy.Player;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

public class LuckPermsSynchronizerTask implements Runnable {

    public static boolean updateUsername(Player player) {
        try {
            updateUsername(player, LuckPermsProvider.get());
            return true;
        } catch (IllegalStateException e) {
            // LuckPerms not installed
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().error("Error updating username for " + player.getUsername());
            e.printStackTrace();
        }
        return false;
    }

    public static void updateUsername(Player player, LuckPerms lp) {
        User user = lp.getUserManager().getUser(player.getUniqueId());
        String prefix = user.getCachedData().getMetaData().getPrefix();
        String formattedPrefix = "";
        if (prefix != null) {
            formattedPrefix = LegacyText.translateAlternateColorCodes('&', prefix);
        }
        String displayName = formattedPrefix + player.getUsername();
        if (!DisplayNameManager.getDisplayName(player).equals(displayName)) {
            DisplayNameManager.setDisplayName(player.getUniqueId(), displayName);
            RedCraftChat.getInstance().getLogger()
                    .info("Set " + player.getUsername() + " display name to " + displayName);
        }
    }

    public void run() {
        for (Player player : RedCraftChat.getInstance().getProxy().getAllPlayers()) {
            updateUsername(player);
        }
    }

}
