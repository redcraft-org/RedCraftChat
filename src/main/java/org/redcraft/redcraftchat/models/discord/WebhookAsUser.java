package org.redcraft.redcraftchat.models.discord;

import java.util.List;

import org.redcraft.redcraftchat.models.SerializableModel;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Message.Attachment;

public class WebhookAsUser extends SerializableModel {
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
}
