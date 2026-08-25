import junit.framework.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.redcraft.redcraftchat.listeners.packets.HologramTranslator;
import org.redcraft.redcraftchat.listeners.packets.HologramTranslator.CustomName;
import org.redcraft.redcraftchat.listeners.packets.HologramTranslator.NameShape;
import org.redcraft.redcraftchat.listeners.packets.HologramTranslator.TextChurn;

import com.github.retrooper.packetevents.protocol.entity.data.EntityData;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * The classification is what keeps this feature safe: everything the proxy
 * knows about an entity comes from these packets, and rewriting the name of
 * something that is not a hologram would corrupt real gameplay.
 *
 * The EntityData type parameter is null throughout: the EntityDataTypes
 * constants boot the whole PacketEvents registry, which needs a live API
 * instance a unit test does not have, and the classifiers never read it.
 */
public class HologramTranslatorTest extends TestCase {

    private static EntityData<?> flags(int bitmask) {
        return new EntityData<Byte>(0, null, (byte) bitmask);
    }

    private static EntityData<?> name(String legacy) {
        Component component = LegacyComponentSerializer.legacySection().deserialize(legacy);
        return new EntityData<Optional<Component>>(2, null, Optional.of(component));
    }

    public void testTheNameIsFoundByShapeNotByIndex() {
        // The custom name lands on a different index in other protocol
        // versions, the lookup must not care
        Component component = Component.text("Survival Vanilla");
        EntityData<?> misplacedName = new EntityData<Optional<Component>>(7, null, Optional.of(component));

        List<EntityData<?>> metadata = Arrays.asList(flags(0x20), misplacedName);
        CustomName found = HologramTranslator.findCustomName(metadata);

        assertNotNull(found);
        assertSame(misplacedName, found.entry);
        assertEquals(NameShape.COMPONENT, found.shape);
        assertEquals("Survival Vanilla", found.legacy);
    }

    public void testAnEmptyNameIsNotAName() {
        List<EntityData<?>> metadata = Arrays.asList(
                flags(0x20),
                new EntityData<Optional<Component>>(2, null, Optional.empty()));

        assertNull(HologramTranslator.findCustomName(metadata));
    }

    public void testOtherOptionalsAreNotMistakenForTheName() {
        // Optionals holding something other than a component or a json
        // component string must not match
        List<EntityData<?>> metadata = Arrays.asList(
                new EntityData<Optional<Integer>>(4, null, Optional.of(42)));

        assertNull(HologramTranslator.findCustomName(metadata));
    }

    public void testOldClientsGetAJsonStringName() {
        // 1.13 to 1.18.2 receive the name as a json component inside the
        // optional, and the rewrite must hand back the same shape
        EntityData<?> jsonName = new EntityData<Optional<String>>(2, null,
                Optional.of("{\"text\":\"Survival Vanilla\"}"));

        CustomName found = HologramTranslator.findCustomName(Arrays.asList(flags(0x20), jsonName));

        assertNotNull(found);
        assertEquals(NameShape.JSON, found.shape);
        assertEquals("Survival Vanilla", found.legacy);

        Object encoded = HologramTranslator.encodeName(NameShape.JSON, "Survie Vanilla");
        assertTrue(encoded instanceof Optional);
        assertTrue(((Optional<?>) encoded).get().toString().contains("Survie Vanilla"));
    }

    public void testTheOldestClientsGetAPlainStringName() {
        // Before 1.13 the name is a bare string on index 2 with colour codes
        EntityData<?> plainName = new EntityData<String>(2, null, "§9§lSurvival §1§lVanilla");

        CustomName found = HologramTranslator.findCustomName(Arrays.asList(flags(0x20), plainName));

        assertNotNull(found);
        assertEquals(NameShape.LEGACY_STRING, found.shape);
        assertEquals("§9§lSurvival §1§lVanilla", found.legacy);

        assertEquals("§9Survie", HologramTranslator.encodeName(NameShape.LEGACY_STRING, "§9Survie"));
    }

    public void testAPlainStringOffTheNameIndexIsIgnored() {
        // Only index 2 is the pre 1.13 name, other string fields must not match
        EntityData<?> stray = new EntityData<String>(5, null, "not a name");

        assertNull(HologramTranslator.findCustomName(Arrays.asList(flags(0x20), stray)));
    }

    public void testInvisibilityIsTheDiscriminator() {
        // A decorative build armor stand is visible, a hologram line is not
        assertTrue(HologramTranslator.isInvisible(Arrays.asList(flags(0x20), name("§7Label"))));
        assertFalse(HologramTranslator.isInvisible(Arrays.asList(flags(0x00), name("§7Label"))));
    }

    public void testInvisibilityIsReadFromTheEntityFlagsOnly() {
        // Index 15 is the armor stand's own bitmask, its 0x20 bit means
        // something else entirely and must not promote the entity
        List<EntityData<?>> metadata = Arrays.asList(
                new EntityData<Byte>(15, null, (byte) 0x20));

        assertFalse(HologramTranslator.isInvisible(metadata));
    }

    public void testNoFlagsMeansNotInvisible() {
        // A name only update carries no flags, promotion must wait for a
        // packet that shows the whole picture
        assertFalse(HologramTranslator.isInvisible(Arrays.asList(name("§7Label"))));
    }

    public void testLinesWithoutLettersAreSkipped() {
        // The colour codes must not count as letters, §e carries an e
        assertFalse(HologramTranslator.hasTranslatableText("§e▲ ▲ ▲"));
        assertFalse(HologramTranslator.hasTranslatableText("§7--------"));
        assertTrue(HologramTranslator.hasTranslatableText("§7You need to be a §bMember§7 to join"));
        // Accents are letters too
        assertTrue(HologramTranslator.hasTranslatableText("§7Réservé aux §bMembres"));
    }

    public void testACountdownIsDetectedAsDynamic() {
        // A line updating every second must trip the detector within the
        // window, well before it can burn provider quota
        TextChurn churn = new TextChurn();

        assertFalse(churn.recordChange(1_000));
        assertFalse(churn.recordChange(2_000));
        assertTrue(churn.recordChange(3_000));
    }

    public void testOccasionalEditsAreNotDynamic() {
        // An admin correcting a line a few times, minutes apart, must keep
        // the line translated
        TextChurn churn = new TextChurn();

        assertFalse(churn.recordChange(0));
        assertFalse(churn.recordChange(60_000));
        assertFalse(churn.recordChange(120_000));
        assertFalse(churn.recordChange(180_000));
    }

    public void testSlowCyclersAreCaughtByTheLifetimeTotal() {
        // A clock line changes once a minute, too gently for the window, but
        // it must not mint one cache entry per frame forever
        TextChurn churn = new TextChurn();

        long now = 0;
        boolean dynamic = false;
        for (int change = 0; change < HologramTranslator.DYNAMIC_TOTAL_CHANGES; change++) {
            dynamic = churn.recordChange(now);
            now += 60_000;
        }

        assertTrue(dynamic);
    }

    public void testTheCacheKeySeparatesLanguages() {
        String line = "§7You need to be a §bMember§7 to join";

        assertFalse(HologramTranslator.cacheKey("fr", line)
                .equals(HologramTranslator.cacheKey("de", line)));
        assertEquals(HologramTranslator.cacheKey("fr", line), HologramTranslator.cacheKey("fr", line));
    }
}
