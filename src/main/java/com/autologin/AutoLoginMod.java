package com.autologin;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
        AutoLoginToast.init();

        // Fires when user clicks Save in the settings GUI — used to process transfer key import.
        AutoConfig.getConfigHolder(AutoLoginConfig.class).registerSaveListener((manager, data) -> {
            boolean changed = false;

            if (data.transferImportKey != null && !data.transferImportKey.isBlank()) {
                List<String> imported = PasswordCrypto.importTransferKey(data.transferImportKey.trim());
                if (imported != null) {
                    for (String entry : imported) {
                        int sep = entry.indexOf('=');
                        if (sep <= 0) continue;
                        String importedKey = entry.substring(0, sep).trim();
                        data.servers.removeIf(e -> {
                            int s = e.indexOf('=');
                            return s > 0 && e.substring(0, s).trim().equalsIgnoreCase(importedKey);
                        });
                        data.servers.add(entry);
                    }
                    LOGGER.info("[AutoLogin] Transfer key imported successfully.");
                } else {
                    LOGGER.warn("[AutoLogin] Transfer key is invalid or empty.");
                }
                data.transferImportKey = "";
                changed = true;
            }

            String freshKey = PasswordCrypto.generateTransferKey(data.servers);
            if (!freshKey.equals(data.transferExportKey)) {
                data.transferExportKey = freshKey;
                changed = true;
            }

            if (changed) {
                Minecraft.getInstance().execute(() ->
                        AutoConfig.getConfigHolder(AutoLoginConfig.class).save());
            }

            return net.minecraft.world.InteractionResult.SUCCESS;
        });

        migratePlaintextPasswords();
        LOGGER.info("[AutoLogin] Mod initialized. {} server(s) configured.", config.servers.size());

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            loginSentThisSession = false;
            if (client.getCurrentServer() != null) {
                currentServerIp = client.getCurrentServer().ip;
                LOGGER.info("[AutoLogin] Connected to: {} as {}",
                        currentServerIp, client.getUser().getName());
            } else {
                currentServerIp = null;
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            currentServerIp = null;
            loginSentThisSession = false;
        });

        // Auto-save password when player manually types an auth command.
        // Entry is stored as  server|nickname=ENC:...  so each account gets its own password.
        ClientSendMessageEvents.COMMAND.register(command -> {
            if (currentServerIp == null) return;
            if (loginSentThisSession) return;

            String trimmed = command.trim();
            String lower   = trimmed.toLowerCase();
            String password = extractPassword(lower, trimmed,
                    "login ", "l ", "register ", "reg ");

            if (password == null || password.isEmpty()) return;

            final String serverIp   = currentServerIp;
            final String playerName = Minecraft.getInstance().getUser().getName();
            final String entryKey   = serverIp + "|" + playerName;
            final String serverHost = AutoLoginConfig.stripPort(serverIp);

            config.servers.removeIf(entry -> {
                int sep = entry.indexOf('=');
                if (sep <= 0) return false;
                String key = entry.substring(0, sep).trim();
                // Remove exact server|nick match
                if (key.equalsIgnoreCase(entryKey)) return true;
                // Also remove old-format (no nick) entry for this server → migrate it
                if (key.indexOf('|') < 0) {
                    return key.equalsIgnoreCase(serverIp)
                            || AutoLoginConfig.stripPort(key).equalsIgnoreCase(serverHost);
                }
                return false;
            });

            config.servers.add(entryKey + "=" + PasswordCrypto.encode(password));
            AutoConfig.getConfigHolder(AutoLoginConfig.class).save();
            loginSentThisSession = true;
            LOGGER.info("[AutoLogin] Password saved for {}@{}", playerName, serverIp);

            AutoLoginToast.show(Component.translatable("autologin.message.saved", playerName, serverIp));
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) handleIncomingMessage(message.getString());
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            handleIncomingMessage(message.getString());
        });
    }

    private static String extractPassword(String lower, String original, String... prefixes) {
        for (String prefix : prefixes) {
            if (lower.startsWith(prefix)) {
                String rest = original.substring(prefix.length()).trim();
                if (rest.isEmpty()) return null;
                int space = rest.indexOf(' ');
                return space > 0 ? rest.substring(0, space) : rest;
            }
        }
        return null;
    }

    private void handleIncomingMessage(String messageText) {
        if (currentServerIp == null) return;
        if (loginSentThisSession) return;

        String playerName = Minecraft.getInstance().getUser().getName();
        String password   = config.getPassword(currentServerIp, playerName);
        if (password == null) return;

        if (!config.isLoginPrompt(messageText)) return;

        loginSentThisSession = true;
        LOGGER.info("[AutoLogin] Login prompt on {}. Sending login for {}...",
                currentServerIp, playerName);

        Minecraft minecraft = Minecraft.getInstance();
        long delay = Math.max(0, config.loginDelayMs);

        new Thread(() -> {
            if (delay > 0) {
                try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
            }
            minecraft.execute(() -> {
                if (minecraft.player != null && minecraft.getConnection() != null) {
                    minecraft.getConnection().sendCommand("login " + password);
                    LOGGER.info("[AutoLogin] Login command sent for {}@{}",
                            playerName, currentServerIp);
                    AutoLoginToast.show(Component.translatable("autologin.message.sent"));
                }
            });
        }, "autologin-thread").start();
    }

    public static AutoLoginConfig getConfig() {
        return config;
    }

    private static void migratePlaintextPasswords() {
        boolean changed = false;
        for (int i = 0; i < config.servers.size(); i++) {
            String entry = config.servers.get(i);
            int sep = entry.indexOf('=');
            if (sep <= 0) continue;
            String value = entry.substring(sep + 1);
            if (!PasswordCrypto.isEncoded(value)) {
                config.servers.set(i, entry.substring(0, sep + 1) + PasswordCrypto.encode(value));
                changed = true;
            }
        }
        if (changed) {
            AutoConfig.getConfigHolder(AutoLoginConfig.class).save();
            LOGGER.info("[AutoLogin] Migrated plaintext passwords to encoded format.");
        }
    }
}
