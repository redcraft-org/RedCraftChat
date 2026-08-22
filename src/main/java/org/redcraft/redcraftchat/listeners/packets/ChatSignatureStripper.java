package org.redcraft.redcraftchat.listeners.packets;

import java.util.Collections;
import java.util.Optional;

import org.redcraft.redcraftchat.Config;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.util.crypto.MessageSignData;
import com.github.retrooper.packetevents.util.crypto.SaltSignature;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatCommand;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;

/**
 * Makes player chat arrive at the proxy unsigned.
 *
 * The whole chat pipeline works by cancelling the player's message and
 * re-emitting a translated copy to every recipient. Velocity refuses to do that
 * with a signed message, and 1.19 clients would get kicked for a broken
 * signature chain, so the signature is stripped from the packet before
 * Velocity's own decoder ever sees it.
 *
 * PacketEvents sits before "minecraft-decoder" in the player pipeline, so the
 * mutated bytes are all Velocity ever reads.
 */
public class ChatSignatureStripper extends PacketListenerAbstract {

    public ChatSignatureStripper() {
        // Lowest so nothing else can build a wrapper from the original bytes first
        super(PacketListenerPriority.LOWEST);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CHAT_MESSAGE) {
            stripChatMessage(event);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.CHAT_COMMAND) {
            stripChatCommand(event);
            return;
        }

        // 1.20.5+ unsigned commands carry no signature at all, nothing to do
        if (event.getPacketType() == PacketType.Login.Client.LOGIN_START) {
            stripLoginProfileKey(event);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Client.CHAT_SESSION_UPDATE && Config.stripLoginProfileKey) {
            // RemoteChatSession is immutable, the packet can only be dropped whole
            event.setCancelled(true);
        }
    }

    private void stripChatMessage(PacketReceiveEvent event) {
        WrapperPlayClientChatMessage wrapper = new WrapperPlayClientChatMessage(event);

        // Empty below 1.19, those clients send nothing to strip
        Optional<MessageSignData> signData = wrapper.getMessageSignData();
        if (!signData.isPresent()) {
            event.markForReEncode(false);
            return;
        }

        clearSignature(signData.get());

        // Never null the sign data itself, write() dereferences it unconditionally
        // from 1.19 on and would throw inside the decoder. The last seen messages
        // are left untouched so the acknowledgement chain keeps round tripping.
        event.markForReEncode(true);
    }

    private void stripChatCommand(PacketReceiveEvent event) {
        WrapperPlayClientChatCommand wrapper = new WrapperPlayClientChatCommand(event);

        MessageSignData signData = wrapper.getMessageSignData();
        if (signData != null) {
            clearSignature(signData);
        }

        // Velocity reads a command as signed when it carries argument signatures
        wrapper.setSignedArguments(Collections.emptyList());
        event.markForReEncode(true);
    }

    private void clearSignature(MessageSignData signData) {
        SaltSignature saltSignature = signData.getSaltSignature();

        // From 1.19.3 on the serializer writes the "signed" boolean as
        // signature.length != 0, so an empty array is what makes Velocity decode
        // the message as unsigned. On 1.19.0 the salt has to be zero as well or
        // the keyed chat packet is rejected outright.
        saltSignature.setSalt(0L);
        saltSignature.setSignature(new byte[0]);
        signData.setSignedPreview(false);
    }

    private void stripLoginProfileKey(PacketReceiveEvent event) {
        if (!Config.stripLoginProfileKey) {
            return;
        }

        ClientVersion clientVersion = event.getUser().getClientVersion();

        // The profile key only lives in Login Start between 1.19 and 1.19.2.
        // PacketEvents reads it up to and including 1.19.3, which misparses the
        // packet, so the version guard is not optional.
        if (clientVersion == null
                || clientVersion.isOlderThan(ClientVersion.V_1_19)
                || clientVersion.isNewerThanOrEquals(ClientVersion.V_1_19_3)) {
            return;
        }

        WrapperLoginClientLoginStart wrapper = new WrapperLoginClientLoginStart(event);
        if (wrapper.getSignatureData().isPresent()) {
            wrapper.setSignatureData(null);
            event.markForReEncode(true);
        } else {
            event.markForReEncode(false);
        }
    }
}
