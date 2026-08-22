package org.redcraft.redcraftchat.commands.minecraft;

import java.util.HashMap;
import java.util.Map;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.helpers.LegacyText;
import org.redcraft.redcraftchat.players.DisplayNameManager;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

public class MeMinecraftCommand implements SimpleCommand {

    public class MeMinecraftCommandHandler implements Runnable {
        CommandSource sender;
        String[] args;

        public MeMinecraftCommandHandler(CommandSource sender, String[] args) {
            this.sender = sender;
            this.args = args;
        }

        @Override
        public void run() {
            if (!(sender instanceof Player)) {
                BasicMessageFormatter.sendInternalError(sender, "This command can only be used by players");
                return;
            }

            Player player = (Player) sender;

            if (args.length < 1) {
                BasicMessageFormatter.sendInternalError(player, "You must specify a message");
                return;
            }

            String message = String.join(" ", args);
            Map<String, String> replacements = new HashMap<>();
            replacements.put("%player%", LegacyText.DARK_PURPLE + " * " + LegacyText.ITALIC + DisplayNameManager.getDisplayName(player) + LegacyText.LIGHT_PURPLE + LegacyText.ITALIC);
            MinecraftDiscordBridge.getInstance().broadcastMessage("%player% " + message, replacements, player);
        }
    }

    @Override
    public void execute(Invocation invocation) {
        var commandHandler = new MeMinecraftCommandHandler(invocation.source(), invocation.arguments());
        RedCraftChat.getInstance().getProxy().getScheduler().buildTask(RedCraftChat.getInstance(), commandHandler).schedule();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("redcraftchat.command.me");
    }
}
