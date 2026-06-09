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
    @ConfigEntry.Gui.Tooltip(count = 1)
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

    /** Set to true by validatePostLoad when the config needs to be re-saved. */
    @ConfigEntry.Gui.Excluded
    public transient boolean needsSave = false;

    // ── ConfigData ────────────────────────────────────────────────────────────

    @Override
    public void validatePostLoad() {
        boolean changed = false;

        // Process a pending import
        if (transferImportKey != null && !transferImportKey.isBlank()) {
            List<String> imported = PasswordCrypto.importTransferKey(transferImportKey.trim());
            if (imported != null) {
                for (String entry : imported) {
                    int sep = entry.indexOf('=');
                    if (sep <= 0) continue;
                    String ip = entry.substring(0, sep).trim();
                    servers.removeIf(e -> {
                        int s = e.indexOf('=');
                        return s > 0 && e.substring(0, s).trim().equalsIgnoreCase(ip);
                    });
                    servers.add(entry);
                }
                changed = true;
            }
            transferImportKey = "";
            changed = true;
        }

        // Regenerate export key to keep it in sync with current servers
        String freshKey = PasswordCrypto.generateTransferKey(servers);
        if (!freshKey.equals(transferExportKey)) {
            transferExportKey = freshKey;
            changed = true;
        }

        needsSave = changed;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public String getPassword(String serverAddress) {
        if (serverAddress == null) return null;
        String incomingHost = stripPort(serverAddress);
        for (String entry : servers) {
            int sep = entry.indexOf('=');
            if (sep <= 0) continue;
            String ip = entry.substring(0, sep).trim();
            if (ip.equalsIgnoreCase(serverAddress) || stripPort(ip).equalsIgnoreCase(incomingHost)) {
                return PasswordCrypto.decode(entry.substring(sep + 1));
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

    private static String stripPort(String address) {
        int colon = address.lastIndexOf(':');
        return colon > 0 ? address.substring(0, colon) : address;
    }
}
