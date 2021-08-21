package org.redcraft.redcraftchat.discord;

import java.util.List;

import javax.security.auth.login.LoginException;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;
import org.redcraft.redcraftchat.caching.CacheManager;
import org.redcraft.redcraftchat.models.caching.CacheCategory;
import org.redcraft.redcraftchat.models.discord.WebhookAsUser;
import org.redcraft.redcraftchat.models.discord.WebhookMessageMappingList;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.requests.RestAction;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class DiscordClient {
    private static JDA jdaClient = null;
    private static boolean jdaCrashed = false;

    public static JDA getClient() {
        if (jdaCrashed || jdaClient != null) {
            return jdaClient;
        }

        JDABuilder builder = JDABuilder.createDefault(Config.discordToken);

        // builder.disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE);

        if (Config.discordActivityEnabled) {
            ActivityType activityType = ActivityType.valueOf(Config.discordActivityType.toUpperCase());
            Activity activity = Activity.of(activityType, Config.discordActivityValue);
            builder.setActivity(activity);
        }

        try {
            jdaClient = builder.build();
            RedCraftChat.getInstance().getLogger().info("Connected to Discord!");
        } catch (LoginException e) {
            jdaCrashed = true;
            RedCraftChat.getInstance().getLogger().warning("Could not connect to Discord, check console");
            e.printStackTrace();
        }
        return jdaClient;
    }

    public static Webhook getOrCreateWebhook(TextChannel channel, String webhookName) {
        RestAction<List<Webhook>> webhooks = channel.retrieveWebhooks();
        for (Webhook webhook : webhooks.complete()) {
            if (webhook.getName().equals(webhookName)) {
                return webhook;
            }
        }

        return channel.createWebhook(webhookName).complete();
    }

    public static WebhookClient getWebhookClient(String token) {
        WebhookClientBuilder builder = new WebhookClientBuilder(token);
        builder.setThreadFactory((job) -> {
            Thread thread = new Thread(job);
            thread.setDaemon(true);
            return thread;
        });
        builder.setWait(true);
        return builder.build();
    }

    public static ReadonlyMessage postAsUser(WebhookAsUser webhookAsUser, String suffix, String previousMessageId) {
        return postAsUser(webhookAsUser.responseChannel, webhookAsUser.member, webhookAsUser.content, webhookAsUser.attachments, suffix, previousMessageId);
    }

    public static ReadonlyMessage postAsUser(TextChannel responseChannel, Member member, String content, List<Attachment> attachments, String suffix, String previousMessageId) {
        String webhookName = RedCraftChat.getInstance().getDescription().getName();

        Webhook webhookDestination = DiscordClient.getOrCreateWebhook(responseChannel, webhookName);

        WebhookClient webhookClient = DiscordClient.getWebhookClient(webhookDestination.getUrl());

        // Change appearance of webhook message
        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        String username = member.getEffectiveName();

        if (suffix != null) {
            username = String.format("%s %s", username, suffix);
        }

        builder.setUsername(username);
        builder.setAvatarUrl(member.getUser().getAvatarUrl());

        if (attachments != null) {
            for (Attachment attachment : attachments) {
                builder.addFile(attachment.getFileName(), attachment.retrieveInputStream().join());
            }
        }

        builder.setContent(content);

        AllowedMentions mentions = new AllowedMentions();
        mentions.withParseEveryone(false);
        builder.setAllowedMentions(mentions);

        if (previousMessageId != null) {
            long messageId = Long.parseLong(previousMessageId);
            return webhookClient.edit(messageId, builder.build()).join();
        }
        return webhookClient.send(builder.build()).join();
    }

    public static ReadonlyMessage postAsPlayer(String responseChannelId, ProxiedPlayer player, String message, String suffix) {
        TextChannel responseChannel = jdaClient.getTextChannelById(responseChannelId);
        String webhookName = RedCraftChat.getInstance().getDescription().getName();

        Webhook webhookDestination = DiscordClient.getOrCreateWebhook(responseChannel, webhookName);

        WebhookClient webhookClient = DiscordClient.getWebhookClient(webhookDestination.getUrl());

        // Change appearance of webhook message
        WebhookMessageBuilder builder = new WebhookMessageBuilder();
        String username = ChatColor.stripColor(player.getDisplayName());

        builder.setUsername("[" + player.getServer().getInfo().getName() + "] " + username + suffix);
        builder.setAvatarUrl("https://testing.redcraft.org/api/v1/skin/head/" + player.getUniqueId().toString() + "?size=128");

        builder.setContent(ChatColor.stripColor(message));

        AllowedMentions mentions = new AllowedMentions();
        mentions.withParseEveryone(false);
        builder.setAllowedMentions(mentions);

        return webhookClient.send(builder.build()).join();
    }

    public static WebhookMessageMappingList getWebhookMessagesFromOriginalMessage(String messageId) {
        Object cachedObject = CacheManager.get(CacheCategory.WEBHOOK_MESSAGE_MAPPING, messageId, WebhookMessageMappingList.class);
        return (WebhookMessageMappingList) cachedObject;
    }
}
