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
import org.redcraft.redcraftchat.models.players.PlayerMail;
import org.redcraft.redcraftchat.messaging.MailMessagesManager;
import org.redcraft.redcraftchat.minecraft.ServerDisplayNameManager;
import org.redcraft.redcraftchat.servers.LoginServer;
import org.redcraft.redcraftchat.servers.ServerTransfer;
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

                case "server_pick": {
                    String target = readString(values, ServerSelectorDialog.SERVER_KEY);
                    if (target != null) {
                        ServerTransfer.send(player, target);
                    }
                    break;
                }

                case "server_list":
                    ServerSelectorDialog.show(player);
                    break;

                case "server_defaults":
                    ServerSelectorDialog.showDefaults(player);
                    break;

                case "server_set_default": {
                    String choice = readString(values, ServerSelectorDialog.SERVER_KEY);
                    // Empty is a real answer here, meaning "leave it to the
                    // proxy", so it is stored rather than treated as missing
                    preferences.loginServer = choice == null || choice.isEmpty() ? null : choice;
                    PlayerPreferencesManager.updatePlayerPreferences(preferences);

                    PlayerPreferences saved = PlayerPreferencesManager.getPlayerPreferences(player);
                    BasicMessageFormatter.sendInternalMessage(player,
                            PlayerPreferencesManager.localizeUiForPlayer(saved, UiStrings.SERVERS_DEFAULT_SAVED)
                                    .replace("%server%", describeDefault(saved)),
                            NamedTextColor.GREEN);
                    ServerSelectorDialog.showDefaults(player);
                    break;
                }

                case "mail_menu":
                    MailDialog.showMenu(player);
                    break;

                case "mail_inbox":
                    MailDialog.showInbox(player);
                    break;

                case "mail_compose":
                    MailDialog.showCompose(player, null);
                    break;

                case "mail_reply": {
                    PlayerMail replying = findMail(player, readString(values, MailDialog.MAIL_ID_KEY));
                    if (replying != null) {
                        MailDialog.showCompose(player,
                                MailMessagesManager.getMailSenderDisplayName(replying));
                    }
                    break;
                }

                case "mail_mark_read": {
                    for (PlayerMail mail : MailMessagesManager.getPlayerMail(player, false)) {
                        if (mail.readAt == null) {
                            MailMessagesManager.markMailAsRead(mail);
                        }
                    }
                    MailDialog.showInbox(player);
                    break;
                }

                case "mail_send":
                    sendMail(player, values);
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            RedCraftChat.getInstance().getLogger().warn("Language dialog action {} failed for {}: {}",
                    id.getKey(), player.getUsername(), e.getMessage());
        }
    }

    /** The setting in words, for the line confirming it was saved. */
    private String describeDefault(PlayerPreferences preferences) {
        String setting = preferences.loginServer;
        if (LoginServer.LAST.equals(setting)) {
            return PlayerPreferencesManager.localizeUiForPlayer(preferences, UiStrings.SERVERS_DEFAULT_LAST);
        }
        if (setting == null || setting.isEmpty()) {
            return ServerDisplayNameManager.getDisplayName(
                    RedCraftChat.getInstance().getProxy().getConfiguration()
                            .getAttemptConnectionOrder().stream().findFirst().orElse(""));
        }
        return ServerDisplayNameManager.getDisplayName(setting);
    }

    private PlayerMail findMail(Player player, String internalId) throws Exception {
        if (internalId == null) {
            return null;
        }
        for (PlayerMail mail : MailMessagesManager.getPlayerMail(player, false)) {
            if (internalId.equals(mail.internalId)) {
                return mail;
            }
        }
        return null;
    }

    /**
     * Sends what the compose form was filled with.
     *
     * The recipient is either baked into the button, when replying, or typed.
     * There is no picker of online players: mail is for reaching someone who
     * is not connected, and the ones the proxy can list are exactly the ones
     * you would /msg instead.
     */
    private void sendMail(Player player, NBTCompound values) throws Exception {
        // Read up front: every path out of here says something to the
        // player, and all of it is said in their own language
        PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);

        String message = readString(values, MailDialog.MESSAGE_INPUT);
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        String recipient = readString(values, MailDialog.RECIPIENT_INPUT);
        if (recipient == null || recipient.trim().isEmpty()) {
            BasicMessageFormatter.sendInternalError(player,
                    PlayerPreferencesManager.localizeUiForPlayer(preferences, UiStrings.MAIL_NO_RECIPIENT));
            return;
        }

        PlayerPreferences target = PlayerPreferencesManager.getPlayerPreferences(recipient.trim(), true, false);
        if (target == null) {
            BasicMessageFormatter.sendInternalError(player,
                    PlayerPreferencesManager.localizeUiForPlayer(preferences, UiStrings.MAIL_PLAYER_NOT_FOUND));
            return;
        }

        MailMessagesManager.sendMail(player, target.minecraftUuid, message.trim());
        BasicMessageFormatter.sendInternalMessage(player,
                PlayerPreferencesManager.localizeUiForPlayer(preferences, UiStrings.MAIL_SENT_TO)
                        .replace("%player%", recipient.trim()),
                NamedTextColor.GREEN);
        MailDialog.showInbox(player);
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
