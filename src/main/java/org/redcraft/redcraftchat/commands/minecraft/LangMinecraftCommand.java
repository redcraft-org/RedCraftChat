package org.redcraft.redcraftchat.commands.minecraft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.helpers.LegacyText;
import org.redcraft.redcraftchat.locales.LocaleManager;
import org.redcraft.redcraftchat.models.locales.SupportedLocale;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

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
        String originalHeaderText = "LANGUAGE SELECTOR";
        String originalHelpText = "Click on a language to enable or disable it, click on the checkbox to make it default.";
        String originalCaptionText = "Caption:";
        String originalDisabledText = "disabled";
        String originalEnabledText = "enabled";

        String headerText = PlayerPreferencesManager.localizeMessageForPlayer(preferences, originalHeaderText);
        String helpText = PlayerPreferencesManager.localizeMessageForPlayer(preferences, originalHelpText);
        String captionText = PlayerPreferencesManager.localizeMessageForPlayer(preferences, originalCaptionText);
        String disabledText = PlayerPreferencesManager.localizeMessageForPlayer(preferences, originalDisabledText);
        String enabledText = PlayerPreferencesManager.localizeMessageForPlayer(preferences, originalEnabledText);

        String alreadyMainLanguage = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "This is already your main language");
        String setAsMainLanguage = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "Click to set as main language");
        String removeFromLanguages = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "Click to remove this languages");
        String addToLanguages = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "Click to add this language");

        List<Component> messages = new ArrayList<Component>();

        // Add 5 empty lines to make the menu look better
        for (int i = 0; i < 5; i++) {
            messages.add(Component.empty());
        }

        // BungeeCord copied the hover of the previous part into every part that did
        // not set one of its own, Adventure has no such cascade between siblings so
        // the inherited events are repeated explicitly below
        HoverEvent<Component> headerHover = showText(originalHeaderText);
        messages.add(Component.text()
                .append(Component.text("---------- ", NamedTextColor.GREEN))
                .append(Component.text(headerText, NamedTextColor.GOLD).hoverEvent(headerHover))
                .append(Component.text(" ----------", NamedTextColor.GREEN).hoverEvent(headerHover))
                .build());

        messages.add(Component.text(helpText, NamedTextColor.YELLOW).hoverEvent(showText(originalHelpText)));

        HoverEvent<Component> captionHover = showText(originalCaptionText);
        HoverEvent<Component> disabledHover = showText(originalDisabledText);
        messages.add(Component.text()
                .append(Component.text(captionText, NamedTextColor.GOLD).hoverEvent(captionHover))
                .append(Component.text(" ", NamedTextColor.GOLD).hoverEvent(captionHover))
                .append(Component.text(disabledText, NamedTextColor.GRAY).hoverEvent(disabledHover))
                .append(Component.text(" ", NamedTextColor.GRAY).hoverEvent(disabledHover))
                .append(Component.text(enabledText, NamedTextColor.GREEN).hoverEvent(showText(originalEnabledText)))
                .build());

        for (SupportedLocale locale : LocaleManager.getSupportedLocales()) {
            boolean isMainLanguage = locale.code.equals(preferences.mainLanguage);

            Component checkbox;
            if (isMainLanguage) {
                checkbox = Component.text("[X] ", NamedTextColor.GREEN)
                        .hoverEvent(showText(LegacyText.RED + alreadyMainLanguage));
            } else {
                checkbox = Component.text("[ ] ", NamedTextColor.DARK_GRAY)
                        .clickEvent(ClickEvent.runCommand("/lang " + locale.code + " main"))
                        .hoverEvent(showText(LegacyText.GREEN + setAsMainLanguage));
            }

            Component localeName = Component.text(locale.name);
            if (preferences.languages.contains(locale.code)) {
                localeName = localeName.color(NamedTextColor.GREEN);
                if (isMainLanguage) {
                    // Inherited from the checkbox, the main language row explains
                    // on hover why nothing on it can be clicked
                    localeName = localeName.hoverEvent(showText(LegacyText.RED + alreadyMainLanguage));
                } else {
                    localeName = localeName.hoverEvent(showText(LegacyText.RED + removeFromLanguages));
                }
            } else {
                localeName = localeName.color(NamedTextColor.GRAY)
                        .hoverEvent(showText(LegacyText.GREEN + addToLanguages));
            }
            if (!isMainLanguage) {
                localeName = localeName.clickEvent(ClickEvent.runCommand("/lang " + locale.code));
            }

            messages.add(Component.text().append(checkbox).append(localeName).build());
        }

        return messages;
    }

    private HoverEvent<Component> showText(String legacyText) {
        return HoverEvent.showText(BasicMessageFormatter.deserialize(legacyText));
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
