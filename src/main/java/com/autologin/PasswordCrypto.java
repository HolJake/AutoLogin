package com.autologin;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class PasswordCrypto {

    private static final byte[] KEY = {
        0x41, 0x75, 0x74, 0x6F, 0x4C, 0x6F, 0x67, 0x69, 0x6E, 0x4B, 0x65, 0x79
    };
    private static final byte[] TRANSFER_KEY = {
        0x54, 0x72, 0x61, 0x6E, 0x73, 0x66, 0x65, 0x72, 0x4B, 0x65, 0x79, 0x21
    };

    static final String PREFIX = "ENC:";
    static final String TRANSFER_PREFIX = "ALTK:";

    private PasswordCrypto() {}

    public static String encode(String password) {
        return PREFIX + xorBase64(password.getBytes(StandardCharsets.UTF_8), KEY);
    }

    public static String decode(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) return stored;
        return xorDecodeBase64(stored.substring(PREFIX.length()), KEY);
    }

    public static boolean isEncoded(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /** Encodes all server entries into a portable transfer key string. */
    public static String generateTransferKey(List<String> servers) {
        if (servers == null || servers.isEmpty()) return TRANSFER_PREFIX;
        StringBuilder sb = new StringBuilder();
        for (String entry : servers) {
            int sep = entry.indexOf('=');
            if (sep <= 0) continue;
            String server = entry.substring(0, sep);
            String plainPass = decode(entry.substring(sep + 1));
            if (sb.length() > 0) sb.append('\n');
            sb.append(server).append('=').append(plainPass == null ? "" : plainPass);
        }
        if (sb.length() == 0) return TRANSFER_PREFIX;
        return TRANSFER_PREFIX + xorBase64(sb.toString().getBytes(StandardCharsets.UTF_8), TRANSFER_KEY);
    }

    /**
     * Decodes a transfer key back into encoded server entries.
     * Returns null if the key is invalid.
     */
    public static List<String> importTransferKey(String key) {
        if (key == null || !key.startsWith(TRANSFER_PREFIX)) return null;
        String payload = key.substring(TRANSFER_PREFIX.length());
        if (payload.isEmpty()) return null;
        try {
            String decoded = xorDecodeBase64(payload, TRANSFER_KEY);
            String[] lines = decoded.split("\n");
            List<String> result = new ArrayList<>();
            for (String line : lines) {
                int sep = line.indexOf('=');
                if (sep <= 0) continue;
                String server = line.substring(0, sep).trim();
                String pass = line.substring(sep + 1);
                if (server.isEmpty() || pass.isEmpty()) continue;
                result.add(server + "=" + encode(pass));
            }
            return result.isEmpty() ? null : result;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isTransferKey(String value) {
        return value != null && value.startsWith(TRANSFER_PREFIX);
    }

    private static String xorBase64(byte[] raw, byte[] key) {
        byte[] xored = new byte[raw.length];
        for (int i = 0; i < raw.length; i++) {
            xored[i] = (byte) (raw[i] ^ key[i % key.length]);
        }
        return Base64.getEncoder().encodeToString(xored);
    }

    private static String xorDecodeBase64(String b64, byte[] key) {
        byte[] xored = Base64.getDecoder().decode(b64);
        byte[] raw = new byte[xored.length];
        for (int i = 0; i < xored.length; i++) {
            raw[i] = (byte) (xored[i] ^ key[i % key.length]);
        }
        return new String(raw, StandardCharsets.UTF_8);
    }
}
