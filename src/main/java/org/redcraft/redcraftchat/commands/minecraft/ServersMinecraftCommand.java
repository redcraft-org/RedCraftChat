package org.redcraft.redcraftchat.commands.minecraft;

import java.util.ArrayList;
import java.util.List;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.commands.Suggestions;
import org.redcraft.redcraftchat.dialog.ServerSelectorDialog;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.minecraft.ServerDisplayNameManager;
import org.redcraft.redcraftchat.servers.ServerTransfer;

/**
 * Opens the server selector, or sends the player straight there when they
 * named one.
 *
 * Naming a server skips the dialog on purpose, the same rule the language and
 * mail commands follow: an argument means the player already knows what they
 * want, and putting a menu in front of that is a step backwards.
 */
public class ServersMinecraftCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        if (!(sender instanceof Player)) {
            BasicMessageFormatter.sendInternalError(sender, "This command can only be used by players");
            return;
        }

        Player player = (Player) sender;
        RedCraftChat plugin = RedCraftChat.getInstance();

        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            if (args.length > 0) {
                ServerTransfer.send(player, args[0]);
                return;
            }
            if (!ServerSelectorDialog.show(player)) {
                // No dialog to draw, so name the servers in chat instead of
                // leaving the command looking broken
                listInChat(player);
            }
        }).schedule();
    }

    /** The fallback for a client too old for dialogs. */
    private void listInChat(Player player) {
        StringBuilder line = new StringBuilder();
        for (RegisteredServer server : RedCraftChat.getInstance().getProxy().getAllServers()) {
            if (line.length() > 0) {
                line.append("§7, ");
            }
            line.append(ServerDisplayNameManager.getDisplayName(server.getServerInfo().getName()));
        }
        BasicMessageFormatter.sendInternalMessage(player, line.toString(),
                net.kyori.adventure.text.format.NamedTextColor.WHITE);
    }

    /**
     * Server names complete for everyone. They are the one argument here, and
     * unlike player names they are a fixed list, so they could also be hints;
     * they are left dynamic so a server added to the proxy shows up without a
     * restart of this plugin.
     */
    @Override
    public List<String> suggest(Invocation invocation) {
        List<String> names = new ArrayList<>();
        for (RegisteredServer server : RedCraftChat.getInstance().getProxy().getAllServers()) {
            names.add(server.getServerInfo().getName());
        }
        String[] args = invocation.arguments();
        if (Suggestions.wordIndex(args) > 0) {
            // One argument only, so nothing completes past it
            return java.util.Collections.emptyList();
        }
        return Suggestions.matching(names, Suggestions.currentWord(args));
    }
}
