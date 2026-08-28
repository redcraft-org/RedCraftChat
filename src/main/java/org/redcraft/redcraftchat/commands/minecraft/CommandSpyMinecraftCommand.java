package org.redcraft.redcraftchat.commands.minecraft;

import java.io.IOException;
import org.redcraft.redcraftchat.commands.Suggestions;
import java.util.List;
import java.util.ArrayList;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.format.NamedTextColor;

public class CommandSpyMinecraftCommand implements SimpleCommand {

    public class CommandSpyMinecraftCommandHandler implements Runnable {
        CommandSource sender;
        String[] args;

        public CommandSpyMinecraftCommandHandler(CommandSource sender, String[] args) {
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
            if (args.length > 0 && sender.hasPermission("redcraftchat.moderation.commandspy.others")) {
                player = RedCraftChat.getInstance().getProxy().getPlayer(args[0]).orElse(null);
            } else {
                player = (Player) sender;
            }

            if (player == null) {
                BasicMessageFormatter.sendInternalError(sender, "The specified player doesn't seem to be online");
                return;
            }

            if (!player.hasPermission("redcraftchat.moderation.commandspy")) {
                BasicMessageFormatter.sendInternalError(sender,
                        "This player does not have the permission to use command spy");
                return;
            }

            try {
                boolean commandSpyEnabled = PlayerPreferencesManager.toggleCommandSpy(player);
                BasicMessageFormatter.sendInternalMessage(sender,
                        "Command spy " + (commandSpyEnabled ? "enabled" : "disabled"), NamedTextColor.GREEN);
            } catch (IOException | InterruptedException e) {
                BasicMessageFormatter.sendInternalError(sender,
                        "An error occured while trying to toggle command spy, check logs for more info");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void execute(Invocation invocation) {
        var commandHandler = new CommandSpyMinecraftCommandHandler(invocation.source(), invocation.arguments());
        RedCraftChat.getInstance().getProxy().getScheduler().buildTask(RedCraftChat.getInstance(), commandHandler).schedule();
    }


    @Override
    public List<String> suggest(Invocation invocation) {
        // Re-checked here on purpose. Velocity gates suggest() on the same
        // hasPermission as execute(), which is the top-level node, so without
        // this the command would quietly complete a roster of player names for
        // somebody who is not allowed to target anybody.
        if (!invocation.source().hasPermission("redcraftchat.moderation.commandspy.others")) {
            return java.util.List.of();
        }
        if (Suggestions.wordIndex(invocation.arguments()) > 0) {
            return java.util.List.of();
        }
        List<String> names = new ArrayList<>();
        for (Player online : RedCraftChat.getInstance().getProxy().getAllPlayers()) {
            names.add(online.getUsername());
        }
        return Suggestions.matching(names, Suggestions.currentWord(invocation.arguments()));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("redcraftchat.moderation.commandspy");
    }
}
