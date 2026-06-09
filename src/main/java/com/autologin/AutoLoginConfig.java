package com.autologin;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Config(name = "autologin")
public class AutoLoginConfig implements ConfigData {

    public enum Corner { TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT }

    @ConfigEntry.Category("servers")
    @ConfigEntry.Gui.Tooltip(count = 3)
    public List<String> servers = new ArrayList<>();

    @ConfigEntry.Category("mod")
    @ConfigEntry.Gui.Tooltip(count = 1)
    public List<String> triggerWords = new ArrayList<>(Arrays.asList(
            "/login",
            "/l ",
            "please login",
            "please log in",
            "войдите",
            "авторизуйтесь",
            "введите пароль",
            "you need to login",
            "you must login",
            "use /login"
    ));

    @ConfigEntry.Category("mod")
    public long loginDelayMs = 300L;

    @ConfigEntry.Category("mod")
    public Corner notificationCorner = Corner.BOTTOM_RIGHT;

    // ── Transfer ──────────────────────────────────────────────────────────────

    /** Auto-generated export key. Copy it to transfer passwords to another device. */
    @ConfigEntry.Category("transfer")
    @ConfigEntry.Gui.Tooltip(count = 4)
    public String transferExportKey = "";

    /** Paste an export key here and save to import passwords from another device. */
    @ConfigEntry.Category("transfer")
    @ConfigEntry.Gui.Tooltip(count = 4)
    public String transferImportKey = "";

    // ── ConfigData ────────────────────────────────────────────────────────────

    @Override
    public void validatePostLoad() {
        transferExportKey = PasswordCrypto.generateTransferKey(servers);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Looks up the password for the given server and player nickname.
     *
     * Priority (highest to lowest):
     *   1. Exact server|nick
     *   2. Same root domain + nick  (e.g. msk.holyworld.ru matches play.holyworld.ru)
     *   3. Exact server  (legacy, no nick)
     *   4. Same root domain  (legacy, no nick)
     */
    public String getPassword(String serverAddress, String playerName) {
        if (serverAddress == null) return null;
        String incomingHost = stripPort(serverAddress);
        String incomingRoot = rootDomain(incomingHost); // null for IPs / short names

        boolean hasNick = playerName != null && !playerName.isBlank();

        // ── Pass 1: exact server|nick ─────────────────────────────────────────
        if (hasNick) {
            for (String entry : servers) {
                int eq = entry.indexOf('=');
                if (eq <= 0) continue;
                String key = entry.substring(0, eq).trim();
                int pipe = key.indexOf('|');
                if (pipe <= 0) continue;
                String ip   = key.substring(0, pipe).trim();
                String nick = key.substring(pipe + 1).trim();
                if (!nick.equalsIgnoreCase(playerName)) continue;
                if (ip.equalsIgnoreCase(serverAddress) || stripPort(ip).equalsIgnoreCase(incomingHost)) {
                    return PasswordCrypto.decode(entry.substring(eq + 1));
                }
            }
        }

        // ── Pass 2: root-domain|nick ──────────────────────────────────────────
        if (hasNick && incomingRoot != null) {
            for (String entry : servers) {
                int eq = entry.indexOf('=');
                if (eq <= 0) continue;
                String key = entry.substring(0, eq).trim();
                int pipe = key.indexOf('|');
                if (pipe <= 0) continue;
                String ip   = key.substring(0, pipe).trim();
                String nick = key.substring(pipe + 1).trim();
                if (!nick.equalsIgnoreCase(playerName)) continue;
                String storedHost = stripPort(ip);
                if (storedHost.equalsIgnoreCase(incomingHost)) continue; // already checked in pass 1
                if (incomingRoot.equalsIgnoreCase(rootDomain(storedHost))) {
                    return PasswordCrypto.decode(entry.substring(eq + 1));
                }
            }
        }

        // ── Pass 3: exact server (legacy, no nick) ────────────────────────────
        for (String entry : servers) {
            int eq = entry.indexOf('=');
            if (eq <= 0) continue;
            String key = entry.substring(0, eq).trim();
            if (key.indexOf('|') >= 0) continue;
            if (key.equalsIgnoreCase(serverAddress) || stripPort(key).equalsIgnoreCase(incomingHost)) {
                return PasswordCrypto.decode(entry.substring(eq + 1));
            }
        }

        // ── Pass 4: root-domain (legacy, no nick) ─────────────────────────────
        if (incomingRoot != null) {
            for (String entry : servers) {
                int eq = entry.indexOf('=');
                if (eq <= 0) continue;
                String key = entry.substring(0, eq).trim();
                if (key.indexOf('|') >= 0) continue;
                String storedHost = stripPort(key);
                if (storedHost.equalsIgnoreCase(incomingHost)) continue; // already checked in pass 3
                if (incomingRoot.equalsIgnoreCase(rootDomain(storedHost))) {
                    return PasswordCrypto.decode(entry.substring(eq + 1));
                }
            }
        }

        return null;
    }

    public boolean isLoginPrompt(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        for (String trigger : triggerWords) {
            if (lower.contains(trigger.toLowerCase())) return true;
        }
        return false;
    }

    static String stripPort(String address) {
        int colon = address.lastIndexOf(':');
        return colon > 0 ? address.substring(0, colon) : address;
    }

    /**
     * Returns the root domain (last two labels) of a hostname, or null if the
     * host is an IP address or has fewer than three labels (no subdomain to strip).
     *
     * Examples:
     *   msk.holyworld.ru  →  holyworld.ru
     *   play.holyworld.ru →  holyworld.ru
     *   mc.hypixel.net    →  hypixel.net
     *   hypixel.net       →  null  (already root — exact match handles it)
     *   192.168.1.1       →  null  (IP address)
     *   localhost         →  null  (no dots)
     */
    static String rootDomain(String host) {
        if (host == null || host.isEmpty()) return null;
        // Numeric IPv4 — no domain matching
        if (host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) return null;
        String[] parts = host.split("\\.");
        if (parts.length < 3) return null; // no subdomain present
        return parts[parts.length - 2] + "." + parts[parts.length - 1];
    }
}
