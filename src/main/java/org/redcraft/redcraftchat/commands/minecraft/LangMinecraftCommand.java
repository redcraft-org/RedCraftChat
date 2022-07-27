package org.redcraft.redcraftchat.commands.minecraft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.locales.LocaleManager;
import org.redcraft.redcraftchat.models.locales.SupportedLocale;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class LangMinecraftCommand extends Command {

    public LangMinecraftCommand() {
        super("lang");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // If it's not a player we need an arg
        if (!(sender instanceof ProxiedPlayer)) {
            BasicMessageFormatter.sendInternalError(sender, "This command can only be used by players");
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (!player.hasPermission("redcraftchat.command.lang")) {
            BasicMessageFormatter.sendInternalError(player, "You do not have the permission to use this command");
            return;
        }

        try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(player);

            if (args.length > 0) {
                if (args.length > 1 && args[1].equals("main")) {
                    PlayerPreferencesManager.setMainPlayerLocale(preferences, args[0]);
                } else {
                    try {
                        PlayerPreferencesManager.togglePlayerLocale(preferences, args[0]);
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        BasicMessageFormatter.sendInternalError(player, e.getMessage());
                    }
                }
                preferences = PlayerPreferencesManager.getPlayerPreferences(player);
            }

            String originalHeaderText = "LANGUAGE SELECTOR";
            String originalHelpText = "Click on a language to enable or disable it, click on the checkbox to make it default.";
            String originalCaptionText = "Caption:";
            String originalDisabledText = "disabled";
            String originalEnabledText = "enabled";

            String headerText = PlayerPreferencesManager.localizeMessageForPlayer(player, originalHeaderText);
            String helpText = PlayerPreferencesManager.localizeMessageForPlayer(player, originalHelpText);
            String captionText = PlayerPreferencesManager.localizeMessageForPlayer(player, originalCaptionText);
            String disabledText = PlayerPreferencesManager.localizeMessageForPlayer(player, originalDisabledText);
            String enabledText = PlayerPreferencesManager.localizeMessageForPlayer(player, originalEnabledText);

            String alreadyMainLanguage = PlayerPreferencesManager.localizeMessageForPlayer(player, "This is already your main language");
            String setAsMainLanguage = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "Click to set as main language");
            String removeFromLanguages = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "Click to remove this languages");
            String addToLanguages = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "Click to add this language");

            List<BaseComponent[]> messages = new ArrayList<BaseComponent[]>();

            // Add 10 lines of empty lines to the message
            for (int i = 0; i < 10; i++) {
                messages.add(new ComponentBuilder().create());
            }

            messages.add(new ComponentBuilder()
                    .append("----- ").color(ChatColor.GREEN)
                    .append(headerText).color(ChatColor.GOLD).event(new HoverEvent(Action.SHOW_TEXT, new Text(originalHeaderText)))
                    .append(" -----").color(ChatColor.GREEN)
                    .create());

            messages.add(new ComponentBuilder()
                    .append(helpText).color(ChatColor.YELLOW).event(new HoverEvent(Action.SHOW_TEXT, new Text(originalHelpText)))
                    .create());

            messages.add(new ComponentBuilder()
                    .append(captionText).color(ChatColor.GOLD).event(new HoverEvent(Action.SHOW_TEXT, new Text(originalCaptionText)))
                    .append(" ")
                    .append(disabledText).color(ChatColor.GRAY).event(new HoverEvent(Action.SHOW_TEXT, new Text(originalDisabledText)))
                    .append(" ")
                    .append(enabledText).color(ChatColor.GREEN).event(new HoverEvent(Action.SHOW_TEXT, new Text(originalEnabledText)))
                    .create());

            for (SupportedLocale locale : LocaleManager.getSupportedLocales()) {
                ComponentBuilder formattedLocale = new ComponentBuilder();
                if (locale.code.equals(preferences.mainLanguage)) {
                    formattedLocale.append("[X] ").color(ChatColor.GREEN)
                        .event(new HoverEvent(Action.SHOW_TEXT, new Text(ChatColor.RED + alreadyMainLanguage)));
                } else {
                    formattedLocale.append("[ ] ").color(ChatColor.DARK_GRAY)
                        .event(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/lang " + locale.code + " main"))
                        .event(new HoverEvent(Action.SHOW_TEXT, new Text(ChatColor.GREEN + setAsMainLanguage)));
                }
                formattedLocale.append(locale.name);
                if (preferences.languages.contains(locale.code)) {
                    formattedLocale.color(ChatColor.GREEN);
                    if (!locale.code.equals(preferences.mainLanguage)) {
                        formattedLocale.event(new HoverEvent(Action.SHOW_TEXT, new Text(ChatColor.RED + removeFromLanguages)));
                    }

                } else {
                    formattedLocale.color(ChatColor.GRAY);
                    formattedLocale.event(new HoverEvent(Action.SHOW_TEXT, new Text(ChatColor.GREEN + addToLanguages)));
                }
                if (!locale.code.equals(preferences.mainLanguage)) {
                    formattedLocale.event(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/lang " + locale.code));
                }
                messages.add(formattedLocale.create());
            }

            for (BaseComponent[] message : messages) {
                player.sendMessage(message);
            }
        } catch (IOException | InterruptedException e) {
            BasicMessageFormatter.sendInternalError(player, "An error occured while trying to load or change languages, please try again later");
            e.printStackTrace();
        }
    }

}
