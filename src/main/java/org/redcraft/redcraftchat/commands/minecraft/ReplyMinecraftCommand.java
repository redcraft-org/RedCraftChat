package org.redcraft.redcraftchat.commands.minecraft;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.messaging.PrivateMessagesManager;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class ReplyMinecraftCommand extends Command {

    public ReplyMinecraftCommand() {
        super("reply", "redcraftchat.command.msg", "r");
    }

    public class ReplyMinecraftCommandHandler implements Runnable {
        CommandSender sender;
        String[] args;

        public ReplyMinecraftCommandHandler(CommandSender sender, String[] args) {
            this.sender = sender;
            this.args = args;
        }

        @Override
        public void run() {
            if (!(sender instanceof ProxiedPlayer)) {
                BasicMessageFormatter.sendInternalError(sender, "You must be a player to use this command");
                return;
            }

            if (args.length < 1) {
                BasicMessageFormatter.sendInternalError(sender, "Usage: /r <message>");
                return;
            }

            String message =  String.join(" ", args);
            if (!PrivateMessagesManager.handleReply((ProxiedPlayer) sender, message)) {
                BasicMessageFormatter.sendInternalError(sender, "You do not have anyone to reply to");
            }
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        var commandHandler = new ReplyMinecraftCommandHandler(sender, args);
        RedCraftChat.getInstance().getProxy().getScheduler().runAsync(RedCraftChat.getInstance(), commandHandler);
    }
}
