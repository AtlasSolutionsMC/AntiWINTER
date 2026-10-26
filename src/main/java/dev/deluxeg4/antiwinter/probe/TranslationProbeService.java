package dev.deluxeg4.antiwinter.probe;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenSignEditor;
import dev.deluxeg4.antiwinter.AntiWinterPlugin;
import dev.deluxeg4.antiwinter.detection.DetectionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TranslationProbeService implements Listener {
    private static final String CONTROL_KEY = "key.forward";
    private static final int PROBES_PER_SIGN = 3;
    private static final long RESPONSE_TIMEOUT_TICKS = 40L;
    private static final long BETWEEN_BATCHES_TICKS = 5L;

    private final AntiWinterPlugin plugin;
    private final DetectionService detections;
    private final Map<User, Scan> scans = new ConcurrentHashMap<>();
    private final Map<User, ActiveProbe> active = new ConcurrentHashMap<>();

    private static final class Scan {
        private final UUID uuid;
        private final Location location;
        private final Vector3i position;
        private List<ProbeDefinition> definitions = ProbeCatalog.DEFAULT;
        private final Set<String> hits = new HashSet<>();
        private final Set<String> firstPassHits = new HashSet<>();
        private Set<String> confirmedHits = Set.of();
        private int offset;
        private int pass;
        private boolean protectionReported;

        private Scan(UUID uuid, Location location, Vector3i position) {
            this.uuid = uuid;
            this.location = location;
            this.position = position;
        }

        private synchronized List<ProbeDefinition> nextBatch() {
            if (offset >= definitions.size()) return List.of();
            int end = Math.min(offset + PROBES_PER_SIGN, definitions.size());
            List<ProbeDefinition> batch = List.copyOf(definitions.subList(offset, end));
            offset = end;
            return batch;
        }

        private synchronized boolean advancePass() {
            if (pass == 0) {
                firstPassHits.addAll(hits);
                if (firstPassHits.isEmpty()) return false;
                definitions = definitions.stream().filter(probe -> firstPassHits.contains(probe.id())).toList();
                hits.clear();
                offset = 0;
                pass = 1;
                return true;
            }
            confirmedHits = new HashSet<>(hits);
            confirmedHits.retainAll(firstPassHits);
            return false;
        }

        private synchronized void recordHit(String id) {
            hits.add(id);
        }

        private synchronized Set<String> confirmedHits() {
            return confirmedHits;
        }

        private synchronized boolean markProtectionReported() {
            if (protectionReported) return false;
            protectionReported = true;
            return true;
        }
    }

    private static final class ActiveProbe {
        private final Scan scan;
        private final List<ProbeDefinition> definitions;

        private ActiveProbe(Scan scan, List<ProbeDefinition> definitions) {
            this.scan = scan;
            this.definitions = definitions;
        }
    }

    private record Evaluation(boolean detected, boolean protectedResult) {
        private static final Evaluation NONE = new Evaluation(false, false);
        private static final Evaluation DETECTED = new Evaluation(true, false);
        private static final Evaluation PROTECTED = new Evaluation(false, true);
    }

    public TranslationProbeService(AntiWinterPlugin plugin, DetectionService detections) {
        this.plugin = plugin;
        this.detections = detections;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().getScheduler().runDelayed(plugin, task -> start(event.getPlayer()), null, 60L);
    }

    private void start(Player player) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null || scans.containsKey(user)) return;

        Location location = player.getLocation().getBlock().getLocation().add(0, 2, 0);
        if (location.getBlockY() >= location.getWorld().getMaxHeight() || !location.getBlock().isEmpty()) return;
        Vector3i position = new Vector3i(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        Scan scan = new Scan(player.getUniqueId(), location.clone(), position);
        if (scans.putIfAbsent(user, scan) == null) sendNextBatch(user, scan);
    }

    private void sendNextBatch(User user, Scan scan) {
        if (scans.get(user) != scan) return;
        List<ProbeDefinition> batch = scan.nextBatch();
        if (batch.isEmpty()) {
            if (scan.advancePass()) {
                sendNextBatch(user, scan);
            } else {
                for (ProbeDefinition probe : ProbeCatalog.DEFAULT) {
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
        player.getScheduler().runDelayed(plugin, task -> openBatch(player, user, scan, batch), null,
                scan.pass == 0 && scan.offset == batch.size() ? 1L : BETWEEN_BATCHES_TICKS);
    }

    private void openBatch(Player player, User user, Scan scan, List<ProbeDefinition> batch) {
        if (scans.get(user) != scan || PacketEvents.getAPI().getPlayerManager().getUser(player) != user) return;
        ActiveProbe probe = new ActiveProbe(scan, batch);
        if (active.putIfAbsent(user, probe) != null) return;

        player.sendBlockChange(scan.location, Material.OAK_SIGN.createBlockData());
        List<Component> lines = new ArrayList<>(4);
        for (int i = 0; i < PROBES_PER_SIGN; i++) {
            lines.add(i < batch.size() ? component(batch.get(i)) : Component.empty());
        }
        lines.add(Component.keybind(CONTROL_KEY));
        player.sendSignChange(scan.location, lines);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerOpenSignEditor(scan.position, true));

        player.getScheduler().runDelayed(plugin,
                task -> player.sendBlockChange(scan.location, Material.AIR.createBlockData()), null, 1L);
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin,
                task -> timeout(user, probe), RESPONSE_TIMEOUT_TICKS);
    }

    private static Component component(ProbeDefinition probe) {
        return switch (probe.mode()) {
            case METEOR, TRANSLATE -> Component.translatable(probe.key()).fallback(probe.fallback());
            case KEYBIND -> Component.keybind(probe.key());
        };
    }

    public boolean handle(User user, UUID uuid, Vector3i position, String[] lines, boolean frontText) {
        ActiveProbe probe = active.get(user);
        if (probe == null || !probe.scan.position.equals(position) || !frontText || !active.remove(user, probe)) return false;

        String[] plainLines = new String[lines == null ? 0 : lines.length];
        for (int i = 0; i < plainLines.length; i++) plainLines[i] = plain(lines[i]);
        boolean exploitProtection = plainLines.length > 3 && plainLines[3].strip().equalsIgnoreCase(CONTROL_KEY);
        if (exploitProtection && probe.scan.markProtectionReported()) {
            detections.recordProtection(user, uuid, "client returned the unresolved keybind " + CONTROL_KEY);
        }

        for (int i = 0; i < probe.definitions.size(); i++) {
            String response = i < plainLines.length ? plainLines[i].strip() : "";
            Evaluation result = evaluate(probe.definitions.get(i), response, exploitProtection);
            if (result.detected()) probe.scan.recordHit(probe.definitions.get(i).id());
            if (result.protectedResult() && probe.scan.markProtectionReported()) {
                detections.recordProtection(user, uuid, "a translation probe was blocked or unresolved");
            }
        }
        restore(probe.scan);
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) finish(user, probe.scan);
        else player.getScheduler().runDelayed(plugin, task -> sendNextBatch(user, probe.scan), null, BETWEEN_BATCHES_TICKS);
        return true;
    }

    private static Evaluation evaluate(ProbeDefinition probe, String response, boolean exploitProtection) {
        if (response.isEmpty()) return Evaluation.NONE;
        String lowerResponse = response.toLowerCase(java.util.Locale.ROOT);
        String lowerKey = probe.key().toLowerCase(java.util.Locale.ROOT);
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

    private static String plain(String line) {
        if (line == null) return "";
        try {
            return PlainTextComponentSerializer.plainText()
                    .serialize(GsonComponentSerializer.gson().deserialize(line));
        } catch (RuntimeException ignored) {
            return line;
        }
    }

    private void timeout(User user, ActiveProbe probe) {
        if (!active.remove(user, probe)) return;
        if (probe.scan.markProtectionReported()) {
            detections.recordProtection(user, probe.scan.uuid, "no sign response; result is inconclusive");
        }
        finish(user, probe.scan);
    }

    private void finish(User user, Scan scan) {
        if (scans.remove(user, scan)) restore(scan);
        active.remove(user);
    }

    private void restore(Scan scan) {
        Player player = Bukkit.getPlayer(scan.uuid);
        if (player == null) return;
        plugin.getServer().getRegionScheduler().execute(plugin, scan.location, () -> {
            BlockData current = scan.location.getBlock().getBlockData().clone();
            player.getScheduler().execute(plugin,
                    () -> player.sendBlockChange(scan.location, current), null, 1L);
        });
    }

    public void clear() {
        active.clear();
        scans.clear();
    }

    public void disconnect(User user) {
        active.remove(user);
        scans.remove(user);
    }
}
