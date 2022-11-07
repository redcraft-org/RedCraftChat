package org.redcraft.redcraftchat.commands.minecraft;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

public class BroadcastMinecraftCommand extends Command {

    public BroadcastMinecraftCommand() {
        super("broadcast", "redcraftchat.command.broadcast", "bc", "alert");
    }

    public class BroadcastMinecraftCommandHandler implements Runnable {
        CommandSender sender;
        String[] args;

        public BroadcastMinecraftCommandHandler(CommandSender sender, String[] args) {
            this.sender = sender;
            this.args = args;
        }

        @Override
        public void run() {
            if (args.length < 1) {
                BasicMessageFormatter.sendInternalError(sender, "You must specify a message");
                return;
            }

            String message = String.join(" ", args);

            // TODO embeds
            MinecraftDiscordBridge.getInstance().broadcastMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "[Alert]" + ChatColor.RESET + " " + ChatColor.YELLOW + message);
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        var commandHandler = new BroadcastMinecraftCommandHandler(sender, args);
        RedCraftChat.getInstance().getProxy().getScheduler().runAsync(RedCraftChat.getInstance(), commandHandler);
    }
}
