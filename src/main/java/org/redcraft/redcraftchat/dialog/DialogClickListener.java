package org.redcraft.redcraftchat.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTNumber;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCustomClickAction;
import com.velocitypowered.api.proxy.Player;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.displaykit.LanguageSelectorSession;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.locales.UiStrings;
import org.redcraft.redcraftchat.locales.LocaleManager;
import org.redcraft.redcraftchat.models.locales.SupportedLocale;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * The return channel for the native dialog.
 *
 * A dialog button carrying a dynamic custom action makes the client send
 * minecraft:custom_click_action, and because every packet crosses the proxy
 * this listener sees it without any backend plugin. The packet is cancelled
 * either way: the ids are ours, and a backend that does not know them would
 * only log a complaint.
 *
 * Runs on netty. Everything that touches preferences or translation blocks,
 * so it is bounced to a scheduler thread.
 */
public class DialogClickListener extends PacketListenerAbstract {

    public DialogClickListener() {
        super(PacketListenerPriority.NORMAL);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CUSTOM_CLICK_ACTION) {
            return;
        }

        WrapperPlayClientCustomClickAction wrapper = new WrapperPlayClientCustomClickAction(event);
        ResourceLocation id = wrapper.getId();
        if (id == null || !"redcraftchat".equals(id.getNamespace())) {
            return;
        }

        event.setCancelled(true);

        UUID playerId = event.getUser().getUUID();
        if (playerId == null) {
            return;
        }
        // The payload is nullable, and is only present when the dialog had
        // inputs or the button carried additions
        NBT payload = wrapper.getPayload();
        NBTCompound values = payload instanceof NBTCompound ? (NBTCompound) payload : null;

        RedCraftChat plugin = RedCraftChat.getInstance();
        plugin.getProxy().getScheduler().buildTask(plugin, () -> handle(playerId, id, values)).schedule();
    }

    private void handle(UUID playerId, ResourceLocation id, NBTCompound values) {
        Player player = RedCraftChat.getInstance().getProxy().getPlayer(playerId).orElse(null);
        if (player == null) {
            return;
        }

        try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);

            switch (id.getKey()) {
                case "pick_primary":
                    String code = readString(values, NativeDialogSelector.LANGUAGE_KEY);
                    if (code != null) {
                        preferences = setPrimary(player, preferences, code);
                    }
                    // Back to the same question, now written in the language
                    // just chosen. Continue is what leaves this step.
                    NativeDialogSelector.showPrimary(player, preferences);
                    break;

                case "go_to_others":
                    NativeDialogSelector.showOthers(player, preferences);
                    break;

                case "back_to_primary":
                    NativeDialogSelector.showPrimary(player, preferences);
                    break;

                case "confirm_others":
                    applyUnderstood(player, preferences, values);
                    PlayerPreferences confirmed = PlayerPreferencesManager.getPlayerPreferences(player);
                    PlayerPreferencesManager.confirmLanguageSelection(confirmed);
                    NativeDialogSelector.clear(player);
                    NativeDialogSelector.forget(playerId);
                    // The dialog closing is not an answer: without this the
                    // player is left guessing whether anything was saved
                    BasicMessageFormatter.sendInternalMessage(player,
                            PlayerPreferencesManager.localizeUiForPlayer(confirmed,
                                    UiStrings.SELECTOR_CONFIRMED),
                            NamedTextColor.GREEN);
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().warn("Language dialog action {} failed for {}: {}",
                    id.getKey(), player.getUsername(), e.getMessage());
        }
    }

    /**
     * Sets the primary language, taking back the previous trial if this flow
     * was the only reason it counted as understood.
     *
     * Without this, trying French and then English leaves French ticked on
     * the next screen as a language the player never claimed to read.
     */
    private PlayerPreferences setPrimary(Player player, PlayerPreferences preferences, String code)
            throws Exception {
        java.util.UUID playerId = player.getUniqueId();
        boolean alreadyUnderstood = preferences.languages != null
                && preferences.languages.contains(code);
        String autoAdded = NativeDialogSelector.autoAdded(playerId);

        String drop = LanguageSelectorSession.languageToDropOnPrimaryChange(autoAdded, code);
        if (drop != null && preferences.languages != null) {
            // In memory only: setMainPlayerLocale persists straight after, so
            // the swap costs one write rather than two
            preferences.languages.remove(drop);
        }

        PlayerPreferencesManager.setMainPlayerLocale(preferences, code);
        NativeDialogSelector.rememberAutoAdded(playerId,
                LanguageSelectorSession.rememberAutoAdded(autoAdded, code, alreadyUnderstood));

        return PlayerPreferencesManager.getPlayerPreferences(player);
    }

    /**
     * Applies the toggles the player left ticked. Boolean inputs arrive as
     * the onTrue/onFalse strings the dialog declared, not as NBT bytes.
     */
    private void applyUnderstood(Player player, PlayerPreferences preferences, NBTCompound values) {
        if (values == null) {
            return;
        }

        // Collected first: togglePlayerLocale writes and re-reads preferences
        // each time, so deciding from a snapshot keeps the intent stable
        List<String> toToggle = new ArrayList<>();
        for (SupportedLocale locale : LocaleManager.getSupportedLocales()) {
            if (locale.code.equalsIgnoreCase(preferences.mainLanguage)) {
                continue;
            }
            String raw = readString(values, NativeDialogSelector.understoodKey(locale.code));
            if (raw == null) {
                continue;
            }
            boolean wanted = "true".equalsIgnoreCase(raw);
            boolean current = preferences.languages != null
                    && preferences.languages.contains(locale.code);
            if (wanted != current) {
                toToggle.add(locale.code);
            }
        }

        for (String code : toToggle) {
            try {
                PlayerPreferences current = PlayerPreferencesManager.getPlayerPreferences(player);
                PlayerPreferencesManager.togglePlayerLocale(current, code);
            } catch (Exception e) {
                RedCraftChat.getInstance().getLogger().warn("Could not toggle {} for {}: {}",
                        code, player.getUsername(), e.getMessage());
            }
        }
    }

    /**
     * Reads one string out of the payload.
     *
     * The declared on_true and on_false strings are what template
     * substitution uses; a custom payload is free to carry the value in its
     * own type instead, so a boolean can arrive as a byte rather than as the
     * word. Reading only NBTString silently drops those, which reads to the
     * player as a submit button that does nothing.
     */
    private String readString(NBTCompound values, String key) {
        if (values == null) {
            return null;
        }
        NBT tag = values.getTagOrNull(key);
        if (tag == null) {
            return null;
        }
        if (tag instanceof NBTString) {
            return ((NBTString) tag).getValue();
        }
        if (tag instanceof NBTNumber) {
            // Any non-zero number is the ticked state
            return ((NBTNumber) tag).getAsInt() != 0 ? "true" : "false";
        }
        return null;
    }
}
