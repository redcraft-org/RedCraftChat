package org.redcraft.redcraftchat.commands.minecraft;

import org.redcraft.redcraftchat.discord.AccountLinkManager;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.models.caching.AccountLinkCode;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class LinkDiscordAccountCommand extends Command {

    public LinkDiscordAccountCommand() {
        super("discord-link");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        ProxiedPlayer player = null;

        // If it's not a player we need an arg
        if (!(sender instanceof ProxiedPlayer)) {
            BasicMessageFormatter.sendInternalError(sender, "This command can only be used by players");
            return;
        }

        player = (ProxiedPlayer) sender;

        if (!player.hasPermission("redcraftchat.command.link-discord-account")) {
            BasicMessageFormatter.sendInternalError(player, "You do not have the permission to use this command");
            return;
        }

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
                BasicMessageFormatter.sendInternalError(player, "You already linked your Discord account. If you wish to unlink it, use `/discord-link unlink`");
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
            BasicMessageFormatter.sendInternalMessage(player, "Please run the following command on our Discord server: `/minecraft-link " + code.token + "`", ChatColor.GREEN);
        } catch (Exception e) {
            BasicMessageFormatter.sendInternalError(player, "An error occured while trying to link Discord account, please try again later");
            e.printStackTrace();
        }
    }

}
