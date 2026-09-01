import junit.framework.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.redcraft.redcraftchat.listeners.packets.SystemChatInterceptor;
import org.redcraft.redcraftchat.listeners.packets.SystemChatInterceptor.BufferedMessage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * A plugin printing a menu sends one packet per line, so the interceptor
 * collects what arrives together and translates it as one text. What may join
 * that text is the whole question: a clickable line would lose its click on
 * the way through legacy serialisation, and action bar text is a different
 * display that merely shares this packet, so neither belongs in a block with
 * chat.
 */
public class SystemChatInterceptorTest extends TestCase {

    private static BufferedMessage chat(String legacy) {
        return new BufferedMessage(LegacyComponentSerializer.legacySection().deserialize(legacy), false);
    }

    public void testTheLinesOfAMenuAreOneText() {
        List<BufferedMessage> batch = Arrays.asList(
                chat("§6Welcome to the"),
                chat("§eCreative Build"),
                chat("§6server!"));

        assertEquals(Arrays.asList("§6Welcome to the", "§eCreative Build", "§6server!"),
                SystemChatInterceptor.blockOf(batch));
    }

    public void testAClickableLineIsLeftAlone() {
        // Serialising it down to legacy text would drop the click, so it is
        // forwarded untouched and never joins the block
        BufferedMessage clickable = new BufferedMessage(
                Component.text("Click to teleport").clickEvent(ClickEvent.runCommand("/spawn")), false);

        assertNull(clickable.legacy);
        assertFalse(clickable.groupable);
        assertEquals(Collections.emptyList(), SystemChatInterceptor.blockOf(Collections.singletonList(clickable)));
    }

    public void testAClickDeepInTheTreeStillCounts() {
        Component message = Component.text("Open the ")
                .append(Component.text("menu").clickEvent(ClickEvent.runCommand("/menu")));

        assertTrue(SystemChatInterceptor.isInteractive(message));
        assertNull(new BufferedMessage(message, false).legacy);
    }

    public void testActionBarTextDoesNotJoinTheChatBlock() {
        // The action bar is its own surface, blocking it with chat would
        // translate two unrelated things as one sentence
        BufferedMessage actionBar = new BufferedMessage(Component.text("§eMining speed boost"), true);

        assertNotNull(actionBar.legacy);
        assertFalse(actionBar.groupable);
        assertEquals(Collections.emptyList(), SystemChatInterceptor.blockOf(Collections.singletonList(actionBar)));
    }

    public void testDecorationCarriesNothingToTranslate() {
        assertNull(chat("§7-----------------").legacy);
        assertNull(chat("§e▲ ▲ ▲").legacy);
    }

    public void testOnlyTheGroupableLinesFormTheBlockButOrderIsKept() {
        // A menu with a clickable line in the middle: the two plain lines are
        // still translated together, and the batch itself keeps every message
        BufferedMessage clickable = new BufferedMessage(
                Component.text("Click here").clickEvent(ClickEvent.runCommand("/spawn")), false);

        List<BufferedMessage> batch = Arrays.asList(
                chat("§6Welcome to the server"),
                clickable,
                chat("§6Have fun!"));

        assertEquals(Arrays.asList("§6Welcome to the server", "§6Have fun!"),
                SystemChatInterceptor.blockOf(batch));
        assertEquals(3, batch.size());
    }

    public void testOverlayIsLeftToTheActionBarPath() {
        boolean before = org.redcraft.redcraftchat.Config.translateActionBars;
        try {
            // Flag on: an overlay message belongs to ActionBarTranslator and
            // must not enter the buffer
            org.redcraft.redcraftchat.Config.translateActionBars = true;
            assertTrue(SystemChatInterceptor.leaveToActionBarPath(true));
            assertFalse(SystemChatInterceptor.leaveToActionBarPath(false));

            // Flag off: the pre-existing behaviour, overlay takes the buffer
            org.redcraft.redcraftchat.Config.translateActionBars = false;
            assertFalse(SystemChatInterceptor.leaveToActionBarPath(true));
        } finally {
            org.redcraft.redcraftchat.Config.translateActionBars = before;
        }
    }
}
