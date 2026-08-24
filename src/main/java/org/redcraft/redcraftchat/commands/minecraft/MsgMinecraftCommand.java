package org.redcraft.redcraftchat.commands.minecraft;

import java.util.Arrays;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.helpers.LegacyText;
import org.redcraft.redcraftchat.messaging.PrivateMessagesManager;
import org.redcraft.redcraftchat.players.DisplayNameManager;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

public class MsgMinecraftCommand implements SimpleCommand {

    public class MsgMinecraftCommandHandler implements Runnable {
        CommandSource sender;
        String[] args;

        public MsgMinecraftCommandHandler(CommandSource sender, String[] args) {
            this.sender = sender;
            this.args = args;
        }

        @Override
        public void run() {
            if (args.length < 2) {
                BasicMessageFormatter.sendInternalError(sender, "Usage: /m <player> <message>");
                return;
            }

            Player receiver = RedCraftChat.getInstance().getProxy().getPlayer(args[0]).orElse(null);
            if (receiver == null) {
                BasicMessageFormatter.sendInternalError(sender, "Not found:", args[0]);
                return;
            }

            String message =  String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            if (!(sender instanceof Player)) {
                String displayedMessage = PlayerPreferencesManager.localizeMessageForPlayer(receiver, message);
                PrivateMessagesManager.sendToPlayer(receiver, LegacyText.DARK_RED + "Console", DisplayNameManager.getDisplayName(receiver), displayedMessage, message, null, null, null);
                return;
            }

            PrivateMessagesManager.handlePrivateMessage((Player) sender, receiver, message);
        }
    }

    @Override
    public void execute(Invocation invocation) {
        var commandHandler = new MsgMinecraftCommandHandler(invocation.source(), invocation.arguments());
        RedCraftChat.getInstance().getProxy().getScheduler().buildTask(RedCraftChat.getInstance(), commandHandler).schedule();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("redcraftchat.command.msg");
    }
}
