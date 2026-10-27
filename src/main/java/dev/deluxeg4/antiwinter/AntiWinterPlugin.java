package dev.deluxeg4.antiwinter;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import dev.deluxeg4.antiwinter.detection.DetectionService;
import dev.deluxeg4.antiwinter.packet.ClientPacketListener;
import dev.deluxeg4.antiwinter.probe.ClientProbeService;
import dev.deluxeg4.antiwinter.probe.PackShimService;
import dev.deluxeg4.antiwinter.commands.WinterChannelsCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AntiWinterPlugin extends JavaPlugin {
    private DetectionService detections;
    private ClientProbeService probes;
    private PackShimService packShim;
    private PacketListenerCommon packetListener;

    @Override
    public void onLoad() {
        detections = new DetectionService(this);
        packShim = new PackShimService(this);
        probes = new ClientProbeService(this, detections, packShim);
        packetListener = PacketEvents.getAPI().getEventManager()
                .registerListener(new ClientPacketListener(detections, probes), PacketListenerPriority.NORMAL);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(detections, this);
        getServer().getPluginManager().registerEvents(probes, this);
        getCommand("winterchannels").setExecutor(new WinterChannelsCommand(detections));
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
        detections.clear();
        probes.clear();
    }
}
