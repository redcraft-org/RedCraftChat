package org.redcraft.redcraftchat.commands.discord;

import java.util.ArrayList;
import java.util.List;

import org.atteo.evo.inflector.English;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.helpers.LegacyText;
import org.redcraft.redcraftchat.players.DisplayNameManager;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import com.velocitypowered.api.proxy.Player;

public class PlayersDiscordCommand extends ListenerAdapter {

    public PlayersDiscordCommand() {
        DiscordClient.getClient().upsertCommand(Commands.slash("players", "List online players")).queue();
    }

    // This is a fallback because Discord is sometimes dumb
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getMessage().getContentRaw().startsWith("/players")) {
            return;
        }

        try {
            event.getMessage().delete().queue();
        } catch (Exception e) {
            // Ignore
        }

        User user = event.getAuthor();

        user.openPrivateChannel().queue(channel -> {
            channel.sendMessageEmbeds(handleCommand(user)).queue();
        });
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("players")) {
            return;
        }

        event.deferReply().setEphemeral(true).queue();

        event.getHook().editOriginalEmbeds(handleCommand(event.getUser())).queue();
    }

    public List<MessageEmbed> handleCommand(User user) {
        List<MessageEmbed> serverMessageEmbeds = new ArrayList<>();

        for (RegisteredServer server : RedCraftChat.getInstance().getProxy().getAllServers()) {

            // TODO better formatting
            String description = "";

            for (Player player : server.getPlayersConnected()) {
                description += "- " + LegacyText.stripColor(DisplayNameManager.getDisplayName(player)) + "\n";
            }

            int playerCount = server.getPlayersConnected().size();

            description += "\n*" + playerCount + " " + English.plural("player", playerCount) + " online*";

            description = PlayerPreferencesManager.localizeMessageForPlayer(user, description);

            MessageEmbed message = new EmbedBuilder()
                    .setTitle(LegacyText.stripColor(server.getServerInfo().getName()))
                    .setDescription(description)
                    .build();

            serverMessageEmbeds.add(message);
        }

        return serverMessageEmbeds;
    }
}
