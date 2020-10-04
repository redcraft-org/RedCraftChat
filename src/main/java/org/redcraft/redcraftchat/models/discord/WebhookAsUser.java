package org.redcraft.redcraftchat.models.discord;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Message.Attachment;

public class WebhookAsUser {
    public TextChannel responseChannel;
    public Member member;
    public String content;
    public List<Attachment> attachments;

    public WebhookAsUser(TextChannel responseChannel, Member member, String content, List<Attachment> attachments) {
        this.responseChannel = responseChannel;
        this.member = member;
        this.content = content;
        this.attachments = attachments;
    }

    public String toString() {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.toJson(this);
    }
}