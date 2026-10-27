package dev.deluxeg4.antiwinter.utils;


import dev.deluxeg4.antiwinter.probe.Dot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class ProbeResponseUtil {
    private ProbeResponseUtil() {
    }

    public static Component component(Dot probe) {
        return switch (probe.mode()) {
            case METEOR, TRANSLATE -> Component.translatable(probe.key()).fallback(probe.fallback());
            case KEYBIND -> Component.keybind(probe.key());
        };
    }

    public static String plain(String line) {
        if (line == null) return "";
        try {
            return PlainTextComponentSerializer.plainText()
                    .serialize(GsonComponentSerializer.gson().deserialize(line));
        } catch (RuntimeException ignored) {
            return line;
        }
    }

    public static Evaluation evaluate(Dot probe, String response, boolean exploitProtection) {
        return evaluate(probe, response, exploitProtection, false);
    }

    public static Evaluation evaluate(Dot probe, String response, boolean exploitProtection, boolean packShimVerified) {
        if (response.isEmpty()) return Evaluation.NONE;
        String lowerResponse = response.toLowerCase(java.util.Locale.ROOT);
        String lowerKey = probe.key().toLowerCase(java.util.Locale.ROOT);
        if (packShimVerified && probe.mode() != dev.deluxeg4.antiwinter.probe.AEFft.KEYBIND) {
            if (lowerResponse.equals(probe.shim().toLowerCase(java.util.Locale.ROOT))
                    || lowerResponse.startsWith(probe.fallback().toLowerCase(java.util.Locale.ROOT))) return Evaluation.NONE;
            if (lowerResponse.equals(lowerKey)) return Evaluation.PROTECTED;
            return Evaluation.DETECTED;
        }
        if (lowerResponse.length() == lowerKey.length() + 1
                && lowerResponse.startsWith(lowerKey)
                && Character.isLetter(lowerResponse.charAt(lowerKey.length()))) return Evaluation.NONE;

        return switch (probe.mode()) {
            case METEOR -> lowerResponse.equals(lowerKey)
                    ? Evaluation.DETECTED
                    : lowerResponse.startsWith(probe.fallback().toLowerCase(java.util.Locale.ROOT))
                    ? Evaluation.NONE : Evaluation.DETECTED;
            case TRANSLATE -> lowerResponse.startsWith(probe.fallback().toLowerCase(java.util.Locale.ROOT))
                    ? Evaluation.NONE
                    : lowerResponse.equals(lowerKey) ? Evaluation.PROTECTED : Evaluation.DETECTED;
            case KEYBIND -> exploitProtection && lowerResponse.equals(lowerKey)
                    ? Evaluation.PROTECTED
                    : lowerResponse.equals(lowerKey) ? Evaluation.NONE : Evaluation.DETECTED;
        };
    }

    public record Evaluation(boolean detected, boolean protectedResult) {
        private static final Evaluation NONE = new Evaluation(false, false);
        private static final Evaluation DETECTED = new Evaluation(true, false);
        private static final Evaluation PROTECTED = new Evaluation(false, true);
    }
}
