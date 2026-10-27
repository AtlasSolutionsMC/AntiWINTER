package dev.deluxeg4.antiwinter.probe;

import dev.deluxeg4.antiwinter.AntiWinterPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.Set;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PackShimService {
    public static final UUID PACK_ID = UUID.fromString("310e7568-ffce-4edf-9d4a-bfd69cc2f67a");
    public static final String CANARY_KEY = "antiwinter.probe.canary";
    public static final String CANARY_VALUE = "ANTIWINTER_PACK_CANARY_7F31";

    private final AntiWinterPlugin plugin;
    private final Set<UUID> requested = ConcurrentHashMap.newKeySet();
    private final Map<UUID, PlayerResourcePackStatusEvent.Status> statuses = new ConcurrentHashMap<>();
    private final AtomicBoolean warned = new AtomicBoolean();

    public PackShimService(AntiWinterPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean enabled() {
        if (!plugin.getConfig().getBoolean("pack-shim.enabled")) return false;
        String url = plugin.getConfig().getString("pack-shim.url", "").trim();
        String sha1 = plugin.getConfig().getString("pack-shim.sha1", "").trim();
        boolean valid = (url.startsWith("https://") || url.startsWith("http://"))
                && sha1.matches("(?i)[0-9a-f]{40}");
        if (!valid && warned.compareAndSet(false, true)) {
            plugin.getLogger().warning("Pack shim is enabled but needs an HTTP(S) URL and a 40-character SHA-1; using regular probes.");
        }
        return valid;
    }

    public void request(Player player) {
        requested.add(player.getUniqueId());
        String url = plugin.getConfig().getString("pack-shim.url", "").trim();
        String sha1 = plugin.getConfig().getString("pack-shim.sha1", "").trim();
        boolean required = plugin.getConfig().getBoolean("pack-shim.required", true);
        player.getScheduler().execute(plugin,
                () -> player.setResourcePack(PACK_ID, url, sha1,
                        Component.text("This server uses a client compatibility check."), required), null, 1L);
    }

    public boolean owns(PlayerResourcePackStatusEvent event) {
        return PACK_ID.equals(event.getID()) && requested.contains(event.getPlayer().getUniqueId());
    }

    public void status(PlayerResourcePackStatusEvent event) {
        statuses.put(event.getPlayer().getUniqueId(), event.getStatus());
    }

    public boolean resolved(UUID uuid) {
        PlayerResourcePackStatusEvent.Status status = statuses.get(uuid);
        return status != null && status != PlayerResourcePackStatusEvent.Status.ACCEPTED
                && status != PlayerResourcePackStatusEvent.Status.DOWNLOADED;
    }

    public boolean loaded(UUID uuid) {
        return statuses.get(uuid) == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED;
    }

    public boolean hasStatus(UUID uuid) {
        return statuses.containsKey(uuid);
    }

    public void forget(UUID uuid) {
        requested.remove(uuid);
        statuses.remove(uuid);
    }
}
