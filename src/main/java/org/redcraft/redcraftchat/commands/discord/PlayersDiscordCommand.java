package org.redcraft.redcraftchat.commands.discord;

import java.util.ArrayList;
import java.util.List;

import org.atteo.evo.inflector.English;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

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

        event.getAuthor().openPrivateChannel().queue(channel -> {
            channel.sendMessageEmbeds(handleCommand(user)).queue();
        });
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("players")) {
            return;
        }

        event.replyEmbeds(handleCommand(event.getUser())).queue();
    }

    public List<MessageEmbed> handleCommand(User user) {
        List<MessageEmbed> serverMessageEmbeds = new ArrayList<>();

        for (ServerInfo server : RedCraftChat.getInstance().getProxy().getServers().values()) {

            // TODO better formatting
            String description = "";

            for (ProxiedPlayer player : server.getPlayers()) {
                description += "- " + player.getDisplayName() + "\n";
            }

            int playerCount = server.getPlayers().size();

            description += "\n*" + playerCount + " " + English.plural("player", playerCount) + " online*";

            description = PlayerPreferencesManager.localizeMessageForPlayer(user, description);

            MessageEmbed message = new EmbedBuilder()
                    .setTitle(server.getName())
                    .setDescription(description)
                    .build();

            serverMessageEmbeds.add(message);
        }

        return serverMessageEmbeds;
    }
}
