package org.redcraft.redcraftchat.commands.minecraft;

import java.io.IOException;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.google.gson.Gson;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class PlayerSettingsMinecraftCommand extends Command {

    public PlayerSettingsMinecraftCommand() {
        super("player-settings", "redcraftchat.command.player-settings");
    }

    public class PlayerSettingsMinecraftCommandHandler implements Runnable {
        CommandSender sender;
        String[] args;

        public PlayerSettingsMinecraftCommandHandler(CommandSender sender, String[] args) {
            this.sender = sender;
            this.args = args;
        }

        @Override
        public void run() {
            ProxiedPlayer player = null;

            // If it's not a player we need an arg
            if (!(sender instanceof ProxiedPlayer) && args.length < 1) {
                BasicMessageFormatter.sendInternalError(sender, "You need to specify a player name");
                return;
            }

            // Get from arg
            if (args.length > 0 && sender.hasPermission("redcraftchat.command.player-settings.others")) {
                player = ProxyServer.getInstance().getPlayer(args[0]);
            } else {
                player = (ProxiedPlayer) sender;
            }

            if (player == null) {
                BasicMessageFormatter.sendInternalError(sender, "The specified player doesn't seem to be online");
                return;
            }

            try {
                PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
                BasicMessageFormatter.sendInternalMessage(sender, "Current settings: " + new Gson().toJson(preferences), ChatColor.GOLD);
            } catch (IOException | InterruptedException e) {
                BasicMessageFormatter.sendInternalError(sender, "An error occured while trying to display player settings, check logs for more info");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        var commandHandler = new PlayerSettingsMinecraftCommandHandler(sender, args);
        RedCraftChat.getInstance().getProxy().getScheduler().runAsync(RedCraftChat.getInstance(), commandHandler);
    }
}
