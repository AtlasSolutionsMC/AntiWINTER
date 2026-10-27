package dev.deluxeg4.antiwinter.utils;

import java.nio.charset.StandardCharsets;

public final class BrandReader {
    private BrandReader() {
    }

    public static String read(byte[] data) {
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
}
