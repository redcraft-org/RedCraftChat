import java.util.UUID;

import junit.framework.*;

import org.redcraft.redcraftchat.minecraft.BedrockPlayers;

/**
 * Floodgate mints a Bedrock player's UUID as new UUID(0, xuid), and its own
 * isFloodgateId is exactly this comparison. Checked against the 14 Bedrock
 * rows on the live network, where it matched every one and no Java player.
 */
public class BedrockDetectionTest extends TestCase {

    public void testFloodgateUuidsHaveAZeroHighHalf() {
        // Real shapes taken from the live database
        assertTrue(BedrockPlayers.isFloodgateUuid(UUID.fromString("00000000-0000-0000-0009-0000023c61aa")));
        assertTrue(BedrockPlayers.isFloodgateUuid(UUID.fromString("00000000-0000-0000-0009-01f883db6ff8")));
        assertTrue(BedrockPlayers.isFloodgateUuid(new UUID(0L, 1234567890L)));
    }

    public void testAMojangUuidIsNeverMistakenForBedrock() {
        // A version 4 UUID always carries its version nibble in the high half,
        // so the high half cannot be zero
        for (int i = 0; i < 200; i++) {
            assertFalse(BedrockPlayers.isFloodgateUuid(UUID.randomUUID()));
        }
    }

    public void testNullIsNotBedrock() {
        assertFalse(BedrockPlayers.isFloodgateUuid(null));
        assertFalse(BedrockPlayers.isBedrock(null));
    }

    public void testTheAllZeroUuidCountsAsFloodgate() {
        // Degenerate but consistent: an xuid of zero is still a zero high half,
        // and treating it as Java would hand it a surface it cannot render
        assertTrue(BedrockPlayers.isFloodgateUuid(new UUID(0L, 0L)));
    }
}
