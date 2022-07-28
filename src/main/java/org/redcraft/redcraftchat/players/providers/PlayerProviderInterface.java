package org.redcraft.redcraftchat.players.providers;

import java.io.IOException;

import org.redcraft.redcraftchat.models.players.PlayerPreferences;

import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;

interface PlayerProviderInterface {
    public PlayerPreferences getPlayerPreferences(ProxiedPlayer player, boolean createIfNotFound) throws IOException, InterruptedException;

    public PlayerPreferences getPlayerPreferences(User user, boolean createIfNotFound) throws IOException, InterruptedException;

    public void updatePlayerPreferences(PlayerPreferences preferences) throws IOException, InterruptedException;

    public void deletePlayerPreferences(PlayerPreferences preferences) throws IOException, InterruptedException;
}
