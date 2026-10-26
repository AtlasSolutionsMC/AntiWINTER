package dev.deluxeg4.antiwinter;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import dev.deluxeg4.antiwinter.detection.DetectionService;
import dev.deluxeg4.antiwinter.packet.ClientPacketListener;
import dev.deluxeg4.antiwinter.probe.TranslationProbeService;
import org.bukkit.plugin.java.JavaPlugin;

public final class AntiWinterPlugin extends JavaPlugin {
    private DetectionService detections;
    private TranslationProbeService probes;
    private PacketListenerCommon packetListener;

    @Override
    public void onLoad() {
        detections = new DetectionService(this);
        probes = new TranslationProbeService(this, detections);
        packetListener = PacketEvents.getAPI().getEventManager()
                .registerListener(new ClientPacketListener(detections, probes), PacketListenerPriority.NORMAL);
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(detections, this);
        getServer().getPluginManager().registerEvents(probes, this);
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
        detections.clear();
        probes.clear();
    }
}
