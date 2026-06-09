package com.autologin;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Config(name = "autologin")
public class AutoLoginConfig implements ConfigData {

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

    // ── Helpers (not serialised) ──────────────────────────────────────────────

    public String getPassword(String serverAddress) {
        if (serverAddress == null) return null;
        String incomingHost = stripPort(serverAddress);
        for (String entry : servers) {
            int sep = entry.indexOf('=');
            if (sep <= 0) continue;
            String ip = entry.substring(0, sep).trim();
            if (ip.equalsIgnoreCase(serverAddress) || stripPort(ip).equalsIgnoreCase(incomingHost)) {
                return entry.substring(sep + 1);
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
