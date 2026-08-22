package org.redcraft.redcraftchat.commands.minecraft;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.bridge.MinecraftDiscordBridge;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.helpers.LegacyText;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

public class BroadcastMinecraftCommand implements SimpleCommand {

    public class BroadcastMinecraftCommandHandler implements Runnable {
        CommandSource sender;
        String[] args;

        public BroadcastMinecraftCommandHandler(CommandSource sender, String[] args) {
            this.sender = sender;
            this.args = args;
        }

        @Override
        public void run() {
            if (args.length < 1) {
                BasicMessageFormatter.sendInternalError(sender, "You must specify a message");
                return;
            }

            String message = String.join(" ", args);

            // TODO embeds
            MinecraftDiscordBridge.getInstance().broadcastMessage(LegacyText.DARK_RED + LegacyText.BOLD + "[Alert]" + LegacyText.RESET + " " + LegacyText.YELLOW + message);
        }
    }

    @Override
    public void execute(Invocation invocation) {
        var commandHandler = new BroadcastMinecraftCommandHandler(invocation.source(), invocation.arguments());
        RedCraftChat.getInstance().getProxy().getScheduler().buildTask(RedCraftChat.getInstance(), commandHandler).schedule();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("redcraftchat.command.broadcast");
    }
}
