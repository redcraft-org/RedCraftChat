package org.redcraft.redcraftchat.commands.minecraft;

import java.io.IOException;
import org.redcraft.redcraftchat.commands.Suggestions;
import org.redcraft.redcraftchat.dialog.MailDialog;
import org.redcraft.redcraftchat.locales.UiStrings;
import org.redcraft.redcraftchat.messaging.MailView;
import org.redcraft.redcraftchat.bedrock.BedrockMailInbox;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.helpers.LegacyText;
import org.redcraft.redcraftchat.messaging.MailMessagesManager;
import org.redcraft.redcraftchat.models.players.PlayerMail;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class MailMinecraftCommand implements SimpleCommand {

    private static final String MAIL_UNKNOWN_SLOT = UiStrings.MAIL_UNKNOWN_SLOT;
    private static final String MAIL_REPLY_USAGE = UiStrings.MAIL_REPLY_USAGE;

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

            // Bare /mail opens the mailbox. Printing usage instead made the
            // interface something you had to already know the name of.
            if (openInterface(player, args)) {
                return;
            }

            if (args.length > 0) {
                // Bedrock first: its inbox is a form, and the chat one is
                // inert there in every direction
                switch (args[0].toLowerCase(java.util.Locale.ROOT)) {
                    case "show":
                    case "open":
                        handleShow(player, args.length > 1 ? args[1] : null,
                                "open".equalsIgnoreCase(args[0]));
                        break;

                    case "next":
                        handlePageStep(player, 1);
                        break;

                    case "prev":
                        handlePageStep(player, -1);
                        break;

                    case "page":
                        handlePageJump(player, args.length > 1 ? args[1] : null);
                        break;

                    case "reply":
                        handleSlotReply(player, args);
                        break;

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
                        int page = 1;
                        if (args.length > 1) {
                            try {
                                page = Integer.parseInt(args[1]);
                            } catch (NumberFormatException e) {
                                // BungeeCord let this one blow up inside the async task
                                BasicMessageFormatter.sendInternalError(player, "Invalid page number");
                                return;
                            }
                        }
                        handleMailList(player, page, !args[0].equals("listall"));
                        return;

                    default:
                        break;
                }
            }

            handleUsage(player);
        }

        /**
         * The mailbox, on whichever surface this client can show.
         *
         * Dialog for a modern Java client, the Bedrock form for Bedrock, and
         * the chat inbox for everything else. Only for the bare command and
         * the list verbs: anything with its own arguments means the player
         * already knows what they want and should get it.
         */
        private boolean openInterface(Player player, String[] args) {
            if (!isListing(args)) {
                return false;
            }
            if (MailDialog.isSupported(player) && MailDialog.showInbox(player)) {
                return true;
            }
            if (BedrockMailInbox.isSupported(player)) {
                boolean unreadOnly = args.length == 0 || !"listall".equalsIgnoreCase(args[0]);
                if (BedrockMailInbox.showInbox(player, unreadOnly)) {
                    return true;
                }
            }
            return false;
        }

        /** Bare /mail, /mail list and /mail listall all open the inbox. */
        private boolean isListing(String[] args) {
            if (args.length == 0) {
                return true;
            }
            String verb = args[0].toLowerCase(java.util.Locale.ROOT);
            return "list".equals(verb) || "listall".equals(verb);
        }

        /**
         * Prints one mail in full.
         *
         * The list can only ever show a preview, and the rest used to live in
         * a hover, which is invisible to anyone without a mouse and to every
         * Bedrock player. This is the path that makes the body readable.
         */
        private void handleShow(Player player, String rawSlot, boolean markRead) {
            try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            MailView view = MailView.of(player.getUniqueId());

            Integer slot = parseSlot(rawSlot);
            String internalId = slot == null ? null : view.internalId(slot);
            if (internalId == null) {
                BasicMessageFormatter.sendInternalMessage(player,
                        PlayerPreferencesManager.localizeUiForPlayer(preferences, MAIL_UNKNOWN_SLOT),
                        NamedTextColor.RED);
                return;
            }

            PlayerMail mail = null;
            for (PlayerMail candidate : MailMessagesManager.getPlayerMail(player)) {
                if (internalId.equals(candidate.internalId)) {
                    mail = candidate;
                    break;
                }
            }
            if (mail == null) {
                BasicMessageFormatter.sendInternalMessage(player,
                        PlayerPreferencesManager.localizeUiForPlayer(preferences, MAIL_UNKNOWN_SLOT),
                        NamedTextColor.RED);
                return;
            }

            String sender = MailMessagesManager.getMailSenderDisplayName(mail);
            String body = PlayerPreferencesManager.localizeMessageForPlayer(preferences, mail.message);

            player.sendMessage(Component.text(sender + " -> ", NamedTextColor.AQUA));
            player.sendMessage(Component.text(body, NamedTextColor.WHITE));
            if (mail.originalLanguage != null && !body.equals(mail.message)) {
                // The original used to be hover-only, so it was lost to
                // exactly the players who most needed the translation shown
                player.sendMessage(Component.text(
                        "[" + mail.originalLanguage.toUpperCase() + "] " + mail.message,
                        NamedTextColor.DARK_GRAY));
            }
            player.sendMessage(Component.text("/mail reply " + slot + " ", NamedTextColor.GRAY));

            if (markRead && mail.readAt == null) {
                MailMessagesManager.markMailAsRead(mail);
            }
            } catch (IOException | InterruptedException e) {
                BasicMessageFormatter.sendInternalError(player, "Could not read that mail");
            }
        }

        private void handlePageStep(Player player, int delta) {
            MailView view = MailView.of(player.getUniqueId());
            handleMailList(player, Math.max(1, view.page() + delta), view.unreadOnly());
        }

        private void handlePageJump(Player player, String rawPage) {
            MailView view = MailView.of(player.getUniqueId());
            int page = 1;
            try {
                page = rawPage == null ? 1 : Integer.parseInt(rawPage);
            } catch (NumberFormatException e) {
                page = 1;
            }
            handleMailList(player, Math.max(1, page), view.unreadOnly());
        }

        /** Reply by slot, so the sender's name never has to be typed. */
        private void handleSlotReply(Player player, String[] args) {
            try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            MailView view = MailView.of(player.getUniqueId());

            Integer slot = args.length > 1 ? parseSlot(args[1]) : null;
            String internalId = slot == null ? null : view.internalId(slot);
            if (internalId == null || args.length < 3) {
                BasicMessageFormatter.sendInternalMessage(player,
                        PlayerPreferencesManager.localizeUiForPlayer(preferences, MAIL_REPLY_USAGE),
                        NamedTextColor.RED);
                return;
            }

            for (PlayerMail mail : MailMessagesManager.getPlayerMail(player)) {
                if (internalId.equals(mail.internalId)) {
                    String message = String.join(" ", Arrays.asList(args).subList(2, args.length));
                    MailMessagesManager.sendMail(player, mail.senderUuid, message);
                    BasicMessageFormatter.sendInternalMessage(player,
                            PlayerPreferencesManager.localizeUiForPlayer(preferences, "Mail sent to ")
                                    + MailMessagesManager.getMailSenderDisplayName(mail),
                            NamedTextColor.GREEN);
                    return;
                }
            }
            } catch (IOException | InterruptedException e) {
                BasicMessageFormatter.sendInternalError(player, "Could not send that reply");
            }
        }

        private Integer parseSlot(String raw) {
            if (raw == null) {
                return null;
            }
            try {
                return Integer.parseInt(raw.trim());
            } catch (NumberFormatException e) {
                return null;
            }
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

            int elementsPerPage = 5;
            int totalPages = (int) Math.ceil(mails.size() / (double) elementsPerPage);

            // BungeeCord never checked the upper bound and threw out of the async
            // task, leaving the player without a menu and without an explanation.
            // An empty inbox has no pages but still renders its page 1 menu, so the
            // bound is never below 1, otherwise "/mail list 3" on an empty inbox
            // walks past the end of the list and throws again.
            if (page > Math.max(totalPages, 1)) {
                BasicMessageFormatter.sendInternalError(player, "Invalid page number");
                return;
            }

            int start = (page - 1) * elementsPerPage;
            int end = start + elementsPerPage;

            if (end > mails.size()) {
                end = mails.size();
            }

            List<PlayerMail> mailsToDisplay = mails.subList(start, end);
            // Recorded before rendering, so the numbers printed on the rows
            // and the numbers /mail show accepts are the same list
            MailView.of(player.getUniqueId()).record(page, unreadOnly, mailsToDisplay);

            List<Component> menu;
            try {
                menu = generateMenu(player, page, totalPages, elementsPerPage, mailsToDisplay, unreadOnly);
            } catch (IOException | InterruptedException e) {
                BasicMessageFormatter.sendInternalError(player, "Error while generating menu, please try again later");
                return;
            }

            for (Component message : menu) {
                player.sendMessage(message);
            }
        }

        private List<Component> generateMenu(Player player, int page, int totalPages, int elementsPerPage, List<PlayerMail> mailsToDisplay, boolean unreadOnly) throws IOException, InterruptedException {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);

            String originalHeaderText = "MAIL INBOX";
            String originalNoMailsText = "You have no mails.";
            if (unreadOnly) {
                originalNoMailsText += LegacyText.DARK_PURPLE + "\n\nTip: run the command %command% to see messages you already read";
            }

            String headerText = PlayerPreferencesManager.localizeUiForPlayer(preferences, originalHeaderText);
            String noMailsText = PlayerPreferencesManager.localizeUiForPlayer(preferences, originalNoMailsText).replace("%command%", "/mail listall");

            List<Component> messages = new ArrayList<Component>();

            // Add 5 empty lines to make the menu look better
            for (int i = 0; i < 5; i++) {
                messages.add(Component.empty());
            }

            // The trailing dashes inherited the header hover through the BungeeCord
            // format retention, Adventure has no cascade between siblings so it is set again
            HoverEvent<Component> headerHover = showText(originalHeaderText);
            messages.add(Component.text()
                    .append(Component.text("---------- ", NamedTextColor.GREEN))
                    .append(Component.text(headerText, NamedTextColor.GOLD).hoverEvent(headerHover))
                    .append(Component.text(" ----------", NamedTextColor.GREEN).hoverEvent(headerHover))
                    .build());

            messages.add(getPageSelector(preferences, page, totalPages, unreadOnly));

            if (mailsToDisplay.isEmpty()) {
                messages.add(Component.empty());
                messages.add(Component.empty());
                messages.add(BasicMessageFormatter.deserialize(noMailsText).colorIfAbsent(NamedTextColor.RED)
                        .hoverEvent(showText(originalNoMailsText.replace("%command%", "/mail listall"))));
                messages.add(Component.empty());
                messages.add(Component.empty());
            } else {
                int slotNumber = 0;
                for (PlayerMail mail : mailsToDisplay) {
                    messages.add(getMailMessage(preferences, mail, ++slotNumber));
                }
                // Pad to always have the same number of lines
                for (int i = 0; i < elementsPerPage - mailsToDisplay.size(); i++) {
                    messages.add(Component.empty());
                }

                String hoverHelpMessage = PlayerPreferencesManager.localizeUiForPlayer(preferences, "Tip: Hover the message to see the full text");

                // BungeeCord asked for italic then for yellow and the second call won,
                // so this line renders yellow and upright, which is kept as is
                messages.add(Component.text(hoverHelpMessage, NamedTextColor.YELLOW));
            }

            return messages;
        }

        private Component getMailMessage(PlayerPreferences preferences, PlayerMail mail, int slot) {
            // The number is the handle. Before this the only way to name one
            // mail was its internal id, which lived inside a click event and
            // was never printed, so a player without a mouse could not act on
            // a single mail at all.
            Component slotTag = Component.text(slot + " ", NamedTextColor.DARK_AQUA);
            String markAsReadText = PlayerPreferencesManager.localizeUiForPlayer(preferences, "Mark as read");
            String alreadyMarkedAsReadText = PlayerPreferencesManager.localizeUiForPlayer(preferences, "Already marked as read");
            String clickToReplyText = PlayerPreferencesManager.localizeUiForPlayer(preferences, "Click to reply");

            Component statusTag;
            HoverEvent<Component> statusHover;
            ClickEvent statusClick = null;

            if (mail.readAt == null) {
                statusHover = showText(markAsReadText);
                statusClick = ClickEvent.runCommand("/mail read " + mail.internalId);
                statusTag = Component.text("[NEW] ", NamedTextColor.GREEN)
                        .hoverEvent(statusHover)
                        .clickEvent(statusClick);
            } else {
                statusHover = showText(alreadyMarkedAsReadText);
                statusTag = Component.text("[OLD] ", NamedTextColor.GRAY)
                        .hoverEvent(statusHover);
            }

            String senderDisplayName = MailMessagesManager.getMailSenderDisplayName(mail);

            // The sender name and the arrow inherited the status hover and click on
            // BungeeCord, so clicking them also marks an unread mail as read
            Component senderName = Component.text(senderDisplayName, NamedTextColor.GOLD).hoverEvent(statusHover);
            Component arrow = Component.text(" -> ", NamedTextColor.GRAY).hoverEvent(statusHover);
            if (statusClick != null) {
                senderName = senderName.clickEvent(statusClick);
                arrow = arrow.clickEvent(statusClick);
            }

            String fullMessage = PlayerPreferencesManager.localizeMessageForPlayer(preferences, mail.message);

            // BungeeCord bounded the localized preview by the length of the original
            // message, which threw whenever a translation came out shorter
            String messagePreview = fullMessage.substring(0, Math.min(fullMessage.length(), 25));

            if (!messagePreview.equals(fullMessage)) {
                messagePreview += "...";
            }

            if (!mail.message.equals(fullMessage)) {
                String originalLanguage = mail.originalLanguage != null ? mail.originalLanguage.toUpperCase() : "??";
                fullMessage += "\n\nOriginal [" + originalLanguage + "]\n" + mail.message;
            }

            fullMessage += "\n\n" + LegacyText.GREEN + clickToReplyText;

            // Mail bodies carry the sender's own colour codes, BungeeCord left them
            // literal in the component text and let the client sort them out
            Component preview = BasicMessageFormatter.deserialize(messagePreview)
                    .colorIfAbsent(NamedTextColor.WHITE)
                    .hoverEvent(showText(fullMessage))
                    .clickEvent(ClickEvent.suggestCommand("/mail send " + senderDisplayName + " "));

            return Component.text()
                    .append(slotTag)
                    .append(statusTag)
                    .append(senderName)
                    .append(arrow)
                    .append(preview)
                    .build();
        }

        private Component getPageSelector(PlayerPreferences preferences, int page, int totalPages, boolean unreadOnly) {
            String originalPreviousText = "Previous page";
            String originalPreviousTooltipText = "Click to go to the previous page";
            String originalNextText = "Next page";
            String originalNextTooltipText = "Click to go to the next page";

            String previousText = PlayerPreferencesManager.localizeUiForPlayer(preferences, originalPreviousText);
            String previousTooltipText = PlayerPreferencesManager.localizeUiForPlayer(preferences, originalPreviousTooltipText);
            String nextText = PlayerPreferencesManager.localizeUiForPlayer(preferences, originalNextText);
            String nextTooltipText = PlayerPreferencesManager.localizeUiForPlayer(preferences, originalNextTooltipText);

            String subCommand = unreadOnly ? "list" : "listall";

            Component previous = Component.text("[" + previousText + "]");
            HoverEvent<Component> previousHover = null;
            ClickEvent previousClick = null;
            if (page > 1) {
                previousHover = showText(previousTooltipText);
                previousClick = ClickEvent.runCommand("/mail " + subCommand + " " + (page - 1));
                previous = previous.hoverEvent(previousHover).clickEvent(previousClick);
            } else {
                previous = previous.color(NamedTextColor.GRAY);
            }

            String spacer = "          ";
            Component counter = Component.text(spacer + page + "/" + totalPages + spacer, NamedTextColor.YELLOW);
            // The counter inherited the previous page events on BungeeCord, which makes
            // it a second previous page button, and that is kept
            if (previousClick != null) {
                counter = counter.hoverEvent(previousHover).clickEvent(previousClick);
            }

            Component next = Component.text("[" + nextText + "]");
            if (page < totalPages) {
                next = next.hoverEvent(showText(nextTooltipText))
                        .clickEvent(ClickEvent.runCommand("/mail " + subCommand + " " + (page + 1)));
            } else {
                // On BungeeCord the greyed out next button still inherited the previous
                // page click and walked backwards, here it stays inert
                next = next.color(NamedTextColor.GRAY);
            }

            return Component.text()
                    .append(previous)
                    .append(counter)
                    .append(next)
                    .build();
        }

        private HoverEvent<Component> showText(String legacyText) {
            return HoverEvent.showText(BasicMessageFormatter.deserialize(legacyText));
        }
    }

    @Override
    public void execute(Invocation invocation) {
        var commandHandler = new MailMinecraftCommandHandler(invocation.source(), invocation.arguments());
        RedCraftChat.getInstance().getProxy().getScheduler().buildTask(RedCraftChat.getInstance(), commandHandler).schedule();
    }


    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        int index = Suggestions.wordIndex(args);
        String word = Suggestions.currentWord(args);

        if (index == 0) {
            return Suggestions.matching(java.util.List.of(
                    "list", "listall", "read", "next", "prev", "page", "show", "open", "reply", "send"), word);
        }
        if (index == 1 && args.length > 0) {
            String verb = args[0].toLowerCase(java.util.Locale.ROOT);
            if ("show".equals(verb) || "open".equals(verb) || "reply".equals(verb)) {
                // Only the slots actually on screen, so a suggestion cannot
                // point at a row the player is not looking at
                if (invocation.source() instanceof Player) {
                    return Suggestions.matching(
                            MailView.of(((Player) invocation.source()).getUniqueId()).slotNumbers(), word);
                }
            }
        }
        if (index == 1 && args.length > 0 && "send".equalsIgnoreCase(args[0])) {
            return Suggestions.matching(onlinePlayerNames(), word);
        }
        return java.util.List.of();
    }

    private List<String> onlinePlayerNames() {
        List<String> names = new ArrayList<>();
        for (Player online : RedCraftChat.getInstance().getProxy().getAllPlayers()) {
            names.add(online.getUsername());
        }
        return names;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("redcraftchat.command.mail");
    }
}
