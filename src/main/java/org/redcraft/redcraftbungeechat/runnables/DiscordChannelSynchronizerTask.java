package org.redcraft.redcraftbungeechat.runnables;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.redcraft.redcraftbungeechat.Config;
import org.redcraft.redcraftbungeechat.discord.DiscordClient;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;

public class DiscordChannelSynchronizerTask implements Runnable{

    public void run() {
        JDA discordClient = DiscordClient.getClient();

        for (Guild guild : discordClient.getGuilds()) {
            List<String> detectedTopics = new ArrayList<String>();
            List<String> detectedLanguages = new ArrayList<String>();
            List<String> missingLanguages = new ArrayList<String>();

            for (Category category : guild.getCategories()) {
                for (String supportedLanguage : Config.translationSupportedLanguages) {
                    String[] charsToEscape = new String[]{"\\", "[", "]", "(", ")", "{", "}", ",", "-", "&", "?", "^", "!"};
                    String channelRegex = Config.translationDiscordCategoryFormat;
                    for (String charToEscape : charsToEscape) {
                        channelRegex = channelRegex.replace(charToEscape, String.format("\\%s", charToEscape));
                    }
                    channelRegex = channelRegex.replace("%lang%", supportedLanguage.toUpperCase());
                    channelRegex = channelRegex.replace("%topic%", "(.*)");

                    Pattern channelPattern = Pattern.compile(channelRegex);
                    Matcher channelMatcher = channelPattern.matcher(category.getName());

                    if (channelMatcher.matches()) {
                        if (!detectedLanguages.contains(supportedLanguage)) {
                            detectedLanguages.add(supportedLanguage);
                        }
                        String topic = channelMatcher.group(1);
                        if (!detectedTopics.contains(topic)) {
                            detectedTopics.add(topic);
                        }
                    }
                }
            }

            for (String supportedLanguage : Config.translationSupportedLanguages) {
                if (!detectedLanguages.contains(supportedLanguage)) {
                    missingLanguages.add(supportedLanguage);
                }
            }

            System.out.println("Topics:");
            System.out.println(detectedTopics);
            System.out.println("Detected languages:");
            System.out.println(detectedLanguages);
            System.out.println("Missing languages:");
            System.out.println(missingLanguages);
        }
    }

}
