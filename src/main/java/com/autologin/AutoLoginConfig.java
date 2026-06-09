package com.autologin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class AutoLoginConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("autologin.json");

    // server address (ip or ip:port) -> password
    public Map<String, String> servers = new LinkedHashMap<>();

    // Words/phrases that indicate a login prompt in a chat message
    public List<String> triggerWords = Arrays.asList(
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
    );

    // Milliseconds to wait before sending the command after detecting the prompt
    public long loginDelayMs = 300;

    public static AutoLoginConfig load() {
        File file = CONFIG_PATH.toFile();
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                AutoLoginConfig loaded = GSON.fromJson(reader, AutoLoginConfig.class);
                if (loaded != null) {
                    return loaded;
                }
            } catch (IOException e) {
                AutoLoginMod.LOGGER.error("[AutoLogin] Failed to read config: {}", e.getMessage());
            }
        }
        AutoLoginConfig defaults = new AutoLoginConfig();
        defaults.save();
        return defaults;
    }

    public void save() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            AutoLoginMod.LOGGER.error("[AutoLogin] Failed to save config: {}", e.getMessage());
        }
    }

    /**
     * Returns the password for the given server address, or null if not configured.
     * Tries exact match first, then match by hostname ignoring port.
     */
    public String getPassword(String serverAddress) {
        if (serverAddress == null) return null;

        // Exact match
        String password = servers.get(serverAddress);
        if (password != null) return password;

        // Match by hostname only (strip port from both sides)
        String incomingHost = stripPort(serverAddress);
        for (Map.Entry<String, String> entry : servers.entrySet()) {
            if (stripPort(entry.getKey()).equalsIgnoreCase(incomingHost)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String stripPort(String address) {
        int colon = address.lastIndexOf(':');
        if (colon > 0) {
            return address.substring(0, colon);
        }
        return address;
    }

    public boolean isLoginPrompt(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        for (String trigger : triggerWords) {
            if (lower.contains(trigger.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
