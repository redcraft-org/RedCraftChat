package org.redcraft.redcraftchat.commands.minecraft;

import java.io.IOException;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.google.gson.Gson;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.format.NamedTextColor;

public class PlayerSettingsMinecraftCommand implements SimpleCommand {

    public class PlayerSettingsMinecraftCommandHandler implements Runnable {
        CommandSource sender;
        String[] args;

        public PlayerSettingsMinecraftCommandHandler(CommandSource sender, String[] args) {
            this.sender = sender;
            this.args = args;
        }

        @Override
        public void run() {
            Player player = null;

            // If it's not a player we need an arg
            if (!(sender instanceof Player) && args.length < 1) {
                BasicMessageFormatter.sendInternalError(sender, "You need to specify a player name");
                return;
            }

            // Get from arg
            if (args.length > 0 && sender.hasPermission("redcraftchat.command.player-settings.others")) {
                player = RedCraftChat.getInstance().getProxy().getPlayer(args[0]).orElse(null);
            } else {
                player = (Player) sender;
            }

            if (player == null) {
                BasicMessageFormatter.sendInternalError(sender, "The specified player doesn't seem to be online");
                return;
            }

            try {
                PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
                BasicMessageFormatter.sendInternalMessage(sender, "Current settings: " + new Gson().toJson(preferences), NamedTextColor.GOLD);
            } catch (IOException | InterruptedException e) {
                BasicMessageFormatter.sendInternalError(sender, "An error occured while trying to display player settings, check logs for more info");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void execute(Invocation invocation) {
        var commandHandler = new PlayerSettingsMinecraftCommandHandler(invocation.source(), invocation.arguments());
        RedCraftChat.getInstance().getProxy().getScheduler().buildTask(RedCraftChat.getInstance(), commandHandler).schedule();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("redcraftchat.command.player-settings");
    }
}
