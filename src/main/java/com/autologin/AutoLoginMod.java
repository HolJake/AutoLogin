package com.autologin;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoLoginMod implements ClientModInitializer {

    public static final String MOD_ID = "autologin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static AutoLoginConfig config;

    private String currentServerIp = null;
    private boolean loginSentThisSession = false;

    @Override
    public void onInitializeClient() {
        AutoConfig.register(AutoLoginConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(AutoLoginConfig.class).getConfig();
        LOGGER.info("[AutoLogin] Mod initialized. {} server(s) configured.", config.servers.size());

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            loginSentThisSession = false;
            if (client.getCurrentServer() != null) {
                currentServerIp = client.getCurrentServer().ip;
                LOGGER.info("[AutoLogin] Connected to: {}", currentServerIp);
            } else {
                currentServerIp = null;
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            currentServerIp = null;
            loginSentThisSession = false;
        });

        // Auto-save password when player manually types /login or /l
        ClientSendMessageEvents.COMMAND.register(command -> {
            if (currentServerIp == null) return;

            String trimmed = command.trim();
            String lower = trimmed.toLowerCase();
            String password = null;

            if (lower.startsWith("login ")) {
                password = trimmed.substring("login ".length()).trim();
            } else if (lower.startsWith("l ")) {
                password = trimmed.substring("l ".length()).trim();
            }

            if (password == null || password.isEmpty()) return;

            final String serverIp = currentServerIp;
            final String finalPassword = password;

            // Replace existing entry for this server (or add new one)
            config.servers.removeIf(entry -> {
                int sep = entry.indexOf('=');
                if (sep <= 0) return false;
                return entry.substring(0, sep).trim().equalsIgnoreCase(serverIp);
            });
            config.servers.add(serverIp + "=" + finalPassword);
            AutoConfig.getConfigHolder(AutoLoginConfig.class).save();
            loginSentThisSession = true;
            LOGGER.info("[AutoLogin] Password auto-saved for server: {}", serverIp);
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) handleIncomingMessage(message.getString());
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            handleIncomingMessage(message.getString());
        });
    }

    private void handleIncomingMessage(String messageText) {
        if (currentServerIp == null) return;
        if (loginSentThisSession) return;

        String password = config.getPassword(currentServerIp);
        if (password == null) return;

        if (!config.isLoginPrompt(messageText)) return;

        loginSentThisSession = true;
        LOGGER.info("[AutoLogin] Login prompt detected on {}. Sending login command...", currentServerIp);

        Minecraft minecraft = Minecraft.getInstance();
        long delay = Math.max(0, config.loginDelayMs);

        new Thread(() -> {
            if (delay > 0) {
                try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
            }
            minecraft.execute(() -> {
                if (minecraft.player != null && minecraft.getConnection() != null) {
                    minecraft.getConnection().sendCommand("login " + password);
                    LOGGER.info("[AutoLogin] Login command sent for server: {}", currentServerIp);
                }
            });
        }, "autologin-thread").start();
    }

    public static AutoLoginConfig getConfig() {
        return config;
    }
}
