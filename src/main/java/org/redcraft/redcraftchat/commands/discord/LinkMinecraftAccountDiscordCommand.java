package org.redcraft.redcraftchat.commands.discord;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.discord.AccountLinkManager;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.models.caching.AccountLinkCode;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class LinkMinecraftAccountDiscordCommand extends ListenerAdapter {

    public LinkMinecraftAccountDiscordCommand() {
        DiscordClient.getClient().upsertCommand(Commands.slash("minecraft-link", "Link Minecraft account").addOption(OptionType.STRING, "code", "Validation code")).queue();
    }

    // This is a fallback because Discord is sometimes dumb
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getMessage().getContentRaw().startsWith("/minecraft-link")) {
            return;
        }

        String[] elements = event.getMessage().getContentRaw().split(" ");

        try {
            event.getMessage().delete().queue();
        } catch (Exception e) {
            // Ignore
        }

        User user = event.getAuthor();

        String code = elements.length > 1 ? elements[1] : null;

        user.openPrivateChannel().queue(channel -> {
            channel.sendMessageEmbeds(handleCommand(user, code)).queue();
        });
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("minecraft-link")) {
            return;
        }

        event.deferReply().setEphemeral(true).queue();

        String code = null;
        OptionMapping codeOption = event.getOption("code");
        if (codeOption != null) {
            code = codeOption.getAsString();
        }

        event.getHook().editOriginalEmbeds(handleCommand(event.getUser(), code)).queue();
    }

    public MessageEmbed handleCommand(User user, String code) {
        PlayerPreferences preferences = null;

        try {
            preferences = PlayerPreferencesManager.getPlayerPreferences(user);

            String debugMessage = "User " + user.getName() + " (" + user.getId() + ") is trying to link their Minecraft account with the code `" + code + "`";
            RedCraftChat.getInstance().getLogger().info(debugMessage);

            if (code != null && code.equalsIgnoreCase("unlink")) {
                if (preferences == null || preferences.discordId == null) {
                    return BasicMessageFormatter.generateDiscordError(user, "You are not linked to a Discord account, can't unlink");
                }
                AccountLinkManager.unLinkAccounts(preferences);
                return BasicMessageFormatter.generateDiscordMessage(user, "Success", "You have successfully unlinked your accounts", 0x00FF00);
            }

            if (preferences != null && preferences.minecraftUuid != null) {
                return BasicMessageFormatter.generateDiscordError(user, "You are already linked to a Minecraft account, if you wish to unlink it, use `/minecraft-link unlink`");
            }

            if (code != null) {
                if (AccountLinkManager.linkAccounts(preferences, code, user)) {
                    return BasicMessageFormatter.generateDiscordMessage(user, "Success", "You have successfully linked your accounts", 0x00FF00);
                }
                return BasicMessageFormatter.generateDiscordError(user, "Either the code is invalid or you're not connected to our Minecraft server");
            }

            AccountLinkCode linkCode = AccountLinkManager.getLinkCode(user);
            return BasicMessageFormatter.generateDiscordMessage(user, "Action required", "Please run the following command on our Minecraft server: `/discord-link " + linkCode.token + "`", 0x00FF00);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return BasicMessageFormatter.generateDiscordError(user, "An error occurred while trying to link your accounts, please try again later");
    }
}
