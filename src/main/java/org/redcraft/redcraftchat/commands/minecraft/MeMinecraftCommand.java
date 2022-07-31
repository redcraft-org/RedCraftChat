package org.redcraft.redcraftchat.commands.minecraft;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class MeMinecraftCommand extends Command {

    public MeMinecraftCommand() {
        super("me", "redcraftchat.command.me", "minecraft:me");
    }

    public class MeMinecraftCommandHandler implements Runnable {
        CommandSender sender;
        String[] args;

        public MeMinecraftCommandHandler(CommandSender sender, String[] args) {
            this.sender = sender;
            this.args = args;
        }

        @Override
        public void run() {
            if (!(sender instanceof ProxiedPlayer)) {
                BasicMessageFormatter.sendInternalError(sender, "This command can only be used by players");
                return;
            }

            ProxiedPlayer player = (ProxiedPlayer) sender;

            if (args.length < 1) {
                BasicMessageFormatter.sendInternalError(player, "You must specify a message");
                return;
            }

            String message = String.join(" ", args);
            MinecraftDiscordBridge.getInstance().broadcastMessage(
                    ChatColor.DARK_PURPLE + " * " +
                    ChatColor.ITALIC + player.getDisplayName() + " " +
                    ChatColor.LIGHT_PURPLE + ChatColor.ITALIC + message, player);
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        var commandHandler = new MeMinecraftCommandHandler(sender, args);
        RedCraftChat.getInstance().getProxy().getScheduler().runAsync(RedCraftChat.getInstance(), commandHandler);
    }
}
