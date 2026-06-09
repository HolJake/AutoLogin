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
    @ConfigEntry.Gui.Tooltip(count = 2)
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
     * Priority: server|nickname entry → legacy server-only entry (backwards compat).
     */
    public String getPassword(String serverAddress, String playerName) {
        if (serverAddress == null) return null;
        String incomingHost = stripPort(serverAddress);

        // 1st pass — nickname-specific entry
        if (playerName != null && !playerName.isBlank()) {
            for (String entry : servers) {
                int eq = entry.indexOf('=');
                if (eq <= 0) continue;
                String key = entry.substring(0, eq).trim();
                int pipe = key.indexOf('|');
                if (pipe <= 0) continue; // no nickname in this entry
                String ip   = key.substring(0, pipe).trim();
                String nick = key.substring(pipe + 1).trim();
                if (!nick.equalsIgnoreCase(playerName)) continue;
                if (ip.equalsIgnoreCase(serverAddress) || stripPort(ip).equalsIgnoreCase(incomingHost)) {
                    return PasswordCrypto.decode(entry.substring(eq + 1));
                }
            }
        }

        // 2nd pass — legacy entry without nickname (any nickname)
        for (String entry : servers) {
            int eq = entry.indexOf('=');
            if (eq <= 0) continue;
            String key = entry.substring(0, eq).trim();
            if (key.indexOf('|') >= 0) continue; // has nickname, already checked above
            if (key.equalsIgnoreCase(serverAddress) || stripPort(key).equalsIgnoreCase(incomingHost)) {
                return PasswordCrypto.decode(entry.substring(eq + 1));
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
}
