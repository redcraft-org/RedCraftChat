package org.redcraft.redcraftchat.dialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.dialog.CommonDialogData;
import com.github.retrooper.packetevents.protocol.dialog.Dialog;
import com.github.retrooper.packetevents.protocol.dialog.DialogAction;
import com.github.retrooper.packetevents.protocol.dialog.MultiActionDialog;
import com.github.retrooper.packetevents.protocol.dialog.action.Action;
import com.github.retrooper.packetevents.protocol.dialog.action.DynamicCustomAction;
import com.github.retrooper.packetevents.protocol.dialog.body.DialogBody;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessage;
import com.github.retrooper.packetevents.protocol.dialog.body.PlainMessageDialogBody;
import com.github.retrooper.packetevents.protocol.dialog.button.ActionButton;
import com.github.retrooper.packetevents.protocol.dialog.button.CommonButtonData;
import com.github.retrooper.packetevents.protocol.dialog.input.BooleanInputControl;
import com.github.retrooper.packetevents.protocol.dialog.input.Input;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerClearDialog;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerShowDialog;
import com.velocitypowered.api.proxy.Player;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.locales.LocaleManager;
import org.redcraft.redcraftchat.locales.UiStrings;
import org.redcraft.redcraftchat.models.locales.SupportedLocale;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.kyori.adventure.text.Component;

/**
 * The language selector as a native Minecraft dialog.
 *
 * The client draws it, so there is no surface to place, no text to wrap, no
 * hover to track and nothing to swallow the player's clicks. It is also a
 * lower floor than the in-world panel: dialogs arrived in 1.21.6, display
 * entity surfaces need roughly 1.21.9.
 *
 * The dialogs are built unregistered, which is what makes this possible from
 * a proxy at all: PacketEvents writes an unregistered dialog inline into the
 * packet instead of referencing a registry entry, so nothing has to be
 * declared during configuration and no backend plugin is involved.
 */
public final class NativeDialogSelector {

    /** Ids the client echoes back in its custom click action. */
    public static final ResourceLocation PICK_PRIMARY =
            new ResourceLocation("redcraftchat", "pick_primary");
    public static final ResourceLocation CONFIRM_OTHERS =
            new ResourceLocation("redcraftchat", "confirm_others");
    public static final ResourceLocation BACK_TO_PRIMARY =
            new ResourceLocation("redcraftchat", "back_to_primary");
    public static final ResourceLocation GO_TO_OTHERS =
            new ResourceLocation("redcraftchat", "go_to_others");

    /** The payload key carrying the language a primary button stands for. */
    public static final String LANGUAGE_KEY = "language";

    /** Prefix for the per-language toggles on the second step. */
    public static final String UNDERSTOOD_PREFIX = "understood_";

    /**
     * The characters Minecraft allows in a dialog input key: the resource
     * path set. A key outside it makes the client fail to decode the whole
     * packet and drop the connection, so this is a kick, not a glitch.
     */
    private static final java.util.regex.Pattern INPUT_KEY =
            java.util.regex.Pattern.compile("[a-z0-9_.-]+");

    /**
     * Builds the toggle key for a language. Locale codes are mixed case
     * ("fr-FR") and the key charset is lowercase only, so the case has to go
     * before it reaches the wire.
     */
    public static String understoodKey(String localeCode) {
        return UNDERSTOOD_PREFIX + localeCode.toLowerCase(java.util.Locale.ROOT).replace('-', '_');
    }

    /**
     * The language this flow filed under "understood" purely because it was
     * picked as primary, per player. Remembered so that changing the primary
     * takes the old trial back out, instead of leaving behind a language the
     * player only passed through on the way to another one.
     */
    private static final java.util.Map<java.util.UUID, String> AUTO_ADDED =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final int BUTTON_WIDTH_PX = 150;
    private static final int BODY_WIDTH_PX = 300;

    private NativeDialogSelector() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    /**
     * Dialogs are a 1.21.6 feature and PacketEvents does not guard the send.
     * Below that version the packet has no id, so it goes out as id -1 and
     * the client drops the connection: this check is the only thing standing
     * between an old client and a kick.
     */
    public static boolean isSupported(Player player) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) {
            return false;
        }
        ClientVersion version = user.getClientVersion();
        return version != null && version.isNewerThanOrEquals(ClientVersion.V_1_21_6);
    }

    /**
     * Step one: one button per language. Blocking, so call it off netty.
     *
     * @return false when the dialog could not be built or sent, so a caller
     * can say so instead of leaving the player looking at nothing.
     */
    public static boolean showPrimary(Player player, PlayerPreferences preferences) {
        try {
            return buildPrimary(player, preferences);
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().error("Could not build the primary language dialog for {}: {}",
                    player.getUsername(), e.getMessage());
            return false;
        }
    }

    private static boolean buildPrimary(Player player, PlayerPreferences preferences) {
        List<ActionButton> buttons = new ArrayList<>();
        for (SupportedLocale locale : LocaleManager.getSupportedLocales()) {
            boolean isMain = locale.code.equalsIgnoreCase(preferences.mainLanguage);
            String label = isMain
                    ? LocaleManager.getEndonym(locale) + " ✔"
                    : LocaleManager.getEndonym(locale);

            NBTCompound additions = new NBTCompound();
            additions.setTag(LANGUAGE_KEY, new NBTString(locale.code));

            Action action = new DynamicCustomAction(PICK_PRIMARY, additions);
            buttons.add(new ActionButton(
                    new CommonButtonData(Component.text(label), null, BUTTON_WIDTH_PX),
                    action));
        }


        CommonDialogData common = new CommonDialogData(
                Component.text(localize(preferences, UiStrings.SELECTOR_PRIMARY_TITLE)),
                null,
                true,
                // Not a pausing dialog. PacketEvents rejects pause with an
                // after_action that does not unpause, and this one has to
                // stay up across a click so the answer can replace it.
                false,
                // NONE, not WAIT_FOR_RESPONSE: the latter freezes the client on
                // a "Waiting for Server" screen from the moment of the click
                // until we answer, and answering means a database write and a
                // round of translation. Leaving the dialog up means the
                // replacement simply swaps in when it is ready.
                DialogAction.NONE,
                body(preferences, UiStrings.SELECTOR_PRIMARY_HELP),
                Collections.emptyList());

        // The advance button goes in the exit slot rather than the grid: the
        // client draws that one apart from the list, which is the gap between
        // the languages and the way forward.
        ActionButton advance = new ActionButton(
                new CommonButtonData(
                        Component.text(localize(preferences, UiStrings.SELECTOR_CONTINUE)),
                        null,
                        BUTTON_WIDTH_PX),
                new DynamicCustomAction(GO_TO_OTHERS, null));

        return send(player, new MultiActionDialog(common, buttons, advance, 1));
    }

    /**
     * Step two: a toggle per language, with the main one left out because
     * understanding it is implied and togglePlayerLocale refuses to remove it.
     */
    public static boolean showOthers(Player player, PlayerPreferences preferences) {
        try {
            return buildOthers(player, preferences);
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().error("Could not build the secondary language dialog for {}: {}",
                    player.getUsername(), e.getMessage());
            return false;
        }
    }

    private static boolean buildOthers(Player player, PlayerPreferences preferences) {
        List<Input> inputs = new ArrayList<>();
        for (SupportedLocale locale : LocaleManager.getSupportedLocales()) {
            if (locale.code.equalsIgnoreCase(preferences.mainLanguage)) {
                continue;
            }
            boolean understood = preferences.languages != null
                    && preferences.languages.contains(locale.code);
            inputs.add(new Input(
                    understoodKey(locale.code),
                    new BooleanInputControl(
                            Component.text(LocaleManager.getEndonym(locale)),
                            understood,
                            "true",
                            "false")));
        }

        // "Submit" and "Change main language" say what they do without
        // relying on the reader knowing where they are. Next, Done and Back
        // are each one word whose meaning lives in the surrounding screen,
        // which is exactly what a translator never sees.
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(new ActionButton(
                new CommonButtonData(
                        Component.text(localize(preferences, UiStrings.SELECTOR_SUBMIT)),
                        null,
                        BUTTON_WIDTH_PX),
                new DynamicCustomAction(CONFIRM_OTHERS, null)));

        ActionButton back = new ActionButton(
                new CommonButtonData(
                        Component.text(localize(preferences, UiStrings.CHANGE_MAIN_LANGUAGE)),
                        null,
                        BUTTON_WIDTH_PX),
                new DynamicCustomAction(BACK_TO_PRIMARY, null));

        CommonDialogData common = new CommonDialogData(
                Component.text(localize(preferences, UiStrings.SELECTOR_OTHERS_TITLE)),
                null,
                true,
                false,
                // Same reason as the first step; the confirm handler sends an
                // explicit clear, since NONE leaves the dialog standing
                DialogAction.NONE,
                body(preferences, UiStrings.SELECTOR_OTHERS_HELP),
                inputs);

        return send(player, new MultiActionDialog(common, buttons, back, 1));
    }

    /** Records what setting this primary added on the player's behalf. */
    public static void rememberAutoAdded(java.util.UUID playerId, String autoAdded) {
        if (autoAdded == null) {
            AUTO_ADDED.remove(playerId);
        } else {
            AUTO_ADDED.put(playerId, autoAdded);
        }
    }

    public static String autoAdded(java.util.UUID playerId) {
        return AUTO_ADDED.get(playerId);
    }

    public static void forget(java.util.UUID playerId) {
        AUTO_ADDED.remove(playerId);
    }

    /** Dismisses whatever dialog the player currently has open. */
    public static void clear(Player player) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null || !isSupported(player)) {
            return;
        }
        user.sendPacketSilently(new WrapperPlayServerClearDialog());
    }

    private static List<DialogBody> body(PlayerPreferences preferences, String key) {
        return Collections.singletonList(new PlainMessageDialogBody(
                new PlainMessage(Component.text(localize(preferences, key)), BODY_WIDTH_PX)));
    }

    /** The first input key the client would reject, or null when all are fine. */
    private static String firstInvalidKey(Dialog dialog) {
        if (!(dialog instanceof MultiActionDialog)) {
            return null;
        }
        for (Input input : ((MultiActionDialog) dialog).getCommon().getInputs()) {
            if (!INPUT_KEY.matcher(input.getKey()).matches()) {
                return input.getKey();
            }
        }
        return null;
    }

    private static String localize(PlayerPreferences preferences, String message) {
        return PlayerPreferencesManager.localizeUiForPlayer(preferences, message);
    }

    private static boolean send(Player player, Dialog dialog) {
        if (!isSupported(player)) {
            return false;
        }
        // Cheap, and the alternative is a disconnect: a malformed key makes
        // the client fail to decode show_dialog entirely
        String badKey = firstInvalidKey(dialog);
        if (badKey != null) {
            RedCraftChat.getInstance().getLogger().error(
                    "Refusing to send a language dialog with the invalid input key '{}'; "
                            + "Minecraft only accepts [a-z0-9_.-] and would drop the connection", badKey);
            return false;
        }
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) {
            return false;
        }
        try {
            user.sendPacketSilently(new WrapperPlayServerShowDialog(dialog));
            return true;
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().warn("Could not show the language dialog to {}: {}",
                    player.getUsername(), e.getMessage());
            return false;
        }
    }
}
