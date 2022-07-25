package org.redcraft.redcraftchat.discord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.discord.TranslatedChannel;
import org.redcraft.redcraftchat.models.discord.UserMessageMapping;
import org.redcraft.redcraftchat.models.discord.WebhookAsUser;
import org.redcraft.redcraftchat.models.discord.WebhookMessageMapping;
import org.redcraft.redcraftchat.translate.TranslationManager;

import club.minnced.discord.webhook.receive.ReadonlyMessage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class ChannelManager {

    static TranslationManager translationManager = new TranslationManager(Config.chatTranslationProvider);

    private static HashMap<TranslatedChannel, List<TranslatedChannel>> translatedChannelsMapping = new HashMap<>();
    private static ReadWriteLock translatedChannelsMappingLock = new ReentrantReadWriteLock();

    private static List<TranslatedChannel> minecraftBridgeChannels = new ArrayList<>();
    private static ReadWriteLock minecraftBridgeChannelsLock = new ReentrantReadWriteLock();

    public void syncChannelCategories() {
        JDA discordClient = DiscordClient.getClient();

        HashMap<TranslatedChannel, List<TranslatedChannel>> newTranslatedChannelsMapping = new HashMap<>();

        for (Guild guild : discordClient.getGuilds()) {
            List<String> detectedTopics = getTopics(guild);

            HashMap<String, List<String>> textChannels = new HashMap<>();
            HashMap<String, List<String>> voiceChannels = new HashMap<>();

            // Loop a first time to Creating missing categories and gather channel names
            for (String topic : detectedTopics) {
                textChannels.put(topic, new ArrayList<>());
                voiceChannels.put(topic, new ArrayList<>());

                List<String> topicTextChannel = textChannels.get(topic);
                List<String> topicVoiceChannel = voiceChannels.get(topic);

                for (String language : Config.translationDiscordSupportedLanguages) {
                    String categoryName = getCategoryName(language, topic);
                    List<Category> matchingCategories = guild.getCategoriesByName(categoryName, false);

                    if (matchingCategories.isEmpty()) {
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

            // Loop a first time to Creating missing channels
            for (String topic : detectedTopics) {
                List<String> topicTextChannels = textChannels.get(topic);
                List<String> topicVoiceChannels = voiceChannels.get(topic);

                createCategoryChannels(guild, topic, topicTextChannels, ChannelType.TEXT);
                createCategoryChannels(guild, topic, topicVoiceChannels, ChannelType.VOICE);
            }

            // Loop a third time to update the channel mapping
            for (String topic : detectedTopics) {
                HashMap<String, List<TranslatedChannel>> channelsList = new HashMap<>();

                for (String language : Config.translationDiscordSupportedLanguages) {
                    String categoryName = getCategoryName(language, topic);
                    List<Category> matchingCategories = guild.getCategoriesByName(categoryName, false);

                    for (Category category: matchingCategories) {
                        for (GuildChannel channel: category.getChannels()) {
                            if (channel.getType().equals(ChannelType.TEXT)) {
                                String channelName = channel.getName();
                                if (!channelsList.containsKey(channelName)) {
                                    channelsList.put(channelName, new ArrayList<>());
                                }

                                TranslatedChannel translatedChannel = new TranslatedChannel(
                                    guild.getId(),
                                    channel.getId(),
                                    language.toLowerCase()
                                );

                                channelsList.get(channelName).add(translatedChannel);
                            }
                        }
                    }
                }

                // Update Minecraft channels list
                String bridgeChannelName = Config.discordChannelMinecraft;
                if (channelsList.containsKey(bridgeChannelName)) {
                    saveMinecraftBridgeChannels(channelsList.get(bridgeChannelName));
                }

                // Update translated channel mapping
                Iterator<Map.Entry<String, List<TranslatedChannel>>> channelsListIterator = channelsList.entrySet().iterator();
                while (channelsListIterator.hasNext()) {
                    Map.Entry<String, List<TranslatedChannel>> channelsListEntry = channelsListIterator.next();
                    for (TranslatedChannel translatedSourceChannel: channelsListEntry.getValue()) {
                        String translatedSourceChannelId = translatedSourceChannel.channelId;
                        newTranslatedChannelsMapping.put(translatedSourceChannel, new ArrayList<>());
                        List<TranslatedChannel> channelMapping = newTranslatedChannelsMapping.get(translatedSourceChannel);

                        for (TranslatedChannel translatedTargetChannel: channelsListEntry.getValue()) {
                            if (!translatedSourceChannelId.equals(translatedTargetChannel.channelId)) {
                                channelMapping.add(translatedTargetChannel);
                            }
                        }
                    }
                    channelsListIterator.remove(); // avoids a ConcurrentModificationException
                }
            }
        }

        saveTranslatedChannelsMapping(newTranslatedChannelsMapping);
    }

    public static WebhookMessageMapping translateAndPublishMessage(TranslatedChannel sourceChannel, TranslatedChannel targetChannel, Member member, Message message, String previousMessageId) throws Exception {
        Guild guild = DiscordClient.getClient().getGuildById(sourceChannel.guildId);

        String translatedMessage = translationManager.translate(message.getContentRaw(), sourceChannel.languageId, targetChannel.languageId);
        TextChannel responseChannel = guild.getTextChannelById(targetChannel.channelId);

        WebhookAsUser webhookToPost = new WebhookAsUser(responseChannel, member, translatedMessage, message.getAttachments());

        String suffix = "[" + TranslationManager.getLanguagePrefix(sourceChannel.languageId, targetChannel.languageId) + "]";

        ReadonlyMessage webhookMessage = DiscordClient.postAsUser(webhookToPost, suffix, previousMessageId);
        String webhookMessageId = String.valueOf(webhookMessage.getId());

        WebhookMessageMapping webhookMessageMapping = new WebhookMessageMapping(sourceChannel.guildId, targetChannel.channelId, webhookMessageId, targetChannel.languageId, member.getId());
        UserMessageMapping userMessageMapping = new UserMessageMapping(sourceChannel.guildId, sourceChannel.channelId, message.getId());

        CacheManager.put(CacheCategory.USER_MESSAGE_MAPPING, webhookMessageId, userMessageMapping);

        return webhookMessageMapping;
    }

    public static Map<TranslatedChannel, List<TranslatedChannel>> getTranslatedChannelsMapping() {
        translatedChannelsMappingLock.readLock().lock();
        try {
            return translatedChannelsMapping;
        } finally {
            translatedChannelsMappingLock.readLock().unlock();
        }
    }

    public static List<TranslatedChannel> getMinecraftBridgeChannels() {
        minecraftBridgeChannelsLock.readLock().lock();
        try {
            return minecraftBridgeChannels;
        } finally {
            minecraftBridgeChannelsLock.readLock().unlock();
        }
    }

    public List<String> getTopics(Guild guild) {
        List<String> detectedTopics = new ArrayList<>();

        for (Category category : guild.getCategories()) {
            for (String supportedLanguage : Config.translationDiscordSupportedLanguages) {
                Matcher channelMatcher = getCategoryRegexMatcher(supportedLanguage, category.getName());

                if (channelMatcher.matches()) {
                    String topic = channelMatcher.group(1);
                    if (!detectedTopics.contains(topic)) {
                        detectedTopics.add(topic);
                    }
                }
            }
        }

        return detectedTopics;
    }

    public void createCategoryChannels(Guild guild, String topic, List<String> channelNames, ChannelType channelType) {
        for (String language : Config.translationDiscordSupportedLanguages) {
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

    private static void saveTranslatedChannelsMapping(HashMap<TranslatedChannel, List<TranslatedChannel>> updatedMapping) {
        translatedChannelsMappingLock.writeLock().lock();
        try {
            translatedChannelsMapping = updatedMapping;
        } finally {
            translatedChannelsMappingLock.writeLock().unlock();
        }
    }

    private static void saveMinecraftBridgeChannels(List<TranslatedChannel> updatedChannels) {
        minecraftBridgeChannelsLock.writeLock().lock();
        try {
            minecraftBridgeChannels = updatedChannels;
        } finally {
            minecraftBridgeChannelsLock.writeLock().unlock();
        }
    }
}
