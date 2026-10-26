package dev.deluxeg4.antiwinter.packet;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import dev.deluxeg4.antiwinter.detection.DetectionService;
import dev.deluxeg4.antiwinter.probe.TranslationProbeService;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class ClientPacketListener implements PacketListener {
    private final DetectionService detections;
    private final TranslationProbeService probes;

    public ClientPacketListener(DetectionService detections, TranslationProbeService probes) {
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
        if ("minecraft:brand".equals(channel)) {
            detections.recordBrand(event.getUser(), uuid, readBrand(data));
        } else if ("minecraft:register".equals(channel) && data.length <= 4096) {
            for (String name : new String(data, StandardCharsets.UTF_8).split("\u0000")) {
                int separator = name.indexOf(':');
                if (separator > 0) {
                    detections.recordChannel(event.getUser(), uuid, name.substring(0, separator));
                }
            }
        }
    }

    private static String readBrand(byte[] data) {
        int length = 0;
        int offset = 0;
        for (int shift = 0; shift < 35 && offset < data.length; shift += 7) {
            int next = data[offset++] & 0xff;
            length |= (next & 0x7f) << shift;
            if ((next & 0x80) == 0) {
                if (length < 0 || length > 256 || length != data.length - offset) return "";
                return new String(data, offset, length, StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        detections.disconnect(event.getUser());
        probes.disconnect(event.getUser());
    }
}
