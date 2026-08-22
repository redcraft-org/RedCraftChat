package org.redcraft.redcraftchat.commands.minecraft;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.messaging.PrivateMessagesManager;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

public class ReplyMinecraftCommand implements SimpleCommand {

    public class ReplyMinecraftCommandHandler implements Runnable {
        CommandSource sender;
        String[] args;

        public ReplyMinecraftCommandHandler(CommandSource sender, String[] args) {
            this.sender = sender;
            this.args = args;
        }

        @Override
        public void run() {
            if (!(sender instanceof Player)) {
                BasicMessageFormatter.sendInternalError(sender, "You must be a player to use this command");
                return;
            }

            if (args.length < 1) {
                BasicMessageFormatter.sendInternalError(sender, "Usage: /r <message>");
                return;
            }

            String message =  String.join(" ", args);
            if (!PrivateMessagesManager.handleReply((Player) sender, message)) {
                BasicMessageFormatter.sendInternalError(sender, "You do not have anyone to reply to");
            }
        }
    }

    @Override
    public void execute(Invocation invocation) {
        var commandHandler = new ReplyMinecraftCommandHandler(invocation.source(), invocation.arguments());
        RedCraftChat.getInstance().getProxy().getScheduler().buildTask(RedCraftChat.getInstance(), commandHandler).schedule();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("redcraftchat.command.msg");
    }
}
