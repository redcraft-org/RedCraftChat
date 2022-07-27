package org.redcraft.redcraftchat.commands.minecraft;

import java.io.IOException;

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
        super("player-settings");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer player = null;

        if (!sender.hasPermission("redcraftchat.command.player-settings")) {
            BasicMessageFormatter.sendInternalError(sender, "You do not have the permission to use this command");
            return;
        }

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
