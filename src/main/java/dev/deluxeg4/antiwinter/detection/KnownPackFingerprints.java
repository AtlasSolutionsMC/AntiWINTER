package dev.deluxeg4.antiwinter.detection;

import dev.deluxeg4.antiwinter.AntiWinterPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.KnownPack;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public final class KnownPackFingerprints {
    private final File file;
    private final Map<User, Set<String>> current = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> baselines = new ConcurrentHashMap<>();

    public KnownPackFingerprints(AntiWinterPlugin plugin) {
        this(new File(plugin.getDataFolder(), "known-pack-fingerprints.yml"));
    }

    KnownPackFingerprints(File file) {
        this.file = file;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("fingerprints");
        if (section == null) return;
        for (String name : section.getKeys(false)) {
            baselines.put(name, Set.copyOf(section.getStringList(name)));
        }
    }

    // Each response replaces the previous snapshot, including an empty response.
    public void record(User user, List<KnownPack> packs) {
        Set<String> snapshot = new TreeSet<>();
        for (KnownPack pack : packs) {
            snapshot.add(encode(pack.getNamespace()) + ":" + encode(pack.getId())
                    + "@" + encode(pack.getVersion()));
        }
        current.put(user, Set.copyOf(snapshot));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public Set<String> packs(User user) {
        Set<String> snapshot = user == null ? null : current.get(user);
        if (snapshot == null) throw new IllegalArgumentException("No Known Packs response observed; reconnect with the plugin enabled.");
        return snapshot;
    }

    public List<String> baselineNames() {
        return baselines.keySet().stream().sorted().toList();
    }

    public synchronized void capture(String name, User user) throws IOException {
        String key = normalizeName(name);
        Set<String> captured = new TreeSet<>(packs(user));

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("fingerprints." + key, new ArrayList<>(captured));
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        config.save(file);
        baselines.put(key, Set.copyOf(captured));
    }

    public Comparison compare(String name, User user) {
        Set<String> expected = baselines.get(normalizeName(name));
        if (expected == null) return null;
        Set<String> observed = packs(user);
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(observed);
        Set<String> extra = new TreeSet<>(observed);
        extra.removeAll(expected);
        return new Comparison(missing.isEmpty() && extra.isEmpty(), missing, extra);
    }

    public void disconnect(User user) {
        current.remove(user);
    }

    public void clear() {
        current.clear();
    }

    private static String normalizeName(String name) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_-]{1,32}")) throw new IllegalArgumentException("Invalid fingerprint name");
        return normalized;
    }

    public record Comparison(boolean identical, Set<String> missing, Set<String> extra) {
        public Comparison {
            missing = Collections.unmodifiableSet(new TreeSet<>(missing));
            extra = Collections.unmodifiableSet(new TreeSet<>(extra));
        }
    }
}
