package dev.deluxeg4.antiwinter.detection;

import com.github.retrooper.packetevents.util.KnownPack;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class KnownPackDetectionTest {
    @Test void recognizesBlockedOwnersAndExactIds() {
        assertEquals("meteor", DetectionService.knownPackSignature(new KnownPack("meteor", "built_in", "1")));
        assertEquals("meteor-client", DetectionService.knownPackSignature(new KnownPack("fabric", "meteor-client", "1")));
        assertEquals("wurst", DetectionService.knownPackSignature(new KnownPack("fabric", "wurst:built_in", "1")));
        assertEquals("opsec", DetectionService.knownPackSignature(new KnownPack("OpSec", "main", "1")));
    }

    @Test void doesNotTreatVanillaOrUnknownModsAsBlockedClients() {
        assertNull(DetectionService.knownPackSignature(new KnownPack("minecraft", "core", "1.21.11")));
        assertNull(DetectionService.knownPackSignature(new KnownPack("fabric", "sodium", "1")));
        assertNull(DetectionService.knownPackSignature(new KnownPack("example", "meteor-theme", "1")));
    }

    @Test void singleKnownPackSignalIsEnoughIncludingDeferredJoinEnforcement() {
        assertEquals(1, DetectionService.kickThreshold(Set.of("knownpack:meteor")));
        assertEquals(1, DetectionService.kickThreshold(Set.of("filter:xaeros-minimap")));
        assertEquals(1, DetectionService.kickThreshold(Set.of("knownpack:wurst", "brand:unrecognized")));
        assertEquals(2, DetectionService.kickThreshold(Set.of("brand:unrecognized")));
        assertEquals(1, DetectionService.kickThreshold(Set.of("probe:meteor")));
    }
}
