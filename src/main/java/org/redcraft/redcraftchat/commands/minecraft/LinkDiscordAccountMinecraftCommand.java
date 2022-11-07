package org.redcraft.redcraftchat.commands.minecraft;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.discord.AccountLinkManager;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.models.caching.AccountLinkCode;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class LinkDiscordAccountMinecraftCommand extends Command {

    public LinkDiscordAccountMinecraftCommand() {
        super("discord-link", "redcraftchat.command.link-discord-account");
    }

    public class LinkDiscordAccountMinecraftCommandHandler implements Runnable {
        CommandSender sender;
        String[] args;

        public LinkDiscordAccountMinecraftCommandHandler(CommandSender sender, String[] args) {
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

            try {
                PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);

                if (args.length > 0 && args[0].equalsIgnoreCase("unlink")) {
                    if (preferences.discordId == null) {
                        BasicMessageFormatter.sendInternalError(player, "You are not linked to a Discord account, can't unlink");
                        return;
                    }
                    AccountLinkManager.unLinkAccounts(preferences);
                    BasicMessageFormatter.sendInternalMessage(player, "You have successfully unlinked your accounts", ChatColor.GREEN);
                    return;
                }

                if (preferences.discordId != null) {
                    String message = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "You already linked your Discord account. If you wish to unlink it, click on the button below");
                    String unlink = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "Unlink");
                    String tooltip = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "Unlink your Discord account");

                    String command = "/discord-link unlink";

                    BaseComponent[] formattedMessage = new ComponentBuilder()
                            .append(message)
                            .color(ChatColor.YELLOW)
                            .create();

                    BaseComponent[] button = new ComponentBuilder()
                            .append(ChatColor.BOLD + unlink)
                            .color(ChatColor.DARK_RED)
                            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.RED + tooltip)))
                            .create();

                    player.sendMessage(formattedMessage);
                    player.sendMessage(button);
                    return;
                }

                if (args.length > 0) {
                    if (AccountLinkManager.linkAccounts(preferences, args[0])) {
                        BasicMessageFormatter.sendInternalMessage(player, "You have successfully linked your accounts", ChatColor.GREEN);
                        return;
                    }
                    BasicMessageFormatter.sendInternalError(player, "Invalid code");
                    return;
                }

                AccountLinkCode code = AccountLinkManager.getLinkCode(player);

                String message = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "Please run the following command on our Discord server (click to copy): ");
                String copyToClipboard = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "Copy to clipboard");

                String command = "/minecraft-link " + code.token;

                BaseComponent[] formattedMessage = new ComponentBuilder()
                        .append(message)
                        .color(ChatColor.GREEN)
                        .append(command)
                        .color(ChatColor.GOLD)
                        .event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, command))
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.GREEN + copyToClipboard)))
                        .create();

                player.sendMessage(formattedMessage);
            } catch (Exception e) {
                BasicMessageFormatter.sendInternalError(player, "An error occured while trying to link Discord account, please try again later");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        var commandHandler = new LinkDiscordAccountMinecraftCommandHandler(sender, args);
        RedCraftChat.getInstance().getProxy().getScheduler().runAsync(RedCraftChat.getInstance(), commandHandler);
    }
}
