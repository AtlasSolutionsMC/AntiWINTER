package dev.deluxeg4.antiwinter.detection;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import dev.deluxeg4.antiwinter.AntiWinterPlugin;
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

public final class DetectionService implements Listener {
    private static final int KICK_VL = 2;
    private static final Set<String> KNOWN_BRANDS = Set.of(
            "vanilla", "minecraft", "fabric", "forge", "fml,forge", "neoforge", "quilt", "optifine",
            "lunarclient", "lunar client", "badlion", "badlionclient", "feather", "feather fabric",
            "dawn", "dawnclient", "dawn client",
            "labymod", "labymod3", "geyser"
    );

    private final AntiWinterPlugin plugin;
    private final Map<User, Set<String>> violations = new ConcurrentHashMap<>();

    public DetectionService(AntiWinterPlugin plugin) {
        this.plugin = plugin;
    }

    public void recordChannel(User user, UUID uuid, String name) {
        if (!"opsec".equalsIgnoreCase(name)) return;
        recordSignal(user, uuid, "opsec-channel", "advertised opsec via channel");
    }

    public void recordBrand(User user, UUID uuid, String brand) {
        String normalized = brand.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) return;
        if ("opsec".equals(normalized)) {
            recordSignal(user, uuid, "opsec-brand", "advertised opsec via brand");
        } else if (!KNOWN_BRANDS.contains(normalized) && !normalized.startsWith("lunarclient:")) {
            String displayBrand = brand.replaceAll("[\\p{Cntrl}]", "?");
            plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
                String message = "AntiWINTER: " + uuid + " sent an unrecognized client brand '" + displayBrand + "' (review only)";
                alertStaff(message);
            });
        }
    }

    private void recordSignal(User user, UUID uuid, String signal, String description) {
        Set<String> signals = violations.computeIfAbsent(user, ignored -> ConcurrentHashMap.newKeySet());
        if (!signals.add(signal)) return;

        int vl = signals.size();
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> {
            if (violations.get(user) != signals) return;
            alertStaff("AntiWINTER: " + uuid + " " + description + " (VL " + vl + "/" + KICK_VL + ")");
            if (vl >= KICK_VL) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) kick(player, user, signals);
            }
        });
    }

    private void alertStaff(String message) {
        plugin.getLogger().info(message);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            staff.getScheduler().execute(plugin, () -> {
                if (staff.hasPermission("antiwinter.alert")) staff.sendMessage(Component.text(message));
            }, null, 1L);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(event.getPlayer());
        if (user == null) return;
        Set<String> signals = violations.get(user);
        if (signals != null && signals.size() >= KICK_VL) kick(event.getPlayer(), user, signals);
    }

    private void kick(Player player, User user, Set<String> signals) {
        player.getScheduler().execute(plugin, () -> {
            if (violations.get(user) == signals
                    && PacketEvents.getAPI().getPlayerManager().getUser(player) == user) {
                player.kick(Component.text("Blocked: multiple client detection signals (VL " + KICK_VL + ")."));
            }
        }, null, 1L);
    }

    public void disconnect(User user) {
        violations.remove(user);
    }

    public void clear() {
        violations.clear();
    }

    public void recordProbe(User user, UUID uuid, String id, String displayName, boolean violation) {
        String message = "resolved a " + displayName + " translation key";
        if (violation) recordSignal(user, uuid, "probe:" + id, message);
        else plugin.getServer().getGlobalRegionScheduler().execute(plugin,
                () -> alertStaff("AntiWINTER: " + uuid + " " + message + " (review only; no VL)"));
    }

    public void recordProtection(User user, UUID uuid, String reason) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin,
                () -> alertStaff("AntiWINTER: " + uuid + " probe result inconclusive: " + reason));
    }

}
