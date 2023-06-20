package org.redcraft.redcraftchat.commands.minecraft;

import java.util.Arrays;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.messaging.PrivateMessagesManager;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class MsgMinecraftCommand extends Command {

    public MsgMinecraftCommand() {
        super("msg", "redcraftchat.command.msg", "minecraft:tell", "tell", "m", "w");
    }

    public class MsgMinecraftCommandHandler implements Runnable {
        CommandSender sender;
        String[] args;

        public MsgMinecraftCommandHandler(CommandSender sender, String[] args) {
            this.sender = sender;
            this.args = args;
        }

        @Override
        public void run() {
            if (args.length < 2) {
                BasicMessageFormatter.sendInternalError(sender, "Usage: /m <player> <message>");
                return;
            }

            ProxiedPlayer receiver = RedCraftChat.getInstance().getProxy().getPlayer(args[0]);
            if (receiver == null) {
                BasicMessageFormatter.sendInternalError(sender, "Not found:", args[0]);
                return;
            }

            String message =  String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            if (!(sender instanceof ProxiedPlayer)) {
                String displayedMessage = PlayerPreferencesManager.localizeMessageForPlayer(receiver, message);
                PrivateMessagesManager.sendToPlayer(receiver, ChatColor.DARK_RED + "Console", receiver.getDisplayName(), displayedMessage, message, null, null, null);
                return;
            }

            PrivateMessagesManager.handlePrivateMessage((ProxiedPlayer) sender, receiver, message);
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        var commandHandler = new MsgMinecraftCommandHandler(sender, args);
        RedCraftChat.getInstance().getProxy().getScheduler().runAsync(RedCraftChat.getInstance(), commandHandler);
    }
}
