package org.redcraft.redcraftchat.commands.minecraft;

import java.util.Arrays;
import java.util.List;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.messaging.MailMessagesManager;
import org.redcraft.redcraftchat.models.players.PlayerMail;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.format.NamedTextColor;

public class MailMinecraftCommand implements SimpleCommand {

    public class MailMinecraftCommandHandler implements Runnable {
        CommandSource sender;
        String[] args;

        public MailMinecraftCommandHandler(CommandSource sender, String[] args) {
            this.sender = sender;
            this.args = args;
        }

        @Override
        public void run() {
            if (!(sender instanceof Player)) {
                BasicMessageFormatter.sendInternalError(sender, "This command can only be used by players");
                return;
            }

            Player player = (Player) sender;

            if (args.length > 0) {
                switch (args[0]) {
                    case "read":
                        String messageId = args.length > 1 ? args[1] : null;
                        handleMessageRead(player, messageId);
                        return;

                    case "send":
                        if (args.length > 2) {
                            String receiver = args[1];
                            String message = String.join(" ", Arrays.asList(args).subList(2, args.length));
                            handleMailSend(player, receiver, message);
                        } else {
                            BasicMessageFormatter.sendInternalError(player, "Usage:", "/mail send <player> <message>");
                        }
                        return;

                    case "list":
                    case "listall":
                        int page = args.length > 1 ? Integer.parseInt(args[1]) : 1;
                        handleMailList(player, page, !args[0].equals("listall"));
                        return;

                    default:
                        break;
                }
            }

            handleUsage(player);
        }

        private void handleUsage(Player player) {
            BasicMessageFormatter.sendInternalError(player, "Usage:", "/mail <list | listall | read | send>");
            BasicMessageFormatter.sendInternalMessage(player, "To see your unread mails, type:", "/mail list", NamedTextColor.GREEN);
            BasicMessageFormatter.sendInternalMessage(player, "To see your mails including the ones you already read, type:", "/mail listall", NamedTextColor.GREEN);
            BasicMessageFormatter.sendInternalMessage(player, "To mark all your emails as read, type:", "/mail read", NamedTextColor.GREEN);
            BasicMessageFormatter.sendInternalMessage(player, "To send a mail, type:", "/mail send <player> <message>", NamedTextColor.GREEN);
        }

        private void handleMessageRead(Player player, String id) {
            List<PlayerMail> mails = MailMessagesManager.getPlayerMail(player);

            if (id != null) {
                PlayerMail mail = null;
                for (PlayerMail m : mails) {
                    if (m.internalId != null && m.internalId.equals(id)) {
                        mail = m;
                        break;
                    }
                }
                if (mail == null) {
                    BasicMessageFormatter.sendInternalError(player, "Mail not found");
                    return;
                }
                MailMessagesManager.markMailAsRead(mail);
                BasicMessageFormatter.sendInternalMessage(sender, "Mail marked as read", NamedTextColor.GREEN);
                handleMailList(player, 1, true);
            } else {
                MailMessagesManager.markAllMailAsRead(player);
                BasicMessageFormatter.sendInternalMessage(sender, "All mails marked as read", NamedTextColor.GREEN);
            }
        }

        private void handleMailSend(Player player, String recipientUsername, String message) {
            PlayerPreferences recipient = null;
            try {
                recipient = PlayerPreferencesManager.getPlayerPreferences(recipientUsername, true, false);
                if (recipient == null || recipient.minecraftUuid == null) {
                    throw new IllegalArgumentException();
                }
            } catch (Exception e) {
                BasicMessageFormatter.sendInternalError(sender, "Player not found");
                return;
            }

            MailMessagesManager.sendMail(player, recipient.minecraftUuid, message);
            BasicMessageFormatter.sendInternalMessage(sender, "Mail sent to " + recipient.lastKnownMinecraftName, NamedTextColor.GREEN);
        }

        private void handleMailList(Player player, int page, boolean unreadOnly) {
            List<PlayerMail> mails = MailMessagesManager.getPlayerMail(player, unreadOnly);
            if (page < 1) {
                BasicMessageFormatter.sendInternalError(player, "Invalid page number");
                return;
            }

            // TODO wave 2: port the paginated mail inbox menu (header, page selector,
            // per mail hover previews and reply click events) from the BungeeCord
            // ComponentBuilder API to Adventure
            int unread = 0;
            for (PlayerMail mail : mails) {
                if (mail.readAt == null) {
                    unread++;
                }
            }
            BasicMessageFormatter.sendInternalMessage(player, "You have " + mails.size() + " mails (" + unread + " unread)", NamedTextColor.GREEN);
        }
    }

    @Override
    public void execute(Invocation invocation) {
        var commandHandler = new MailMinecraftCommandHandler(invocation.source(), invocation.arguments());
        RedCraftChat.getInstance().getProxy().getScheduler().buildTask(RedCraftChat.getInstance(), commandHandler).schedule();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("redcraftchat.command.mail");
    }
}
