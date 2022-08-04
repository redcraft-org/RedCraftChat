package org.redcraft.redcraftchat.commands.minecraft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.messaging.MailMessagesManager;
import org.redcraft.redcraftchat.models.players.PlayerMail;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class MailMinecraftCommand extends Command {

    public MailMinecraftCommand() {
        super("mail", "redcraftchat.command.mail");
    }

    public class MailMinecraftCommandHandler implements Runnable {
        CommandSender sender;
        String[] args;

        public MailMinecraftCommandHandler(CommandSender sender, String[] args) {
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

        private void handleUsage(ProxiedPlayer player) {
            BasicMessageFormatter.sendInternalError(player, "Usage:", "/mail <list | listall | read | send>");
            BasicMessageFormatter.sendInternalMessage(player, "To see your unread mails, type:", "/mail list", ChatColor.GREEN);
            BasicMessageFormatter.sendInternalMessage(player, "To see your mails including the ones you already read, type:", "/mail listall", ChatColor.GREEN);
            BasicMessageFormatter.sendInternalMessage(player, "To mark all your emails as read, type:", "/mail read", ChatColor.GREEN);
            BasicMessageFormatter.sendInternalMessage(player, "To send a mail, type:", "/mail send <player> <message>", ChatColor.GREEN);
        }

        private void handleMessageRead(ProxiedPlayer player, String id) {
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
                BasicMessageFormatter.sendInternalMessage(sender, "Mail marked as read", ChatColor.GREEN);
                handleMailList(player, 1, true);
            } else {
                MailMessagesManager.markAllMailAsRead(player);
                BasicMessageFormatter.sendInternalMessage(sender, "All mails marked as read", ChatColor.GREEN);
            }
        }

        private void handleMailSend(ProxiedPlayer player, String recipientUsername, String message) {
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
            BasicMessageFormatter.sendInternalMessage(sender, "Mail sent to " + recipient.lastKnownMinecraftName, ChatColor.GREEN);
        }

        private void handleMailList(ProxiedPlayer player, int page, boolean unreadOnly) {
            List<PlayerMail> mails = MailMessagesManager.getPlayerMail(player, unreadOnly);
            if (page < 1) {
                BasicMessageFormatter.sendInternalError(player, "Invalid page number");
                return;
            }

            int elementsPerPage = 5;
            int totalPages = (int) Math.ceil(mails.size() / (double) elementsPerPage);
            int start = (page - 1) * elementsPerPage;
            int end = start + elementsPerPage;

            if (end > mails.size()) {
                end = mails.size();
            }

            List<PlayerMail> mailsToDisplay = mails.subList(start, end);

            List<BaseComponent[]> menu;
            try {
                menu = generateMenu(player, page, totalPages, elementsPerPage, mailsToDisplay, unreadOnly);
            } catch (IOException|InterruptedException e) {
                BasicMessageFormatter.sendInternalError(player, "Error while generating menu, please try again later");
                return;
            }

            for (BaseComponent[] message : menu) {
                player.sendMessage(message);
            }
        }

        private List<BaseComponent[]> generateMenu(ProxiedPlayer player, int page, int totalPages, int elementsPerPage, List<PlayerMail> mailsToDisplay, boolean unreadOnly) throws IOException, InterruptedException {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);

            String originalHeaderText = "MAIL INBOX";
            String originalNoMailsText = "You have no mails.";
            if (unreadOnly) {
                originalNoMailsText += "\nTip: run %command% to see messages you already read";
            }

            String headerText = PlayerPreferencesManager.localizeMessageForPlayer(preferences, originalHeaderText);
            String noMailsText = PlayerPreferencesManager.localizeMessageForPlayer(preferences, originalNoMailsText).replace("%command%", "/mail listall");

            List<BaseComponent[]> messages = new ArrayList<BaseComponent[]>();

            // Add 5 empty lines to make the menu look better
            for (int i = 0; i < 5; i++) {
                messages.add(new ComponentBuilder().create());
            }

            messages.add(new ComponentBuilder()
                    .append("---------- ").color(ChatColor.GREEN)
                    .append(headerText).color(ChatColor.GOLD).event(new HoverEvent(Action.SHOW_TEXT, new Text(originalHeaderText)))
                    .append(" ----------").color(ChatColor.GREEN)
                    .create());

            messages.add(getPageSelector(preferences, page, totalPages, unreadOnly));

            if (mailsToDisplay.isEmpty()) {
                messages.add(new ComponentBuilder("").create());
                messages.add(new ComponentBuilder("").create());
                messages.add(new ComponentBuilder()
                        .append(noMailsText).color(ChatColor.RED)
                        .event(new HoverEvent(Action.SHOW_TEXT, new Text(originalNoMailsText)))
                        .create());
                messages.add(new ComponentBuilder("").create());
                messages.add(new ComponentBuilder("").create());
            } else {
                for (PlayerMail mail : mailsToDisplay) {
                    messages.add(getMailMessage(preferences, mail));
                }
                // Pad to always have the same number of lines
                for (int i = 0; i < elementsPerPage - mailsToDisplay.size(); i++) {
                    messages.add(new ComponentBuilder().create());
                }

                String hoverHelpMessage = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "Tip: Hover the message to see the full text");

                messages.add(new ComponentBuilder(hoverHelpMessage).color(ChatColor.ITALIC).color(ChatColor.YELLOW).create());
            }

            return messages;
        }

        private BaseComponent[] getMailMessage(PlayerPreferences preferences, PlayerMail mail) {
            String markAsReadText = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "Mark as read");
            String alreadyMarkedAsReadText = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "Already marked as read");
            String clickToReplyText = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "Click to reply");

            ComponentBuilder mailMessage = new ComponentBuilder();

            if (mail.readAt == null) {
                mailMessage
                        .append("[NEW] ")
                        .color(ChatColor.GREEN)
                        .event(new HoverEvent(Action.SHOW_TEXT, new Text(markAsReadText)))
                        .event(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/mail read " + mail.internalId));
            } else {
                mailMessage
                        .append("[OLD] ")
                        .color(ChatColor.GRAY)
                        .event(new HoverEvent(Action.SHOW_TEXT, new Text(alreadyMarkedAsReadText)));
            }

            String senderDisplayName = MailMessagesManager.getMailSenderDisplayName(mail);

            mailMessage.append(senderDisplayName).color(ChatColor.GOLD);

            mailMessage.append(" -> ").color(ChatColor.GRAY);

            String fullMessage = PlayerPreferencesManager.localizeMessageForPlayer(preferences, mail.message);

            String messagePreview = fullMessage.substring(0, Math.min(mail.message.length(), 25));

            if (!messagePreview.equals(fullMessage)) {
                messagePreview += "...";
            }

            if (!mail.message.equals(fullMessage)) {
                fullMessage += "\n\nOriginal [" + mail.originalLanguage.toUpperCase() + "]\n" + mail.message;;
            }

            fullMessage += "\n\n" + ChatColor.GREEN + clickToReplyText;

            mailMessage.append(messagePreview).color(ChatColor.WHITE);

            mailMessage.event(new HoverEvent(Action.SHOW_TEXT, new Text(fullMessage)));
            mailMessage.event(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND, "/mail send " + senderDisplayName + " "));

            return mailMessage.create();
        }

        private BaseComponent[] getPageSelector(PlayerPreferences preferences, int page, int totalPages, boolean unreadOnly) {
            String originalPreviousText = "Previous page";
            String originalPreviousTooltipText = "Click to go to the previous page";
            String originalNextText = "Next page";
            String originalNextTooltipText = "Click to go to the next page";

            String previousText = PlayerPreferencesManager.localizeMessageForPlayer(preferences, originalPreviousText);
            String previousTooltipText = PlayerPreferencesManager.localizeMessageForPlayer(preferences, originalPreviousTooltipText);
            String nextText = PlayerPreferencesManager.localizeMessageForPlayer(preferences, originalNextText);
            String nextTooltipText = PlayerPreferencesManager.localizeMessageForPlayer(preferences, originalNextTooltipText);

            String subCommand = unreadOnly ? "list" : "listall";

            ComponentBuilder pageNavigation = new ComponentBuilder();
            pageNavigation.append("[" + previousText + "]");
            if (page > 1) {
                pageNavigation.event(new HoverEvent(Action.SHOW_TEXT, new Text(previousTooltipText)));
                pageNavigation.event(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/mail " + subCommand + " " + (page - 1)));
            } else {
                pageNavigation.color(ChatColor.GRAY);
            }

            String spacer = "          ";
            pageNavigation.append(spacer + page + "/" + totalPages + spacer).color(ChatColor.YELLOW);

            pageNavigation.append("[" + nextText + "]");
            if (page < totalPages) {
                pageNavigation.event(new HoverEvent(Action.SHOW_TEXT, new Text(nextTooltipText)));
                pageNavigation.event(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/mail "
                        + subCommand + " " + (page + 1)));
            } else {
                pageNavigation.color(ChatColor.GRAY);
            }

            return pageNavigation.create();
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        var commandHandler = new MailMinecraftCommandHandler(sender, args);
        RedCraftChat.getInstance().getProxy().getScheduler().runAsync(RedCraftChat.getInstance(), commandHandler);
    }
}
