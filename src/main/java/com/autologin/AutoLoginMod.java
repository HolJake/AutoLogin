package com.autologin;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoLoginMod implements ClientModInitializer {

    public static final String MOD_ID = "autologin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static AutoLoginConfig config;

    // IP of the currently connected server, null when not connected
    private String currentServerIp = null;

    // Cooldown to avoid sending the password multiple times per session
    private boolean loginSentThisSession = false;

    @Override
    public void onInitializeClient() {
        config = AutoLoginConfig.load();
        LOGGER.info("[AutoLogin] Mod initialized. {} server(s) configured.", config.servers.size());

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            loginSentThisSession = false;
            if (client.getCurrentServerEntry() != null) {
                currentServerIp = client.getCurrentServerEntry().address;
                LOGGER.info("[AutoLogin] Connected to: {}", currentServerIp);
            } else {
                currentServerIp = null;
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            currentServerIp = null;
            loginSentThisSession = false;
        });

        // Listen for system/game messages (most auth plugins send these)
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                handleIncomingMessage(message.getString());
            }
        });

        // Also listen for regular chat messages (some plugins use chat channel)
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

        MinecraftClient client = MinecraftClient.getInstance();
        long delay = Math.max(0, config.loginDelayMs);

        // Schedule on the main thread after the configured delay
        new Thread(() -> {
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {
                }
            }
            client.execute(() -> {
                if (client.player != null && client.getNetworkHandler() != null) {
                    client.getNetworkHandler().sendChatCommand("login " + password);
                    LOGGER.info("[AutoLogin] Login command sent for server: {}", currentServerIp);
                }
            });
        }, "autologin-thread").start();
    }

    public static AutoLoginConfig getConfig() {
        return config;
    }
}
