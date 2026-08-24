package org.redcraft.redcraftchat.api;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.LegacyText;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.DisplayNameManager;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

/**
 * The players currently online, grouped by the server they are on.
 *
 * The BungeeCord version refreshed this on a timer and served the last
 * snapshot. Velocity can be asked directly, so the list is built when the
 * request comes in and there is no stale window.
 */
public class PlayerList {

    public Hashtable<String, ArrayList<PlayerInfo>> players = new Hashtable<String, ArrayList<PlayerInfo>>();

    public static PlayerList build() {
        PlayerList playerList = new PlayerList();

        for (RegisteredServer server : RedCraftChat.getInstance().getProxy().getAllServers()) {
            ArrayList<PlayerInfo> serverPlayers = new ArrayList<PlayerInfo>();

            for (Player player : server.getPlayersConnected()) {
                String mainLanguage = null;
                List<String> languages = null;

                try {
                    PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);

                    if (preferences != null) {
                        mainLanguage = preferences.mainLanguage;
                        languages = preferences.languages;
                    }
                } catch (Exception e) {
                    // A player whose preferences cannot be read is still online,
                    // so report them without a language rather than dropping them
                }

                // The section sign is swapped for an ampersand, the website has
                // always been given the display name in that form
                String displayName = DisplayNameManager.getDisplayName(player)
                        .replace(LegacyText.COLOR_CHAR, '&');

                serverPlayers.add(new PlayerInfo(player.getUniqueId(), player.getUsername(), displayName,
                        mainLanguage, languages));
            }

            playerList.players.put(server.getServerInfo().getName(), serverPlayers);
        }

        return playerList;
    }

    public String getOnlinePlayersJson() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.toJson(this);
    }
}
