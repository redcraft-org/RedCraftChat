package org.redcraft.redcraftchat.discord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;

public class ChannelManager {

    public void syncChannelCategories() {
        JDA discordClient = DiscordClient.getClient();

        for (Guild guild : discordClient.getGuilds()) {
            List<String> detectedTopics = new ArrayList<String>();

            for (Category category : guild.getCategories()) {
                for (String supportedLanguage : Config.translationSupportedLanguages) {
                    Matcher channelMatcher = getCategoryRegexMatcher(supportedLanguage, category.getName());

                    if (channelMatcher.matches()) {
                        String topic = channelMatcher.group(1);
                        if (!detectedTopics.contains(topic)) {
                            detectedTopics.add(topic);
                        }
                    }
                }
            }

            HashMap<String, List<String>> textChannels = new HashMap<String, List<String>>();
            HashMap<String, List<String>> voiceChannels = new HashMap<String, List<String>>();

            // Loop a first time to Creating missing categories and gather channel names
            for (String topic : detectedTopics) {
                textChannels.put(topic, new ArrayList<String>());
                voiceChannels.put(topic, new ArrayList<String>());

                List<String> topicTextChannel = textChannels.get(topic);
                List<String> topicVoiceChannel = voiceChannels.get(topic);

                for (String language : Config.translationSupportedLanguages) {
                    String categoryName = getCategoryName(language, topic);
                    List<Category> matchingCategories = guild.getCategoriesByName(categoryName, false);

                    if (matchingCategories.size() == 0) {
                        String logMessage = String.format("Creating missing Discord category %s", categoryName);
                        RedCraftChat.getInstance().getLogger().info(logMessage);
                        guild.createCategory(categoryName).complete();
                    }

                    for (Category category : matchingCategories) {
                        for (GuildChannel channel : category.getChannels()) {
                            String channelName = channel.getName();
                            if (channel.getType().equals(ChannelType.TEXT) && !topicTextChannel.contains(channelName)) {
                                topicTextChannel.add(channelName);
                            }
                            if (channel.getType().equals(ChannelType.VOICE)
                                    && !topicVoiceChannel.contains(channelName)) {
                                topicVoiceChannel.add(channelName);
                            }
                        }
                    }
                }
            }

            for (String topic : detectedTopics) {
                List<String> topicTextChannels = textChannels.get(topic);
                List<String> topicVoiceChannels = voiceChannels.get(topic);

                createCategoryChannels(guild, topic, topicTextChannels, ChannelType.TEXT);
                createCategoryChannels(guild, topic, topicVoiceChannels, ChannelType.VOICE);
            }
        }
    }

    public void createCategoryChannels(Guild guild, String topic, List<String> channelNames, ChannelType channelType) {
        for (String language : Config.translationSupportedLanguages) {
            String categoryName = getCategoryName(language, topic);
            List<Category> matchingCategories = guild.getCategoriesByName(categoryName, false);

            for (Category category : matchingCategories) {
                List<GuildChannel> categoryChannels = category.getChannels();
                for (String channelName : channelNames) {
                    if (!channelExistsInCategory(categoryChannels, channelName, channelType)) {
                        String logMessage = String.format("Creating missing Discord %s channel %s in category %s",
                                channelType.toString().toLowerCase(), channelName, categoryName);
                        RedCraftChat.getInstance().getLogger().info(logMessage);
                        if (channelType.equals(ChannelType.TEXT)) {
                            category.createTextChannel(channelName).complete();
                        } else if (channelType.equals(ChannelType.VOICE)) {
                            category.createVoiceChannel(channelName).complete();
                        }
                    }
                }
            }
        }
    }

    public String extractCategoryLanguage(String categoryName) {
        return extractCategoryMatch(categoryName, "%lang%", "([A-Z]{1,3})");
    }

    public String extractCategoryTopic(String categoryName) {
        return extractCategoryMatch(categoryName, "%topic%", "(.*)");
    }

    public boolean matchesTranslatedCategory(String categoryName) {
        String language = extractCategoryLanguage(categoryName);
        String topic = extractCategoryTopic(categoryName);

        return language != null && topic != null;
    }

    public boolean channelExistsInCategory(List<GuildChannel> channels, String channelName, ChannelType channelType) {
        for (GuildChannel channel : channels) {
            if (channel.getType().equals(channelType) && channel.getName().equals(channelName)) {
                return true;
            }
        }
        return false;
    }

    public String extractCategoryMatch(String categoryName, String target, String replacement) {
        String channelRegex = getCategoryRegex();
        channelRegex = channelRegex.replace(target, replacement);

        Pattern channelPattern = Pattern.compile(channelRegex);
        String match = channelPattern.matcher(categoryName).group();
        if (match.isEmpty()) {
            return null;
        }
        return match;
    }

    private Matcher getCategoryRegexMatcher(String language, String categoryName) {
        String channelRegex = getCategoryRegex();
        channelRegex = channelRegex.replace("%lang%", language.toUpperCase());
        channelRegex = channelRegex.replace("%topic%", "(.*)");

        Pattern channelPattern = Pattern.compile(channelRegex);
        return channelPattern.matcher(categoryName);
    }

    public String getCategoryRegex() {
        String[] charsToEscape = new String[] { "\\", "[", "]", "(", ")", "{", "}", ",", "-", "&", "?", "^", "!" };
        String channelRegex = Config.translationDiscordCategoryFormat;
        for (String charToEscape : charsToEscape) {
            channelRegex = channelRegex.replace(charToEscape, String.format("\\%s", charToEscape));
        }

        return channelRegex;
    }

    public String getCategoryName(String language, String topic) {
        String name = Config.translationDiscordCategoryFormat;

        name = name.replace("%lang%", language.toUpperCase());
        name = name.replace("%topic%", topic);

        return name;
    }
}
