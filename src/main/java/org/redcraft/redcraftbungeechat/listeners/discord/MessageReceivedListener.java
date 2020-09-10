package org.redcraft.redcraftbungeechat.listeners.discord;

import org.redcraft.redcraftbungeechat.RedCraftBungeeChat;
import org.redcraft.redcraftbungeechat.discord.DiscordClient;
import org.redcraft.redcraftbungeechat.models.DeeplResponse;
import org.redcraft.redcraftbungeechat.translate.DeeplClient;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.ProxyServer;

public class MessageReceivedListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE)) {
            this.handlePrivateMessage(event);
        } else if (event.isFromType(ChannelType.TEXT)) {
            this.handlePublicMessage(event);
        }
    }

    public void handlePrivateMessage(MessageReceivedEvent event) {
        String debugMessage = "[PM] " + event.getAuthor().getName() + " " + event.getMessage().getContentDisplay();
        ProxyServer.getInstance().getLogger().info(debugMessage);
    }

    public void handlePublicMessage(MessageReceivedEvent event) {
        if (event.getMember() == null) {
            return;
        }

        // UGLY this is very much hardcoded values for the early alpha

        if (event.getTextChannel().getName().equals("general-en")
                || event.getTextChannel().getName().equals("general-fr")) {
            try {
                String originalMessage = event.getMessage().getContentRaw();

                // TODO properly handle this
                originalMessage = originalMessage.replace("@everyone", "<at>everyone");

                DeeplResponse deeplResponse;
                TextChannel responseChannel;
                if (event.getTextChannel().getName().equals("general-en")) {
                    deeplResponse = DeeplClient.translate(originalMessage, "EN", "FR");
                    responseChannel = DiscordClient.getClient().getTextChannelsByName("general-fr", false).get(0);
                } else {
                    deeplResponse = DeeplClient.translate(originalMessage, "FR", "EN");
                    responseChannel = DiscordClient.getClient().getTextChannelsByName("general-en", false).get(0);
                }
                String translatedMessage = DeeplClient.parseDeeplResponse(deeplResponse);

                String webhookName = RedCraftBungeeChat.getInstance().getDescription().getName();

                Webhook webhookDestination = DiscordClient.getOrCreateWebhook(responseChannel, webhookName);

                WebhookClient webhookClient = DiscordClient.getWebhookClient(webhookDestination.getUrl());

                // Change appearance of webhook message
                WebhookMessageBuilder builder = new WebhookMessageBuilder();
                builder.setUsername(event.getMember().getEffectiveName());
                builder.setAvatarUrl(event.getMember().getUser().getAvatarUrl());
                builder.setContent(translatedMessage);
                webhookClient.send(builder.build());

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }
}
