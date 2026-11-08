package dev.deluxeg4.antiwinter.detection;

import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.KnownPack;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class KnownPackFingerprintsTest {
    @TempDir Path directory;

    @Test void missingResponseIsDifferentFromEmptyAndDisconnectClearsSession() throws Exception {
        var store = new KnownPackFingerprints(directory.resolve("packs.yml").toFile());
        var user = mock(User.class);
        assertThrows(IllegalArgumentException.class, () -> store.capture("missing", user));
        assertThrows(IllegalArgumentException.class, () -> store.packs(null));
        store.record(user, List.of());
        store.capture("empty", user);
        assertTrue(store.compare("empty", user).identical());
        store.disconnect(user);
        assertThrows(IllegalArgumentException.class, () -> store.packs(user));
        assertEquals(List.of("empty"), store.baselineNames());
    }

    @Test void persistencePreservesVersionCaseAndReportsMissingAndExtra() throws Exception {
        var file = directory.resolve("packs.yml").toFile();
        var store = new KnownPackFingerprints(file);
        var user = mock(User.class);
        var core = new KnownPack("minecraft", "core", "1.21.11");
        var mod = new KnownPack("example", "built_in", "ReleaseA");
        store.record(user, List.of(core, mod, core));
        store.capture("fabric", user);
        var reloaded = new KnownPackFingerprints(file);
        reloaded.record(user, List.of(mod, core));
        assertTrue(reloaded.compare("fabric", user).identical());
        reloaded.record(user, List.of(core, new KnownPack("example", "built_in", "releasea")));
        var result = reloaded.compare("fabric", user);
        assertFalse(result.identical());
        assertEquals(Set.of("example:built_in@ReleaseA"), result.missing());
        assertEquals(Set.of("example:built_in@releasea"), result.extra());
        assertThrows(IllegalArgumentException.class, () -> reloaded.capture("nested.name", user));
        assertNull(reloaded.compare("unknown", user));
        reloaded.clear();
        assertThrows(IllegalArgumentException.class, () -> reloaded.packs(user));
    }

    @Test void failedSaveDoesNotPublishBaselineAndEncodingIsUnambiguous() throws Exception {
        var store = new KnownPackFingerprints(directory.toFile());
        var user = mock(User.class);
        store.record(user, List.of(new KnownPack("a:b", "c", "1"), new KnownPack("a", "b:c", "1")));
        assertEquals(2, store.packs(user).size());
        assertThrows(java.io.IOException.class, () -> store.capture("failed", user));
        assertTrue(store.baselineNames().isEmpty());
    }
}
