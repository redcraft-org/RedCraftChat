package org.redcraft.redcraftbungeechat.listeners.discord;

import org.redcraft.redcraftbungeechat.RedCraftBungeeChat;
import org.redcraft.redcraftbungeechat.caching.CacheManager;
import org.redcraft.redcraftbungeechat.discord.DiscordClient;
import org.redcraft.redcraftbungeechat.models.caching.CacheCategory;
import org.redcraft.redcraftbungeechat.translate.TranslationManager;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.receive.ReadonlyMessage;
import club.minnced.discord.webhook.send.AllowedMentions;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

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
        event.getChannel().sendMessage("Sorry, I don't handle private messages yet!");
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

                String translatedMessage;
                TextChannel responseChannel;
                if (event.getTextChannel().getName().equals("general-en")) {
                    translatedMessage = TranslationManager.translate(originalMessage, "EN", "FR");
                    responseChannel = event.getTextChannel().getGuild().getTextChannelsByName("general-fr", false).get(0);
                } else {
                    translatedMessage = TranslationManager.translate(originalMessage, "FR", "EN");
                    responseChannel = event.getTextChannel().getGuild().getTextChannelsByName("general-en", false).get(0);
                }

                String webhookName = RedCraftBungeeChat.getInstance().getDescription().getName();

                Webhook webhookDestination = DiscordClient.getOrCreateWebhook(responseChannel, webhookName);

                WebhookClient webhookClient = DiscordClient.getWebhookClient(webhookDestination.getUrl());

                // Change appearance of webhook message
                WebhookMessageBuilder builder = new WebhookMessageBuilder();
                builder.setUsername(event.getMember().getEffectiveName());
                builder.setAvatarUrl(event.getMember().getUser().getAvatarUrl());

                for (Attachment attachment : event.getMessage().getAttachments()) {
                    builder.addFile(attachment.getFileName(), attachment.retrieveInputStream().join());
                }

                builder.setContent(translatedMessage);

                AllowedMentions mentions = new AllowedMentions();
                mentions.withParseEveryone(false);
                builder.setAllowedMentions(mentions);

                ReadonlyMessage webhookMessage = webhookClient.send(builder.build()).join();
                String webhookMessageId = String.valueOf(webhookMessage.getId());

                CacheManager.put(CacheCategory.MESSAGE_WEBHOOK_MAPPING, event.getMessageId(), webhookMessageId);
                CacheManager.put(CacheCategory.WEBHOOK_MESSAGE_MAPPING, webhookMessageId, event.getMessageId());
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }
}
