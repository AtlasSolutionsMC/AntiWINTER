package dev.deluxeg4.antiwinter.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public final class ClientChannelReader {
    private ClientChannelReader() {
    }

    public static boolean isRegister(String channel) {
        return "minecraft:register".equalsIgnoreCase(channel) || "REGISTER".equalsIgnoreCase(channel);
    }

    public static boolean isUnregister(String channel) {
        return "minecraft:unregister".equalsIgnoreCase(channel) || "UNREGISTER".equalsIgnoreCase(channel);
    }

    public static List<String> readRegistered(byte[] data) {
        if (data == null || data.length == 0 || data.length > 65535) return List.of();
        StringTokenizer tokens = new StringTokenizer(new String(data, StandardCharsets.UTF_8), "\0");
        List<String> channels = new ArrayList<>();
        while (tokens.hasMoreTokens()) channels.add(tokens.nextToken());
        return channels;
    }
}
