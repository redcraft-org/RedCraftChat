package org.redcraft.redcraftchat.discord;

import java.util.List;

import javax.security.auth.login.LoginException;

import org.redcraft.redcraftchat.Config;
import org.redcraft.redcraftchat.RedCraftChat;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.Activity.ActivityType;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class DiscordClient {
    private static JDA jdaClient = null;
    private static boolean jdaCrashed = false;

    public static JDA getClient() {
        if (jdaCrashed || jdaClient != null) {
            return jdaClient;
        }

        JDABuilder builder = JDABuilder.createDefault(Config.discordToken);

        builder.disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE);

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
}
