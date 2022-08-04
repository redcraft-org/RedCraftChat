package org.redcraft.redcraftchat.commands.discord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.redcraft.redcraftchat.discord.DiscordClient;
import org.redcraft.redcraftchat.helpers.BasicMessageFormatter;
import org.redcraft.redcraftchat.locales.LocaleManager;
import org.redcraft.redcraftchat.models.locales.SupportedLocale;
import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.PlayerPreferencesManager;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu.Builder;

public class LangDiscordCommand extends ListenerAdapter {

    String buttonPrefixToggle = "toggle-lang-";
    String mainLanguageMenuId = "main-language";

    public LangDiscordCommand() {
        DiscordClient.getClient().upsertCommand(Commands.slash("lang", "Set your languages")
                .addOption(OptionType.STRING, "locale", "The locale to enable/disable")
                .addOption(OptionType.BOOLEAN, "is-main", "Wether you want this locale to be your main locale")
                ).queue();
    }

    // This is a fallback because Discord is sometimes dumb
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getMessage().getContentRaw().startsWith("/lang")) {
            return;
        }

        String[] elements = event.getMessage().getContentRaw().split(" ");

        try {
            event.getMessage().delete().queue();
        } catch (Exception e) {
            // Ignore
        }

        User user = event.getAuthor();

        String locale = elements.length > 1 ? elements[1] : null;

        boolean isMain = elements.length > 2 ? elements[2].equals("main") : false;

        user.openPrivateChannel().queue(channel -> {
            channel.sendMessageEmbeds(handleCommand(user, locale, isMain)).queue();
        });
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("lang")) {
            return;
        }

        event.deferReply().setEphemeral(true).queue();

        String locale = null;
        OptionMapping localeOption = event.getOption("locale");
        if (localeOption != null) {
            locale = localeOption.getAsString();
        }

        boolean isMain = false;
        OptionMapping isMainOption = event.getOption("is-main");
        if (isMainOption != null) {
            isMain = isMainOption.getAsBoolean();
        }

        MessageEmbed response = handleCommand(event, locale, isMain);

        if (response != null) {
            event.getHook().editOriginalEmbeds(response).queue();
        }
    }

    @Override
    public void onSelectMenuInteraction(SelectMenuInteractionEvent event) {
        if (!event.getComponentId().equals(mainLanguageMenuId)) {
            return;
        }

        event.deferEdit().queue();

        User user = event.getUser();

        try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(user);

            String locale = event.getSelectedOptions().get(0).getValue();

            PlayerPreferencesManager.setMainPlayerLocale(preferences, locale);

            this.sendLanguageSelector(event);
        } catch (IOException | InterruptedException e) {
            event.getHook().editOriginalEmbeds(BasicMessageFormatter.generateDiscordError(user, "An error occurred while trying to main language, please try again later")).queue();
            e.printStackTrace();
            return;
        }

    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().startsWith(buttonPrefixToggle)) {
            return;
        }

        event.deferEdit().queue();

        User user = event.getUser();

        try {
            PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(user);

            String locale = event.getComponentId().substring(buttonPrefixToggle.length());

            PlayerPreferencesManager.togglePlayerLocale(preferences, locale);
            this.sendLanguageSelector(event);
        } catch (Exception e) {
            event.getHook().editOriginalEmbeds(BasicMessageFormatter.generateDiscordError(user, "An error occurred while trying to change languages, please try again later")).queue();
            e.printStackTrace();
            return;
        }
    }

    public MessageEmbed handleCommand(SlashCommandInteractionEvent event, String locale, boolean isMain) {
        if (!event.isAcknowledged()) {
            event.deferReply().setEphemeral(true).queue();
        }

        return handleCommand(event, event.getUser(), locale, isMain);
    }

    public MessageEmbed handleCommand(User user, String locale, boolean isMain) {
        return handleCommand(null, user, locale, isMain);
    }

    public void sendLanguageSelector(IReplyCallback event) throws IOException, InterruptedException {
        User user = event.getUser();

        PlayerPreferences preferences = PlayerPreferencesManager.getPlayerPreferences(user);

        MessageEmbed header = BasicMessageFormatter.generateDiscordMessage(user, "Language selector", "Please select your languages:", 0xFFFF00);

        String chooseMainLanguage = PlayerPreferencesManager.localizeMessageForPlayer(preferences, "Change main language");

        Builder mainLocaleMenu = SelectMenu.create(mainLanguageMenuId).setPlaceholder(chooseMainLanguage);

        List<Button> buttons = new ArrayList<Button>();

        List <ActionRow> actionRows = new ArrayList<ActionRow>();

        for (SupportedLocale locale : LocaleManager.getSupportedLocales()) {
            String buttonId = buttonPrefixToggle + locale.code;
            Button button = null;
            if (preferences.languages.contains(locale.code)) {
                button = Button.danger(buttonId, "- " + locale.name);
            } else {
                button = Button.success(buttonId, "+ " + locale.name);
            }

            if (preferences.mainLanguage.equals(locale.code)) {
                button = button.asDisabled();
            } else {
                mainLocaleMenu.addOption(locale.name, locale.code);
            }

            if (buttons.size() >= 5) {
                actionRows.add(ActionRow.of(buttons));
                buttons.clear();
            }

            buttons.add(button);
        }

        if (buttons.size() > 0) {
            actionRows.add(ActionRow.of(buttons));
            buttons.clear();
        }

        actionRows.add(ActionRow.of(mainLocaleMenu.build()));

        event.getHook().editOriginalEmbeds(header).setActionRows(actionRows).queue();
    }

    public MessageEmbed handleCommand(SlashCommandInteractionEvent event, User user, String locale, boolean isMain) {
        PlayerPreferences preferences = null;

        try {
            preferences = PlayerPreferencesManager.getPlayerPreferences(user);

            if (locale != null) {
                if (isMain) {
                    PlayerPreferencesManager.setMainPlayerLocale(preferences, locale);
                } else {
                    MessageEmbed error = togglePlayerLocale(preferences, locale, user);
                    if (error != null) {
                        return error;
                    }
                }
                PlayerPreferencesManager.getPlayerPreferences(user);

                if (event == null) {
                    return BasicMessageFormatter.generateDiscordMessage(user, "Success", "Preferences updated!", 0x00FF00);
                }
            } else if (event == null) {
                return BasicMessageFormatter.generateDiscordError(user, "You used the command via raw text chat, to select your language, make sure to properly select the command when typing it in Discord.");
            }

            this.sendLanguageSelector(event);
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return BasicMessageFormatter.generateDiscordError(user, "An error occurred while trying to load or change languages, please try again later");
    }

    private MessageEmbed togglePlayerLocale(PlayerPreferences preferences, String locale, User user) {
        try {
            PlayerPreferencesManager.togglePlayerLocale(preferences, locale);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return BasicMessageFormatter.generateDiscordError(user, e.getMessage());
        }
        return null;
    }
}
