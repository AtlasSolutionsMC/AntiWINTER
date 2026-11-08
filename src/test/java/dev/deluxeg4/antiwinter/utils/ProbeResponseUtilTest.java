package dev.deluxeg4.antiwinter.utils;

import dev.deluxeg4.antiwinter.probe.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProbeResponseUtilTest {
    private ProbeDefinition probe(String id) {
        return ProbeCatalog.DEFAULT.stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }
    @Test void recordedMeteorAndXaeroFallbacksAreFilteringOnlyWithVerifiedPack() {
        for (String id : new String[]{"meteor", "xaeros-minimap"}) {
            var p = probe(id);
            for (String suffix : new String[]{"", "w"}) {
                var result = ProbeResponseUtil.evaluate(p, p.fallback() + suffix, false, true);
                assertTrue(result.filtered());
                assertFalse(result.detected());
                assertFalse(ProbeResponseUtil.evaluate(p, p.fallback() + suffix, false, false).filtered());
            }
        }
    }
    @Test void shimEmptyAndKeybindResponsesAreNotFiltering() {
        for (var p : ProbeCatalog.DEFAULT) {
            assertFalse(ProbeResponseUtil.evaluate(p, p.shim(), false, true).filtered());
            assertFalse(ProbeResponseUtil.evaluate(p, "", false, true).filtered());
            if (p.mode() == ProbeMode.KEYBIND) {
                assertFalse(ProbeResponseUtil.evaluate(p, p.key(), false, true).filtered());
            }
        }
    }
}
