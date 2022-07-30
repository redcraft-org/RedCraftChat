package org.redcraft.redcraftchat.commands.minecraft;

import java.io.IOException;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class CommandSpyMinecraftCommand extends Command {

    public CommandSpyMinecraftCommand() {
        super("cspy");
    }

    public class CommandSpyMinecraftCommandHandler implements Runnable {
        CommandSender sender;
        String[] args;

        public CommandSpyMinecraftCommandHandler(CommandSender sender, String[] args) {
            this.sender = sender;
            this.args = args;
        }

        @Override
        public void run() {
            ProxiedPlayer player = null;

            if (!sender.hasPermission("redcraftchat.moderation.commandspy")) {
                BasicMessageFormatter.sendInternalError(sender, "You do not have the permission to use this command");
                return;
            }

            // If it's not a player we need an arg
            if (!(sender instanceof ProxiedPlayer) && args.length < 1) {
                BasicMessageFormatter.sendInternalError(sender, "You need to specify a player name");
                return;
            }

            // Get from arg
            if (args.length > 0 && sender.hasPermission("redcraftchat.moderation.commandspy.others")) {
                player = ProxyServer.getInstance().getPlayer(args[0]);
            } else {
                player = (ProxiedPlayer) sender;
            }

            if (player == null) {
                BasicMessageFormatter.sendInternalError(sender, "The specified player doesn't seem to be online");
                return;
            }

            if (!player.hasPermission("redcraftchat.moderation.commandspy")) {
                BasicMessageFormatter.sendInternalError(sender,
                        "This player does not have the permission to use command spy");
                return;
            }

            try {
                boolean commandSpyEnabled = PlayerPreferencesManager.toggleCommandSpy(player);
                BasicMessageFormatter.sendInternalMessage(sender,
                        "Command spy " + (commandSpyEnabled ? "enabled" : "disabled"), ChatColor.GREEN);
            } catch (IOException | InterruptedException e) {
                BasicMessageFormatter.sendInternalError(sender,
                        "An error occured while trying to toggle command spy, check logs for more info");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        var commandHandler = new CommandSpyMinecraftCommandHandler(sender, args);
        RedCraftChat.getInstance().getProxy().getScheduler().runAsync(RedCraftChat.getInstance(), commandHandler);
    }
}
