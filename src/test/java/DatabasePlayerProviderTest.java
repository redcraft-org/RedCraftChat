import junit.framework.*;

import java.util.Arrays;
import java.util.UUID;

import org.redcraft.redcraftchat.models.players.PlayerPreferences;
import org.redcraft.redcraftchat.players.providers.DatabasePlayerProvider;

/**
 * The two mappers in DatabasePlayerProvider are hand-written field lists, and
 * a copy-paste slip in both had the Minecraft block assigning
 * previousKnownDiscordName a second time instead of previousKnownMinecraftName,
 * which therefore never survived a trip through the database in either
 * direction. A full round trip with a distinct value in every field catches
 * that whole class of mistake.
 *
 * The provider's db field is null here, the mappers never touch it. The email
 * field is not asserted because rcc_player_preferences has no column for it.
 */
public class DatabasePlayerProviderTest extends TestCase {

    public void testEveryFieldSurvivesTheDatabaseRoundTrip() {
        PlayerPreferences original = new PlayerPreferences();
        original.internalUuid = "42";
        original.minecraftUuid = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        original.lastKnownMinecraftName = "CurrentMinecraftName";
        original.previousKnownMinecraftName = "PreviousMinecraftName";
        original.discordId = "123456789012345678";
        original.lastKnownDiscordName = "CurrentDiscordName";
        original.previousKnownDiscordName = "PreviousDiscordName";
        original.languages = Arrays.asList("fr", "en");
        original.mainLanguage = "fr";
        original.commandSpyEnabled = true;

        DatabasePlayerProvider provider = new DatabasePlayerProvider();
        PlayerPreferences restored = provider.transform(provider.transformToDatabase(original));

        assertEquals(original.internalUuid, restored.internalUuid);
        assertEquals(original.minecraftUuid, restored.minecraftUuid);
        assertEquals(original.lastKnownMinecraftName, restored.lastKnownMinecraftName);
        assertEquals(original.previousKnownMinecraftName, restored.previousKnownMinecraftName);
        assertEquals(original.discordId, restored.discordId);
        assertEquals(original.lastKnownDiscordName, restored.lastKnownDiscordName);
        assertEquals(original.previousKnownDiscordName, restored.previousKnownDiscordName);
        assertEquals(original.languages, restored.languages);
        assertEquals(original.mainLanguage, restored.mainLanguage);
        assertEquals(original.commandSpyEnabled, restored.commandSpyEnabled);
    }
}
