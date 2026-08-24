package org.redcraft.redcraftchat.commands.minecraft;

import org.redcraft.redcraftchat.RedCraftChat;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

/**
 * Runs another command in place of the one that was typed.
 *
 * Velocity has no alias mechanism of its own, its config carries nothing of the
 * sort and only the plugin API can name a command, so an alias has to be a
 * command that dispatches the real one. Anything after the alias is appended,
 * so an alias to "server" can still be given a server name.
 */
public class AliasMinecraftCommand implements SimpleCommand {

    private final String target;

    public AliasMinecraftCommand(String target) {
        this.target = target;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        String command = target;

        if (args.length > 0) {
            command = command + " " + String.join(" ", args);
        }

        // Dispatched as the sender, so the target command runs its own
        // permission check and the alias grants nothing on its own
        RedCraftChat.getInstance().getProxy().getCommandManager().executeAsync(sender, command);
    }
}
