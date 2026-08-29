package org.redcraft.redcraftchat.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.retrooper.packetevents.protocol.dialog.CommonDialogData;
import com.github.retrooper.packetevents.protocol.dialog.DialogAction;
import com.github.retrooper.packetevents.protocol.dialog.MultiActionDialog;
import com.github.retrooper.packetevents.protocol.dialog.action.DynamicCustomAction;
import com.github.retrooper.packetevents.protocol.dialog.body.DialogBody;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessage;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessageDialogBody;
import com.github.retrooper.packetevents.protocol.dialog.button.ActionButton;
import com.github.retrooper.packetevents.protocol.dialog.button.CommonButtonData;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.listeners.minecraft.MinecraftLastServerListener;
import org.redcraft.redcraftchat.locales.UiStrings;
import org.redcraft.redcraftchat.minecraft.ServerDisplayNameManager;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;
import org.redcraft.redcraftchat.servers.LoginServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Where to go, as a dialog.
 *
 * The transfer is done with the proxy's own API rather than by running
 * /server for the player. That command lives on the proxy and only works
 * there, and this code is already on the proxy, so going through a command
 * would mean asking a backend to ask us to do what we can do directly.
 *
 * The last server they were on gets its own button at the top, because the
 * common move is going back to what you were doing rather than picking from
 * a list you have read many times before.
 */
public final class ServerSelectorDialog {

    public static final ResourceLocation PICK = new ResourceLocation("redcraftchat", "server_pick");
    public static final ResourceLocation BACK = new ResourceLocation("redcraftchat", "server_list");
    public static final ResourceLocation DEFAULTS = new ResourceLocation("redcraftchat", "server_defaults");
    public static final ResourceLocation SET_DEFAULT = new ResourceLocation("redcraftchat", "server_set_default");

    /** Matches the dialog input key rule, and is what the payload carries. */
    public static final String SERVER_KEY = "server";

    private static final int BUTTON_WIDTH_PX = 200;
    private static final int BODY_WIDTH_PX = 320;

    private ServerSelectorDialog() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static boolean isSupported(Player player) {
        return NativeDialogSelector.isSupported(player);
    }

    public static boolean show(Player player) {
        if (!isSupported(player)) {
            return false;
        }
        try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            String current = player.getCurrentServer()
                    .map(connection -> connection.getServerInfo().getName())
                    .orElse(null);

            List<DialogBody> body = new ArrayList<>();
            List<ActionButton> buttons = new ArrayList<>();

            // Where they are, so the list below reads as "somewhere else"
            if (current != null) {
                body.add(new PlainMessageDialogBody(new PlainMessage(
                        Component.text()
                                .append(Component.text(ui(preferences, UiStrings.SERVERS_YOU_ARE_HERE) + "  ",
                                        NamedTextColor.GRAY))
                                .append(name(current))
                                .build(),
                        BODY_WIDTH_PX)));
            }
            body.add(new PlainMessageDialogBody(new PlainMessage(
                    Component.text(ui(preferences, UiStrings.SERVERS_HELP), NamedTextColor.GRAY),
                    BODY_WIDTH_PX)));

            // The way back comes first and is the only coloured one, because
            // it is the button most players came here to press
            RegisteredServer back = MinecraftLastServerListener.returnTarget(player, preferences);
            if (back != null) {
                String backName = back.getServerInfo().getName();
                buttons.add(button(
                        Component.text()
                                .append(Component.text(
                                        ui(preferences, UiStrings.SERVERS_RETURN).replace("%server%", ""),
                                        NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                                .append(name(backName))
                                .build(),
                        backName));
            }

            for (RegisteredServer server : RedCraftChat.getInstance().getProxy().getAllServers()) {
                String serverName = server.getServerInfo().getName();
                if (serverName.equals(current)) {
                    continue;
                }
                if (back != null && serverName.equals(back.getServerInfo().getName())) {
                    // Already offered above, and twice would be worse than once
                    continue;
                }
                buttons.add(button(name(serverName), serverName));
            }

            if (buttons.isEmpty()) {
                body.add(new PlainMessageDialogBody(new PlainMessage(
                        Component.text(ui(preferences, UiStrings.SERVERS_NONE), NamedTextColor.GRAY),
                        BODY_WIDTH_PX)));
            }

            // The setting says what it currently is on its face, so nobody
            // has to open it to find out where they land
            buttons.add(new ActionButton(
                    new CommonButtonData(
                            Component.text()
                                    .append(Component.text(
                                            ui(preferences, UiStrings.SERVERS_DEFAULT_BUTTON)
                                                    .replace("%server%", ""),
                                            NamedTextColor.GRAY))
                                    .append(currentDefault(preferences))
                                    .build(),
                            null, BUTTON_WIDTH_PX),
                    new DynamicCustomAction(DEFAULTS, null)));

            ActionButton close = new ActionButton(
                    new CommonButtonData(
                            Component.text(ui(preferences, UiStrings.SELECTOR_CLOSE)),
                            null, BUTTON_WIDTH_PX),
                    null);

            CommonDialogData common = new CommonDialogData(
                    Component.text(ui(preferences, UiStrings.SERVERS_TITLE), NamedTextColor.GOLD)
                            .decorate(TextDecoration.BOLD),
                    null, true,
                    // Not a pausing dialog: PacketEvents rejects pause with an
                    // after_action that does not unpause
                    false,
                    DialogAction.CLOSE, body, Collections.emptyList());

            return NativeDialogSelector.send(player, new MultiActionDialog(common, buttons, close, 1));
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().warn("Could not open the server selector for {}: {}",
                    player.getUsername(), e.getMessage());
            return false;
        }
    }

    /** The setting's own screen: the two rules, then every server. */
    public static boolean showDefaults(Player player) {
        if (!isSupported(player)) {
            return false;
        }
        try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);

            List<DialogBody> body = new ArrayList<>();
            body.add(new PlainMessageDialogBody(new PlainMessage(
                    Component.text(
                            ui(preferences, UiStrings.SERVERS_DEFAULT_HELP)
                                    .replace("%network%", Config.networkName),
                            NamedTextColor.GRAY),
                    BODY_WIDTH_PX)));

            List<ActionButton> buttons = new ArrayList<>();
            buttons.add(choice(preferences, UiStrings.SERVERS_DEFAULT_LAST, LoginServer.LAST,
                    LoginServer.LAST.equals(preferences.loginServer)));

            // With nothing set the player lands wherever the proxy's try list
            // puts them, so the tick goes on that server rather than on an
            // extra "let the network decide" row that only ever meant it
            String unset = effectiveDefault();
            for (RegisteredServer server : RedCraftChat.getInstance().getProxy().getAllServers()) {
                String serverName = server.getServerInfo().getName();
                boolean chosen = serverName.equals(preferences.loginServer)
                        || (preferences.loginServer == null && serverName.equals(unset));
                NBTCompound additions = new NBTCompound();
                additions.setTag(SERVER_KEY, new NBTString(serverName));
                buttons.add(new ActionButton(
                        new CommonButtonData(tick(name(serverName), chosen), null, BUTTON_WIDTH_PX),
                        new DynamicCustomAction(SET_DEFAULT, additions)));
            }

            ActionButton back = new ActionButton(
                    new CommonButtonData(
                            Component.text(ui(preferences, UiStrings.SELECTOR_BACK)),
                            null, BUTTON_WIDTH_PX),
                    new DynamicCustomAction(BACK, null));

            CommonDialogData common = new CommonDialogData(
                    Component.text(ui(preferences, UiStrings.SERVERS_DEFAULT_TITLE), NamedTextColor.GOLD)
                            .decorate(TextDecoration.BOLD),
                    null, true, false, DialogAction.CLOSE, body, Collections.emptyList());

            return NativeDialogSelector.send(player, new MultiActionDialog(common, buttons, back, 1));
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().warn("Could not open the login server picker for {}: {}",
                    player.getUsername(), e.getMessage());
            return false;
        }
    }

    private static ActionButton choice(PlayerPreferences preferences, String label, String value,
            boolean chosen) {
        NBTCompound additions = new NBTCompound();
        additions.setTag(SERVER_KEY, new NBTString(value));
        return new ActionButton(
                new CommonButtonData(
                        tick(Component.text(ui(preferences, label), NamedTextColor.WHITE), chosen),
                        null, BUTTON_WIDTH_PX),
                new DynamicCustomAction(SET_DEFAULT, additions));
    }

    /** The one in force is marked, so the screen answers before it is read. */
    private static Component tick(Component label, boolean chosen) {
        if (!chosen) {
            return label;
        }
        return Component.text().append(label)
                .append(Component.text("  \u2714", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .build();
    }

    /** What the setting reads as today, for the button that opens it. */
    private static Component currentDefault(PlayerPreferences preferences) {
        String setting = preferences.loginServer;
        if (LoginServer.LAST.equals(setting)) {
            return Component.text(ui(preferences, UiStrings.SERVERS_DEFAULT_LAST), NamedTextColor.WHITE);
        }
        if (setting == null || setting.isEmpty()) {
            String unset = effectiveDefault();
            return unset == null ? Component.empty() : name(unset);
        }
        return name(setting);
    }

    /** Where an unset player lands, read from the proxy rather than assumed. */
    private static String effectiveDefault() {
        List<String> known = new ArrayList<>();
        for (RegisteredServer server : RedCraftChat.getInstance().getProxy().getAllServers()) {
            known.add(server.getServerInfo().getName());
        }
        return LoginServer.effectiveDefault(
                RedCraftChat.getInstance().getProxy().getConfiguration().getAttemptConnectionOrder(),
                known);
    }

    private static ActionButton button(Component label, String serverName) {
        NBTCompound additions = new NBTCompound();
        additions.setTag(SERVER_KEY, new NBTString(serverName));
        return new ActionButton(
                new CommonButtonData(label, null, BUTTON_WIDTH_PX),
                new DynamicCustomAction(PICK, additions));
    }

    /**
     * The configured readable name, which carries legacy colour codes, turned
     * back into a component so the dialog keeps the colours the rest of the
     * plugin already gives each server.
     */
    private static Component name(String serverName) {
        return LegacyComponentSerializer.legacySection()
                .deserialize(ServerDisplayNameManager.getDisplayName(serverName));
    }

    private static String ui(PlayerPreferences preferences, String message) {
        return PlayerPreferencesManager.localizeUiForPlayer(preferences, message);
    }
}
