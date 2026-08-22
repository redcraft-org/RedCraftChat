package org.redcraft.redcraftchat.commands.minecraft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.Component;

public class LangMinecraftCommand implements SimpleCommand {

    public class LangMinecraftCommandHandler implements Runnable {
        CommandSource sender;
        String[] args;

        public LangMinecraftCommandHandler(CommandSource sender, String[] args) {
            this.sender = sender;
            this.args = args;
        }

        @Override
        public void run() {
            // If it's not a player we need an arg
            if (!(sender instanceof Player)) {
                BasicMessageFormatter.sendInternalError(sender, "This command can only be used by players");
                return;
            }

            Player player = (Player) sender;

            try {
                PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);

                if (args.length > 0) {
                    if (args.length > 1 && args[1].equals("main")) {
                        PlayerPreferencesManager.setMainPlayerLocale(preferences, args[0]);
                    } else {
                        toggleLocale(preferences, player, args[0]);
                    }
                    preferences = PlayerPreferencesManager.getPlayerPreferences(player);
                }

                for (Component message : generateMenu(preferences)) {
                    player.sendMessage(message);
                }
            } catch (IOException | InterruptedException e) {
                BasicMessageFormatter.sendInternalError(player, "An error occurred while trying to load or change languages, please try again later");
                e.printStackTrace();
            }
        }
    }

    private List<Component> generateMenu(PlayerPreferences preferences) {
        // TODO wave 2: port the interactive language selector menu
        // (header, caption, per locale toggle with hover tooltips and run-command
        // click events) from the BungeeCord ComponentBuilder API to Adventure
        return new ArrayList<Component>();
    }

    private void toggleLocale(PlayerPreferences preferences, Player player, String locale) {
        try {
            PlayerPreferencesManager.togglePlayerLocale(preferences, locale);
        } catch (IllegalArgumentException | IllegalStateException e) {
            BasicMessageFormatter.sendInternalError(player, e.getMessage());
        }
    }

    @Override
    public void execute(Invocation invocation) {
        var commandHandler = new LangMinecraftCommandHandler(invocation.source(), invocation.arguments());
        RedCraftChat.getInstance().getProxy().getScheduler().buildTask(RedCraftChat.getInstance(), commandHandler).schedule();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("redcraftchat.command.lang");
    }
}
