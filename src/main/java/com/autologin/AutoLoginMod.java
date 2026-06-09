package com.autologin;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
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

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                handleIncomingMessage(message.getString());
            }
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
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {}
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
