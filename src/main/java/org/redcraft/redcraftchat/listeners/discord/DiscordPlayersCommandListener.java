package org.redcraft.redcraftchat.listeners.discord;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.atteo.evo.inflector.English;
import org.redcraft.redcraftchat.RedCraftChat;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class DiscordPlayersCommandListener extends ListenerAdapter {

    @Override
    public void onSlashCommand(SlashCommandEvent event) {
        if (!event.getName().equals("players")) {
            return;
        }

        List<MessageEmbed> serverMessageEmbeds = new ArrayList<MessageEmbed>();

        for (ServerInfo server : RedCraftChat.getInstance().getProxy().getServers().values()) {

            // TODO better formatting
            String description = "";

            for (ProxiedPlayer player : server.getPlayers()) {
                description += "- " + player.getDisplayName() + "\n";
            }

            int playerCount = server.getPlayers().size();

            description += "\n*" + playerCount + " " + English.plural("player", playerCount) + " online*";

            MessageEmbed message = new EmbedBuilder()
                .setTitle(server.getName())
                .setDescription(description)
                .build();

            serverMessageEmbeds.add(message);
        }

        event.replyEmbeds(serverMessageEmbeds).queue();
    }
}
