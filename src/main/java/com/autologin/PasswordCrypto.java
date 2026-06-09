package com.autologin;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class PasswordCrypto {

    private static final byte[] KEY = {
        0x41, 0x75, 0x74, 0x6F, 0x4C, 0x6F, 0x67, 0x69, 0x6E, 0x4B, 0x65, 0x79
    };
    private static final String PREFIX = "ENC:";

    private PasswordCrypto() {}

    public static String encode(String password) {
        byte[] raw = password.getBytes(StandardCharsets.UTF_8);
        byte[] xored = new byte[raw.length];
        for (int i = 0; i < raw.length; i++) {
            xored[i] = (byte) (raw[i] ^ KEY[i % KEY.length]);
        }
        return PREFIX + Base64.getEncoder().encodeToString(xored);
    }

    public static String decode(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) return stored;
        byte[] xored = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
        byte[] raw = new byte[xored.length];
        for (int i = 0; i < xored.length; i++) {
            raw[i] = (byte) (xored[i] ^ KEY[i % KEY.length]);
        }
        return new String(raw, StandardCharsets.UTF_8);
    }

    public static boolean isEncoded(String value) {
        return value != null && value.startsWith(PREFIX);
    }
}
