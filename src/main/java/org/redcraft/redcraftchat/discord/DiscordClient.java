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
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class DiscordClient {
    private static JDA jdaClient = null;
    private static boolean jdaCrashed = false;

    private DiscordClient() {
        throw new IllegalStateException("This class should not be instantiated");
    }

    public static JDA getClient() {
        if (jdaCrashed || jdaClient != null) {
            return jdaClient;
        }

        JDABuilder builder = JDABuilder.createDefault(Config.discordToken);

        if (Config.discordActivityEnabled) {
            ActivityType activityType = ActivityType.valueOf(Config.discordActivityType.toUpperCase());
            Activity activity = Activity.of(activityType, Config.discordActivityValue);
            builder.setActivity(activity);
        }

        builder.enableIntents(
                GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS));

        builder.setChunkingFilter(ChunkingFilter.ALL);

        builder.setMemberCachePolicy(MemberCachePolicy.ALL);

        try {
            jdaClient = builder.build();

            RedCraftChat.getInstance().getLogger().info("Connected to Discord!");
            for (Guild guild : jdaClient.getGuilds()) {
                RedCraftChat.getInstance().getLogger().info("Connected to guild: " + guild.getName() + " with " + guild.getMembers().size() + " members");
            }
        } catch (LoginException e) {
            jdaCrashed = true;
            RedCraftChat.getInstance().getLogger().warning("Could not connect to Discord, check console");
            e.printStackTrace();
        }
        return jdaClient;
    }

    public static User getUser(String userId) {
        JDA client = getClient();
        if (client == null) {
            return null;
        }

        for (Guild guild : client.getGuilds()) {
            for (Member member : guild.getMembers()) {
                if (member.getUser().getId().equals(userId)) {
                    return member.getUser();
                }
            }
        }

        return null;
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

    @SuppressWarnings("deprecation")
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
        suffix = ChatColor.stripColor(suffix);

        builder.setUsername(username + suffix);

        String avatarUrl = Config.playerAvatarApiEndpoint;
        switch (Config.playerAvatarFormat) {
            case "uuid":
                avatarUrl = avatarUrl.replace("%player%", player.getUniqueId().toString());
                break;

            case "name":
                avatarUrl = avatarUrl.replace("%player%", player.getName());
                break;

            default:
                throw new IllegalArgumentException("Unsupported player-avatar-format: " + Config.playerAvatarFormat);
        }

        builder.setAvatarUrl(avatarUrl);

        builder.setContent(ChatColor.stripColor(message));

        AllowedMentions mentions = new AllowedMentions();
        mentions.withParseEveryone(false);
        builder.setAllowedMentions(mentions);

        return webhookClient.send(builder.build()).join();
    }

    public static WebhookMessageMappingList getWebhookMessagesFromOriginalMessage(String messageId) {
        return (WebhookMessageMappingList) CacheManager.get(CacheCategory.WEBHOOK_MESSAGE_MAPPING, messageId, WebhookMessageMappingList.class);
    }
}
