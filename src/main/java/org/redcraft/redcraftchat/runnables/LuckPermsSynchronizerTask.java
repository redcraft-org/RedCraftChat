package org.redcraft.redcraftchat.runnables;

import org.redcraft.redcraftchat.RedCraftChat;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class LuckPermsSynchronizerTask implements Runnable {

    public static boolean updateUsername(ProxiedPlayer player) {
        try {
            updateUsername(player, LuckPermsProvider.get());
            return true;
        } catch (IllegalStateException e) {
            // LuckPerms not installed
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().severe("Error updating username for " + player.getName());
            e.printStackTrace();
        }
        return false;
    }

    public static void updateUsername(ProxiedPlayer player, LuckPerms lp) {
        User user = lp.getUserManager().getUser(player.getUniqueId());
        String prefix = user.getCachedData().getMetaData().getPrefix();
        String formattedPrefix = "";
        if (prefix != null) {
            formattedPrefix = ChatColor.translateAlternateColorCodes('&', prefix);
        }
        String displayName = formattedPrefix + player.getName();
        if (!player.getDisplayName().equals(displayName)) {
            player.setDisplayName(displayName);
            RedCraftChat.getInstance().getLogger()
                    .info("Set " + player.getName() + " display name to " + player.getDisplayName());
        }
    }

    public void run() {
        for (ProxiedPlayer player : RedCraftChat.getInstance().getProxy().getPlayers()) {
            updateUsername(player);
        }
    }

}
