package org.redcraft.redcraftchat.listeners.minecraft;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.redcraft.redcraftchat.helpers.LegacyText;
import org.redcraft.redcraftchat.players.DisplayNameManager;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MinecraftChatListener {

    private List<String> stylingCodes = Arrays.asList(
        LegacyText.BOLD,
        LegacyText.ITALIC,
        LegacyText.STRIKETHROUGH,
        LegacyText.UNDERLINE
    );

    @Subscribe(order = PostOrder.NORMAL)
    public void onChatEvent(PlayerChatEvent event) {
        // TODO wave 2: port the chat pipeline. On Velocity, cancelling and reposting
        // player chat conflicts with signed messages, and commands do not go through
        // PlayerChatEvent (command spy will move to CommandExecuteEvent).
        // The BungeeCord version stripped unauthorized formatting with the
        // redcraftchat.formatting.* permissions then called
        // MinecraftDiscordBridge.getInstance().translateAndPostMessage(player, message)
    }

    public String stripUnauthorizedFormatting(Player player, String rawMessage) {
        String message = LegacyText.translateAlternateColorCodes('&', rawMessage);

        if (!player.hasPermission("redcraftchat.formatting.colors")) {
            message = LegacyText.stripColor(message);
        }

        if (!player.hasPermission("redcraftchat.formatting.styling")) {
            for (String bannedCode : stylingCodes) {
                message = message.replace(bannedCode, "");
            }
        }

        if (!player.hasPermission("redcraftchat.formatting.magic")) {
            message = message.replace(LegacyText.MAGIC, "");
        }

        return message;
    }

    public void handleCommandSpy(Player player, String message) {
        for (Player potentialStaffMember : org.redcraft.redcraftchat.RedCraftChat.getInstance().getProxy().getAllPlayers()) {
            if (!player.equals(potentialStaffMember) && potentialStaffMember.hasPermission("redcraftchat.moderation.commandspy")) {
                PlayerPreferences playerPreferences;
                try {
                    playerPreferences = PlayerPreferencesManager.getPlayerPreferences(potentialStaffMember);
                    if (playerPreferences.commandSpyEnabled) {
                        String formattedMessage = LegacyText.AQUA + "[CSPY][" + DisplayNameManager.getDisplayName(player) + LegacyText.AQUA + "] " + message;
                        potentialStaffMember.sendMessage(LegacyComponentSerializer.legacySection().deserialize(formattedMessage));
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
