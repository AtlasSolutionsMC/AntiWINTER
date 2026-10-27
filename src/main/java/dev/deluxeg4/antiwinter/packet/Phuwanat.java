package dev.deluxeg4.antiwinter.packet;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import dev.deluxeg4.antiwinter.detection.Dojo;
import dev.deluxeg4.antiwinter.probe.NoNameButOnTop;
import dev.deluxeg4.antiwinter.utils.BrandReader;
import dev.deluxeg4.antiwinter.utils.ClientChannelReader;
import java.util.UUID;

public final class Phuwanat implements PacketListener {
    private final Dojo detections;
    private final NoNameButOnTop probes;

    public Phuwanat(Dojo detections, NoNameButOnTop probes) {
        this.detections = detections;
        this.probes = probes;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        UUID uuid = event.getUser().getUUID();
        if (uuid == null) return;

        if (event.getPacketType() == PacketType.Play.Client.UPDATE_SIGN) {
            WrapperPlayClientUpdateSign update = new WrapperPlayClientUpdateSign(event);
            if (probes.handle(event.getUser(), uuid, update.getBlockPosition(), update.getTextLines(), update.isFrontText())) {
                event.setCancelled(true);
            }
            return;
        }

        String channel;
        byte[] data;
        if (event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            WrapperConfigClientPluginMessage packet = new WrapperConfigClientPluginMessage(event);
            channel = packet.getChannelName();
            data = packet.getData();
        } else if (event.getPacketType() == PacketType.Play.Client.PLUGIN_MESSAGE) {
            WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
            channel = packet.getChannelName();
            data = packet.getData();
        } else {
            return;
        }

        if (data == null) return;
        if (ClientChannelReader.isRegister(channel)) {
            detections.registerChannels(event.getUser(), uuid, ClientChannelReader.readRegistered(data));
            return;
        }
        if (ClientChannelReader.isUnregister(channel)) {
            detections.unregisterChannels(uuid, ClientChannelReader.readRegistered(data));
            return;
        }
        if ("minecraft:brand".equals(channel)) {
            detections.recordBrand(event.getUser(), uuid, BrandReader.read(data));
        }
        detections.recordChannel(event.getUser(), uuid, channel);
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        detections.disconnect(event.getUser());
        probes.disconnect(event.getUser());
    }
}
