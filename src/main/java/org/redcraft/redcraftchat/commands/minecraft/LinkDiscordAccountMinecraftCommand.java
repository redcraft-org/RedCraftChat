package org.redcraft.redcraftchat.commands.minecraft;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.discord.AccountLinkManager;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.models.caching.AccountLinkCode;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class LinkDiscordAccountMinecraftCommand implements SimpleCommand {

    public class LinkDiscordAccountMinecraftCommandHandler implements Runnable {
        CommandSource sender;
        String[] args;

        public LinkDiscordAccountMinecraftCommandHandler(CommandSource sender, String[] args) {
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

            try {
                PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);

                if (args.length > 0 && args[0].equalsIgnoreCase("unlink")) {
                    if (preferences.discordId == null) {
                        BasicMessageFormatter.sendInternalError(player, "You are not linked to a Discord account, can't unlink");
                        return;
                    }
                    AccountLinkManager.unLinkAccounts(preferences);
                    BasicMessageFormatter.sendInternalMessage(player, "You have successfully unlinked your accounts", NamedTextColor.GREEN);
                    return;
                }

                if (preferences.discordId != null) {
                    String message = PlayerPreferencesManager.localizeUiForPlayer(preferences, "You already linked your Discord account. If you wish to unlink it, click on the button below");
                    String unlink = PlayerPreferencesManager.localizeUiForPlayer(preferences, "Unlink");
                    String tooltip = PlayerPreferencesManager.localizeUiForPlayer(preferences, "Unlink your Discord account");

                    String command = "/discord-link unlink";

                    Component formattedMessage = Component.text(message, NamedTextColor.YELLOW);

                    Component button = Component.text(unlink, NamedTextColor.DARK_RED, TextDecoration.BOLD)
                            .clickEvent(ClickEvent.runCommand(command))
                            .hoverEvent(HoverEvent.showText(Component.text(tooltip, NamedTextColor.RED)));

                    player.sendMessage(formattedMessage);
                    player.sendMessage(button);
                    return;
                }

                if (args.length > 0) {
                    if (AccountLinkManager.linkAccounts(preferences, args[0])) {
                        BasicMessageFormatter.sendInternalMessage(player, "You have successfully linked your accounts", NamedTextColor.GREEN);
                        return;
                    }
                    BasicMessageFormatter.sendInternalError(player, "Invalid code");
                    return;
                }

                AccountLinkCode code = AccountLinkManager.getLinkCode(player);

                String message = PlayerPreferencesManager.localizeUiForPlayer(preferences, "Please run the following command on our Discord server (click to copy): ");
                String copyToClipboard = PlayerPreferencesManager.localizeUiForPlayer(preferences, "Copy to clipboard");

                String command = "/minecraft-link " + code.token;

                Component formattedMessage = Component.text()
                        .append(Component.text(message, NamedTextColor.GREEN))
                        .append(Component.text(command, NamedTextColor.GOLD)
                                .clickEvent(ClickEvent.copyToClipboard(command))
                                .hoverEvent(HoverEvent.showText(Component.text(copyToClipboard, NamedTextColor.GREEN))))
                        .build();

                player.sendMessage(formattedMessage);
            } catch (Exception e) {
                BasicMessageFormatter.sendInternalError(player, "An error occured while trying to link Discord account, please try again later");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void execute(Invocation invocation) {
        var commandHandler = new LinkDiscordAccountMinecraftCommandHandler(invocation.source(), invocation.arguments());
        RedCraftChat.getInstance().getProxy().getScheduler().buildTask(RedCraftChat.getInstance(), commandHandler).schedule();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("redcraftchat.command.link-discord-account");
    }
}
