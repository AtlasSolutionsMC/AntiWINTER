package dev.deluxeg4.antiwinter.probe;

import java.util.Locale;

public record ProbeDefinition(String id, String displayName, String key, ProbeMode mode, boolean violation) {
    public String fallback() {
        return "⟦NO_" + id.toUpperCase(Locale.ROOT).replace('-', '_') + "⟧";
    }
}
