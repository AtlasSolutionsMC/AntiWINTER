package dev.deluxeg4.antiwinter.detection;

import dev.deluxeg4.antiwinter.WinterOnTop;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChannelFingerprints {
    private final File file;
    private final Map<UUID, Set<String>> current = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> baselines = new ConcurrentHashMap<>();

    public ChannelFingerprints(WinterOnTop plugin) {
        this.file = new File(plugin.getDataFolder(), "channel-fingerprints.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("fingerprints");
        if (section == null) return;
        for (String name : section.getKeys(false)) {
            Set<String> channels = ConcurrentHashMap.newKeySet();
            for (String channel : section.getStringList(name)) channels.add(normalize(channel));
            baselines.put(name, channels);
        }
    }

    public void begin(UUID uuid) {
        current.computeIfAbsent(uuid, ignored -> ConcurrentHashMap.newKeySet());
    }

    public void register(UUID uuid, List<String> channels) {
        Set<String> observed = current.computeIfAbsent(uuid, ignored -> ConcurrentHashMap.newKeySet());
        for (String channel : channels) observed.add(normalize(channel));
    }

    public void unregister(UUID uuid, List<String> channels) {
        Set<String> observed = current.computeIfAbsent(uuid, ignored -> ConcurrentHashMap.newKeySet());
        for (String channel : channels) observed.remove(normalize(channel));
    }

    public Set<String> channels(UUID uuid) {
        Set<String> observed = current.get(uuid);
        return observed == null ? Set.of() : Set.copyOf(observed);
    }

    public List<String> baselineNames() {
        return baselines.keySet().stream().sorted().toList();
    }

    public Set<String> exactMatches(UUID uuid) {
        Set<String> observed = channels(uuid);
        Set<String> matches = new TreeSet<>();
        baselines.forEach((name, expected) -> {
            if (expected.equals(observed)) matches.add(name);
        });
        return Set.copyOf(matches);
    }

    public void capture(String name, UUID uuid) throws IOException {
        String key = normalizeName(name);
        Set<String> captured = new TreeSet<>(channels(uuid));
        baselines.put(key, ConcurrentHashMap.newKeySet());
        baselines.get(key).addAll(captured);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("fingerprints." + key, new ArrayList<>(captured));
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        config.save(file);
    }

    public Comparison compare(String name, UUID uuid) {
        Set<String> expected = baselines.get(normalizeName(name));
        if (expected == null) return null;
        Set<String> observed = channels(uuid);
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(observed);
        Set<String> extra = new TreeSet<>(observed);
        extra.removeAll(expected);
        return new Comparison(missing.isEmpty() && extra.isEmpty(), missing, extra);
    }

    public void disconnect(UUID uuid) {
        current.remove(uuid);
    }

    public void clear() {
        current.clear();
    }

    private static String normalize(String channel) {
        return channel.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeName(String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9._-]{1,32}")) throw new IllegalArgumentException("Invalid fingerprint name");
        return normalized;
    }

    public record Comparison(boolean identical, Set<String> missing, Set<String> extra) {
        public Comparison {
            missing = Collections.unmodifiableSet(new TreeSet<>(missing));
            extra = Collections.unmodifiableSet(new TreeSet<>(extra));
        }
    }
}
