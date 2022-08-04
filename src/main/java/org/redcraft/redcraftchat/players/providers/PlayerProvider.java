package org.redcraft.redcraftchat.players.providers;

import java.io.IOException;
import java.util.UUID;

import org.redcraft.redcraftchat.models.players.PlayerPreferences;

import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public interface PlayerProvider {
    public PlayerPreferences getPlayerPreferences(ProxiedPlayer player, boolean createIfNotFound) throws IOException, InterruptedException;

    public PlayerPreferences getPlayerPreferences(User user, boolean createIfNotFound) throws IOException, InterruptedException;

    public PlayerPreferences getPlayerPreferences(UUID player) throws IOException, InterruptedException;

    public PlayerPreferences getPlayerPreferences(String username, boolean searchMinecraft, boolean searchDiscord) throws IOException, InterruptedException;

    public void updatePlayerPreferences(PlayerPreferences preferences) throws IOException, InterruptedException;

    public void deletePlayerPreferences(PlayerPreferences preferences) throws IOException, InterruptedException;
}
