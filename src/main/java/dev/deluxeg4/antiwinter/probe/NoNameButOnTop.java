package dev.deluxeg4.antiwinter.probe;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import dev.deluxeg4.antiwinter.WinterOnTop;
import dev.deluxeg4.antiwinter.detection.Dojo;
import dev.deluxeg4.antiwinter.utils.ProbeResponseUtil;
import dev.deluxeg4.antiwinter.utils.ProbeResponseUtil.Evaluation;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NoNameButOnTop implements Listener {
    private static final String CONTROL_KEY = "key.forward";
    private static final long RESPONSE_TIMEOUT_TICKS = 20L;
    private static final long JOIN_DELAY_TICKS = 10L;
    private static final long PACK_STATUS_TIMEOUT_TICKS = 400L;

    private final WinterOnTop plugin;
    private final Dojo detections;
    private final PackShimService packShim;
    private final Map<User, ProbeScan> scans = new ConcurrentHashMap<>();
    private final Map<User, ActiveProbe> active = new ConcurrentHashMap<>();

    public NoNameButOnTop(WinterOnTop plugin, Dojo detections, PackShimService packShim) {
        this.plugin = plugin;
        this.detections = detections;
        this.packShim = packShim;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!packShim.enabled()) {
            player.getScheduler().runDelayed(plugin, task -> start(player, false), null, JOIN_DELAY_TICKS);
            return;
        }

        packShim.request(player);
        player.getScheduler().runDelayed(plugin, task -> {
            if (!packShim.loaded(player.getUniqueId())) {
                User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
                if (user != null && !packShim.resolved(player.getUniqueId())) {
                    detections.recordProtection(user, player.getUniqueId(), "pack-shim resource pack did not load before timeout");
                }
                start(player, false);
            }
        }, null, PACK_STATUS_TIMEOUT_TICKS);
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        if (!packShim.owns(event)) return;
        packShim.status(event);
        if (event.getStatus() == PlayerResourcePackStatusEvent.Status.ACCEPTED
                || event.getStatus() == PlayerResourcePackStatusEvent.Status.DOWNLOADED) return;
        Player player = event.getPlayer();
        boolean loaded = event.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED;
        if (!loaded && event.getStatus() != PlayerResourcePackStatusEvent.Status.ACCEPTED
                && event.getStatus() != PlayerResourcePackStatusEvent.Status.DOWNLOADED) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            if (user != null) detections.recordProtection(user, player.getUniqueId(),
                    "pack-shim resource pack was not loaded: " + event.getStatus());
        }
        player.getScheduler().runDelayed(plugin, task -> start(player, loaded), null,
                loaded ? JOIN_DELAY_TICKS : 1L);
    }

    private void start(Player player, boolean packShimEnabled) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null || scans.containsKey(user)) return;

        Location location = player.getLocation().getBlock().getLocation().add(0, 2, 0);
        if (location.getBlockY() >= location.getWorld().getMaxHeight() || !location.getBlock().isEmpty()) return;
        Vector3i position = new Vector3i(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        ProbeScan scan = new ProbeScan(player.getUniqueId(), location.clone(), position, packShimEnabled);
        if (scans.putIfAbsent(user, scan) == null) sendNextBatch(user, scan);
    }

    private void sendNextBatch(User user, ProbeScan scan) {
        if (scans.get(user) != scan) return;
        List<Dot> batch = scan.nextBatch();
        if (batch.isEmpty()) {
            if (scan.advancePass()) {
                sendNextBatch(user, scan);
            } else {
                for (Dot probe : WinterClient67.DEFAULT) {
                    if (scan.confirmedHits().contains(probe.id())) {
                        detections.recordProbe(user, scan.uuid, probe.id(), probe.displayName(), probe.violation());
                    }
                }
                finish(user, scan);
            }
            return;
        }

        Player player = Bukkit.getPlayer(scan.uuid);
        if (player == null) {
            finish(user, scan);
            return;
        }
        openBatch(player, user, scan, batch);
    }

    private void openBatch(Player player, User user, ProbeScan scan, List<Dot> batch) {
        if (scans.get(user) != scan || PacketEvents.getAPI().getPlayerManager().getUser(player) != user) return;
        ActiveProbe probe = new ActiveProbe(scan, batch);
        if (active.putIfAbsent(user, probe) != null) return;

        player.sendBlockChange(scan.location, Material.OAK_SIGN.createBlockData());
        List<Component> lines = new ArrayList<>(4);
        for (int i = 0; i < ProbeScan.PROBES_PER_SIGN; i++) {
            lines.add(i < batch.size() ? ProbeResponseUtil.component(batch.get(i)) : Component.empty());
        }
        lines.add(Component.keybind(CONTROL_KEY));
        if (scan.packShim) lines.set(3, Component.translatable(PackShimService.CANARY_KEY)
                .fallback(PackShimService.CANARY_KEY));
        player.sendSignChange(scan.location, lines);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerOpenSignEditor(scan.position, true));

        player.getScheduler().runDelayed(plugin,
                task -> player.sendBlockChange(scan.location, Material.AIR.createBlockData()), null, 1L);
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin,
                task -> timeout(user, probe), RESPONSE_TIMEOUT_TICKS);
    }

    public boolean handle(User user, UUID uuid, Vector3i position, String[] lines, boolean frontText) {
        ActiveProbe probe = active.get(user);
        if (probe == null || !probe.scan().position.equals(position) || !frontText || !active.remove(user, probe)) return false;

        String[] plainLines = new String[lines == null ? 0 : lines.length];
        for (int i = 0; i < plainLines.length; i++) plainLines[i] = ProbeResponseUtil.plain(lines[i]);
        String canary = plainLines.length > 3 ? plainLines[3].strip() : "";
        boolean packShimVerified = probe.scan().packShim
                && canary.equalsIgnoreCase(PackShimService.CANARY_VALUE);
        boolean exploitProtection = !probe.scan().packShim && canary.equalsIgnoreCase(CONTROL_KEY);
        if (probe.scan().packShim && !packShimVerified && probe.scan().markProtectionReported()) {
            detections.recordProtection(user, uuid, "pack-shim canary did not resolve; probe result is inconclusive");
            finish(user, probe.scan());
            return true;
        }
        if (exploitProtection && probe.scan().markProtectionReported()) {
            detections.recordProtection(user, uuid, "client returned the unresolved keybind " + CONTROL_KEY);
        }

        for (int i = 0; i < probe.definitions().size() && (!probe.scan().packShim || packShimVerified); i++) {
            String response = i < plainLines.length ? plainLines[i].strip() : "";
            Evaluation result = ProbeResponseUtil.evaluate(probe.definitions().get(i), response, exploitProtection, packShimVerified);
            if (result.detected()) probe.scan().recordHit(probe.definitions().get(i).id());
            if (result.protectedResult() && probe.scan().markProtectionReported()) {
                detections.recordProtection(user, uuid, "a translation probe was blocked or unresolved");
            }
        }
        if (Bukkit.getPlayer(uuid) == null) finish(user, probe.scan());
        else restore(probe.scan(), () -> sendNextBatch(user, probe.scan()));
        return true;
    }

    private void timeout(User user, ActiveProbe probe) {
        if (!active.remove(user, probe)) return;
        if (probe.scan().markProtectionReported()) {
            detections.recordProtection(user, probe.scan().uuid, "no sign response; result is inconclusive");
        }
        finish(user, probe.scan());
    }

    private void finish(User user, ProbeScan scan) {
        if (scans.remove(user, scan)) restore(scan);
        active.remove(user);
    }

    private void restore(ProbeScan scan) {
        restore(scan, null);
    }

    private void restore(ProbeScan scan, Runnable afterRestore) {
        Player player = Bukkit.getPlayer(scan.uuid);
        if (player == null) return;
        plugin.getServer().getRegionScheduler().execute(plugin, scan.location, () -> {
            BlockData current = scan.location.getBlock().getBlockData().clone();
            player.getScheduler().execute(plugin,
                    () -> {
                        player.sendBlockChange(scan.location, current);
                        if (afterRestore != null) afterRestore.run();
                    }, null, 1L);
        });
    }

    public void clear() {
        active.clear();
        scans.clear();
    }

    public void disconnect(User user) {
        active.remove(user);
        scans.remove(user);
        if (user.getUUID() != null) packShim.forget(user.getUUID());
    }
}
