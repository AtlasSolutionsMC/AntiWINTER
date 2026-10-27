package dev.deluxeg4.antiwinter;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import dev.deluxeg4.antiwinter.detection.Dojo;
import dev.deluxeg4.antiwinter.packet.Phuwanat;
import dev.deluxeg4.antiwinter.probe.NoNameButOnTop;
import dev.deluxeg4.antiwinter.probe.PackShimService;
import dev.deluxeg4.antiwinter.commands.WinterChannelsCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class WinterOnTop extends JavaPlugin {
    private Dojo detections;
    private NoNameButOnTop probes;
    private PackShimService packShim;
    private PacketListenerCommon packetListener;

    @Override
    public void onLoad() {
        detections = new Dojo(this);
        packShim = new PackShimService(this);
        probes = new NoNameButOnTop(this, detections, packShim);
        packetListener = PacketEvents.getAPI().getEventManager()
                .registerListener(new Phuwanat(detections, probes), PacketListenerPriority.NORMAL);
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
