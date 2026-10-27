package dev.deluxeg4.antiwinter.probe;

import com.github.retrooper.packetevents.util.Vector3i;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class ProbeScan {
    static final int PROBES_PER_SIGN = 3;

    final UUID uuid;
    final Location location;
    final Vector3i position;
    final boolean packShim;
    private List<Dot> definitions = WinterClient67.DEFAULT;
    private final Set<String> hits = new HashSet<>();
    private final Set<String> firstPassHits = new HashSet<>();
    private Set<String> confirmedHits = Set.of();
    int offset;
    int pass;
    private boolean protectionReported;

    ProbeScan(UUID uuid, Location location, Vector3i position, boolean packShim) {
        this.uuid = uuid;
        this.location = location;
        this.position = position;
        this.packShim = packShim;
    }

    synchronized List<Dot> nextBatch() {
        if (offset >= definitions.size()) return List.of();
        int end = Math.min(offset + PROBES_PER_SIGN, definitions.size());
        List<Dot> batch = List.copyOf(definitions.subList(offset, end));
        offset = end;
        return batch;
    }

    synchronized boolean advancePass() {
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

    synchronized void recordHit(String id) {
        hits.add(id);
    }

    synchronized Set<String> confirmedHits() {
        return confirmedHits;
    }

    synchronized boolean markProtectionReported() {
        if (protectionReported) return false;
        protectionReported = true;
        return true;
    }
}
