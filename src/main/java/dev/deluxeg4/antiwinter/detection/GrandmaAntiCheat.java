package dev.deluxeg4.antiwinter.detection;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import dev.deluxeg4.antiwinter.WinterOnTop;
import dev.deluxeg4.antiwinter.utils.WinterAlerts;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GrandmaAntiCheat implements Listener {
    private static final int KICK_VL = 2;
    private static final int METEOR_KICK_VL = 1;
    private static final String METEOR_SIGNAL = "probe:meteor";
    private static final Set<String> KNOWN_BRANDS = Set.of(
            "vanilla", "minecraft", "fabric", "forge", "fml,forge", "neoforge", "quilt", "optifine",
            "lunarclient", "lunar client", "badlion", "badlionclient", "feather", "feather fabric",
            "dawn", "dawnclient", "dawn client",
            "labymod", "labymod3", "geyser"
    );
    private static final Map<String, String> CLIENT_CHANNELS = Map.ofEntries(
            Map.entry("meteor", "Meteor Client"), Map.entry("meteor-client", "Meteor Client"),
            Map.entry("liquidbounce", "LiquidBounce"), Map.entry("wurst", "Wurst"),
            Map.entry("freecam", "Freecam"), Map.entry("xray", "XRay"),
            Map.entry("chestesp", "ChestESP"), Map.entry("killaura", "KillAura"),
            Map.entry("autofish", "AutoFish"), Map.entry("lumina", "Lumina"),
            Map.entry("autoswitch", "AutoSwitch"), Map.entry("bleachhack", "BleachHack"),
            Map.entry("aristois", "Aristois"), Map.entry("coffee", "Coffee Client"),
            Map.entry("worlddownloader", "World Downloader"), Map.entry("autoclicker", "AutoClicker"),
            Map.entry("antiafk", "AntiAFK"), Map.entry("autoclicker-fabric", "AutoClicker"),
            Map.entry("autoclickermc", "Auto Clicker"),
            Map.entry("simpleworlddownloader", "Simple World Downloader"),
            Map.entry("trouser-streak", "Trouser Streak"), Map.entry("trouserstreak", "Trouser Streak"),
            Map.entry("seedcrackerx", "SeedCrackerX"), Map.entry("glazed", "Glazed"),
            Map.entry("opsec", "OpSec")
    );

    private final WinterOnTop plugin;
    private final WinterAlerts alerts;
    private final ChannelFingerprints channelFingerprints;
    private final Map<User, Set<String>> violations = new ConcurrentHashMap<>();

    public GrandmaAntiCheat(WinterOnTop plugin) {
        this.plugin = plugin;
        this.alerts = new WinterAlerts(plugin);
        this.channelFingerprints = new ChannelFingerprints(plugin);
    }

    public void recordBrand(User user, UUID uuid, String brand) {
        String normalized = brand.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) return;
        if (!KNOWN_BRANDS.contains(normalized) && !normalized.startsWith("lunarclient:")) {
            String displayBrand = brand.replaceAll("[\\p{Cntrl}]", "?");
            recordSignal(user, uuid, "brand:unrecognized", "sent unrecognized client brand '" + displayBrand + "'", KICK_VL);
        }
    }

    public void recordChannel(User user, UUID uuid, String channel) {
        if (channel == null || channel.isBlank()) return;
        String normalized = channel.toLowerCase(java.util.Locale.ROOT);
        int separator = normalized.indexOf(':');
        if (separator <= 0) return;
        String namespace = normalized.substring(0, separator);
        String client = CLIENT_CHANNELS.get(namespace);
        if (client != null) {
            recordSignal(user, uuid, "channel:" + namespace, "advertised a " + client + " network channel", KICK_VL);
        }
    }

    public void registerChannels(User user, UUID uuid, java.util.List<String> channels) {
        channelFingerprints.register(uuid, channels);
        for (String channel : channels) recordChannel(user, uuid, channel);
    }

    public void unregisterChannels(UUID uuid, java.util.List<String> channels) {
        channelFingerprints.unregister(uuid, channels);
    }

    public ChannelFingerprints channelFingerprints() {
        return channelFingerprints;
    }

    private void recordSignal(User user, UUID uuid, String signal, String description, int kickVl) {
        Set<String> signals = violations.computeIfAbsent(user, ignored -> ConcurrentHashMap.newKeySet());
        if (!signals.add(signal)) return;

        int vl = signals.size();
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            if (violations.get(user) != signals) return;
            alerts.send("AntiWINTER: " + playerName(uuid) + " " + description + " (VL " + vl + "/" + kickVl + ")");
            if (vl >= kickVl) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) kick(player, user, signals);
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(event.getPlayer());
        if (user == null) return;
        UUID uuid = event.getPlayer().getUniqueId();
        channelFingerprints.begin(uuid);
        Set<String> channelMatches = channelFingerprints.exactMatches(uuid);
        if (channelMatches.size() == 1 && channelMatches.contains("opsec")) {
            plugin.getServer().getGlobalRegionScheduler().execute(plugin,
                    () -> alerts.send("AntiWINTER: " + event.getPlayer().getName()
                            + " channel fingerprint matches the saved OpSec baseline (review only; no VL)"));
        }
        Set<String> signals = violations.get(user);
        if (signals != null && signals.size() >= kickThreshold(signals)) kick(event.getPlayer(), user, signals);
    }

    private void kick(Player player, User user, Set<String> signals) {
        player.getScheduler().execute(plugin, () -> {
            if (violations.get(user) == signals
                    && PacketEvents.getAPI().getPlayerManager().getUser(player) == user) {
                player.kick(Component.text("Blocked: client detection signals (VL " + signals.size()
                        + "/" + kickThreshold(signals) + ")."));
            }
        }, null, 1L);
    }

    private static int kickThreshold(Set<String> signals) {
        return signals.contains(METEOR_SIGNAL) ? METEOR_KICK_VL : KICK_VL;
    }

    private String playerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player == null ? "unknown player" : player.getName();
    }

    public void disconnect(User user) {
        violations.remove(user);
        if (user.getUUID() != null) {
            channelFingerprints.disconnect(user.getUUID());
        }
    }

    public void clear() {
        violations.clear();
        channelFingerprints.clear();
    }

    public void recordProbe(User user, UUID uuid, String id, String displayName, boolean violation) {
        String message = "resolved a " + displayName + " translation key";
        if (violation) {
            int kickVl = "meteor".equals(id) ? METEOR_KICK_VL : KICK_VL;
            recordSignal(user, uuid, "probe:" + id, message, kickVl);
        }
        else plugin.getServer().getGlobalRegionScheduler().execute(plugin,
                () -> alerts.send("AntiWINTER: " + playerName(uuid) + " " + message + " (review only; no VL)"));
    }

    public void recordProtection(User user, UUID uuid, String reason) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin,
                () -> alerts.send("AntiWINTER: " + playerName(uuid) + " probe result inconclusive: " + reason));
    }

}
