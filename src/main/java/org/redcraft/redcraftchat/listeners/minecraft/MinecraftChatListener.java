package org.redcraft.redcraftchat.listeners.minecraft;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;
import org.redcraft.redcraftchat.helpers.LegacyText;
import org.redcraft.redcraftchat.players.DisplayNameManager;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
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

    @Subscribe
    public void onChatEvent(PlayerChatEvent event) {
        if (!event.getResult().isAllowed()) {
            return;
        }

        Player player = event.getPlayer();

        String message = stripUnauthorizedFormatting(player, event.getMessage());

        // The message never reaches the backend, the proxy re-emits a translated
        // copy to every recipient itself
        event.setResult(PlayerChatEvent.ChatResult.denied());

        MinecraftDiscordBridge.getInstance().translateAndPostMessage(player, message);
    }

    /**
     * BungeeCord fired a single event for chat and commands alike, Velocity does
     * not, so the command spy half of the original listener lives here. The
     * command comes without its leading slash, it is added back so the spy output
     * keeps showing what the player actually typed.
     */
    @Subscribe
    public void onCommandExecute(CommandExecuteEvent event) {
        if (!event.getResult().isAllowed()) {
            return;
        }

        CommandSource source = event.getCommandSource();
        if (!(source instanceof Player)) {
            return;
        }

        handleCommandSpy((Player) source, "/" + event.getCommand());
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
        for (Player potentialStaffMember : RedCraftChat.getInstance().getProxy().getAllPlayers()) {
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
