package org.redcraft.redcraftchat.players.sources;

import java.io.IOException;

import org.redcraft.redcraftchat.models.players.PlayerPreferences;

import net.dv8tion.jda.api.entities.User;
import net.md_5.bungee.api.connection.ProxiedPlayer;

interface PlayerSourceInterface {
    public PlayerPreferences getPlayerPreferences(ProxiedPlayer player) throws IOException, InterruptedException;

    public PlayerPreferences getPlayerPreferences(User user) throws IOException, InterruptedException;
    public void updatePlayerPreferences(PlayerPreferences preferences) throws IOException, InterruptedException;
}
