package com.autologin;

import com.mojang.authlib.GameProfile;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

public class AutoLoginMod implements ClientModInitializer {

    public static final String MOD_ID = "autologin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Method PROFILE_NAME_METHOD = findProfileNameMethod();

    private static AutoLoginConfig config;

    private String currentServerIp = null;
    private boolean loginSentThisSession = false;
    private long connectionGeneration;

    @Override
    public void onInitializeClient() {
        AutoConfig.register(AutoLoginConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(AutoLoginConfig.class).getConfig();
        AutoLoginToast.init();
        KeyMapping openMenu = MenuKeyBinding.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.consumeClick() && !AutoLoginScreen.isOpen(client)) {
                AutoLoginScreen.open(client);
            }
        });

        importPendingTransferKey();
        migratePlaintextPasswords();
        LOGGER.info("[AutoLogin] Mod initialized. {} server(s) configured.", config.servers.size());

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            connectionGeneration++;
            loginSentThisSession = false;
            if (client.getCurrentServer() != null) {
                currentServerIp = client.getCurrentServer().ip;
                LOGGER.info("[AutoLogin] Connected to: {} as {}",
                        currentServerIp, connectedAccountName(client));
            } else {
                currentServerIp = null;
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            connectionGeneration++;
            currentServerIp = null;
            loginSentThisSession = false;
        });

        // Auto-save password when player manually types an auth command.
        // Entry is stored as  server|nickname=ENC:...  so each account gets its own password.
        ClientSendMessageEvents.COMMAND.register(command -> {
            if (currentServerIp == null) return;
            if (!config.autoSavePasswords) return;

            String trimmed = command.trim();
            String lower   = trimmed.toLowerCase();
            String password = extractPassword(lower, trimmed,
                    "login ", "l ", "register ", "reg ");

            if (password == null || password.isEmpty()) return;

            final String serverIp   = currentServerIp;
            final String playerName = connectedAccountName(Minecraft.getInstance());
            if (playerName == null) return;
            final String entryKey   = serverIp + "|" + playerName;

            config.servers.removeIf(entry -> {
                int sep = entry.indexOf('=');
                if (sep <= 0) return false;
                String key = entry.substring(0, sep).trim();
                // Keep legacy server-only entries: another account may still use them.
                return key.equalsIgnoreCase(entryKey);
            });

            config.servers.add(entryKey + "=" + PasswordCrypto.encode(password));
            saveConfig();
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
        if (!config.autoLoginEnabled) return;
        if (!config.isLoginPrompt(messageText)) return;

        String playerName = connectedAccountName(Minecraft.getInstance());
        if (playerName == null) return;
        String password   = config.getPassword(currentServerIp, playerName);
        if (password == null) return;

        loginSentThisSession = true;
        String rootDomain = AutoLoginConfig.rootDomain(AutoLoginConfig.stripPort(currentServerIp));
        LOGGER.info("[AutoLogin] Login prompt on {} (domain: {}). Sending login for {}...",
                currentServerIp, rootDomain != null ? rootDomain : currentServerIp, playerName);

        Minecraft minecraft = Minecraft.getInstance();
        String expectedServer = currentServerIp;
        long expectedGeneration = connectionGeneration;
        long delay = Math.max(0, config.loginDelayMs);

        new Thread(() -> {
            if (delay > 0) {
                try { Thread.sleep(delay); } catch (InterruptedException ignored) {}
            }
            minecraft.execute(() -> {
                if (minecraft.player != null && minecraft.getConnection() != null
                        && expectedServer.equals(currentServerIp) && loginSentThisSession
                        && expectedGeneration == connectionGeneration
                        && playerName.equals(connectedAccountName(minecraft))
                        && config.autoLoginEnabled) {
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

    /** The profile captured for this connection remains stable if another mod changes the menu session. */
    static String connectedAccountName(Minecraft client) {
        ClientPacketListener connection = client.getConnection();
        if (connection != null) {
            String name = profileName(connection.getLocalGameProfile());
            if (name != null) return name;
        }
        return client.player == null ? null : profileName(client.player.getGameProfile());
    }

    static String activeAccountName(Minecraft client) {
        String connected = connectedAccountName(client);
        if (connected != null) return connected;
        User user = client.getUser();
        return user == null || user.getName().isBlank() ? null : user.getName();
    }

    private static String profileName(GameProfile profile) {
        if (profile == null || PROFILE_NAME_METHOD == null) return null;
        try {
            Object value = PROFILE_NAME_METHOD.invoke(profile);
            return value instanceof String name && !name.isBlank() ? name : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Method findProfileNameMethod() {
        for (String name : List.of("name", "getName")) {
            try {
                return GameProfile.class.getMethod(name);
            } catch (NoSuchMethodException ignored) {
                // Authlib changed GameProfile from a class to a record.
            }
        }
        LOGGER.warn("[AutoLogin] Unable to read the connected account name from GameProfile.");
        return null;
    }

    public static void saveConfig() {
        config.transferExportKey = PasswordCrypto.generateTransferKey(config.servers);
        AutoConfig.getConfigHolder(AutoLoginConfig.class).save();
    }

    private static void importPendingTransferKey() {
        if (config.transferImportKey == null || config.transferImportKey.isBlank()) return;
        List<String> imported = PasswordCrypto.importTransferKey(config.transferImportKey.trim());
        if (imported != null) {
            for (String entry : imported) {
                int separator = entry.indexOf('=');
                if (separator <= 0) continue;
                String key = entry.substring(0, separator).trim();
                config.servers.removeIf(existing -> {
                    int equals = existing.indexOf('=');
                    return equals > 0 && existing.substring(0, equals).trim().equalsIgnoreCase(key);
                });
                config.servers.add(entry);
            }
            LOGGER.info("[AutoLogin] Imported pending transfer key from an older configuration.");
        } else {
            LOGGER.warn("[AutoLogin] Ignored invalid pending transfer key.");
        }
        config.transferImportKey = "";
        saveConfig();
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
            saveConfig();
            LOGGER.info("[AutoLogin] Migrated plaintext passwords to encoded format.");
        }
    }
}
