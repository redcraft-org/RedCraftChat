package org.redcraft.redcraftchat.commands.discord;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.discord.AccountLinkManager;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.models.caching.AccountLinkCode;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class LinkMinecraftAccountCommand extends ListenerAdapter {

    public LinkMinecraftAccountCommand() {
        DiscordClient.getClient().updateCommands().addCommands(Commands.slash("minecraft-link", "Link Minecraft account").addOption(OptionType.STRING, "code", "Validation code")).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("minecraft-link")) {
            return;
        }

        String code = null;
        OptionMapping codeOption = event.getOption("code");
        if (codeOption != null) {
            code = codeOption.getAsString();
        }

        User user = event.getUser();

        PlayerPreferences preferences = null;

        try {
            preferences = PlayerPreferencesManager.getPlayerPreferences(user);

            RedCraftChat.getInstance().getLogger().info("User " + user.getName() + " (" + user.getId() + ") is trying to link their Minecraft account with the code `" + code + "`");

            if (code != null && code.equalsIgnoreCase("unlink")) {
                if (preferences == null || preferences.discordId == null) {
                    event.replyEmbeds(BasicMessageFormatter.generateDiscordError(user, "You are not linked to a Discord account, can't unlink")).queue();
                    return;
                }
                AccountLinkManager.unLinkAccounts(preferences);
                event.replyEmbeds(BasicMessageFormatter.generateDiscordMessage(user, "Success", "You have successfully unlinked your accounts", 0x00FF00)).queue();
                return;
            }

            if (preferences != null && preferences.minecraftUuid != null) {
                event.replyEmbeds(BasicMessageFormatter.generateDiscordError(user, "You are already linked to a Minecraft account, if you wish to unlink it, use `/minecraft-link unlink`")).queue();
                return;
            }

            if (code != null) {
                if (AccountLinkManager.linkAccounts(preferences, code, user)) {
                    event.replyEmbeds(BasicMessageFormatter.generateDiscordMessage(user, "Success", "You have successfully linked your accounts", 0x00FF00)).queue();
                    return;
                }
                event.replyEmbeds(BasicMessageFormatter.generateDiscordError(user, "Either the code is invalid or you're not connected to our Minecraft server")).queue();
                return;
            }

            AccountLinkCode linkCode = AccountLinkManager.getLinkCode(user);
            event.replyEmbeds(BasicMessageFormatter.generateDiscordMessage(user, "Action required", "Please run the following command on our Minecraft server: `/discord-link " + linkCode.token + "`", 0x00FF00)).queue();
        } catch (Exception e) {
            event.replyEmbeds(BasicMessageFormatter.generateDiscordError(user, "An error occurred while trying to link your accounts, please try again later")).queue();
            e.printStackTrace();
        }
    }
}
