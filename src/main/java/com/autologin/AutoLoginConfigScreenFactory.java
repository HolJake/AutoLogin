package com.autologin;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AutoLoginConfigScreenFactory implements ModMenuApi {

    private static final List<String> DEFAULT_TRIGGERS = Arrays.asList(
            "/login", "/l ", "please login", "please log in",
            "войдите", "авторизуйтесь", "введите пароль",
            "you need to login", "you must login", "use /login"
    );

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return this::buildScreen;
    }

    private Screen buildScreen(Screen parent) {
        AutoLoginConfig config = AutoLoginMod.getConfig();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("AutoLogin — Settings"))
                .setSavingRunnable(config::save);

        ConfigEntryBuilder eb = builder.entryBuilder();

        // ── Servers ─────────────────────────────────────────────────────────
        ConfigCategory servers = builder.getOrCreateCategory(Text.literal("Servers"));

        List<String> serverList = config.servers.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.toList());

        servers.addEntry(eb.startStrList(Text.literal("Server passwords"), serverList)
                .setDefaultValue(Collections.emptyList())
                .setTooltip(
                        Text.literal("One entry per line, format:  serverip=password"),
                        Text.literal("Examples:"),
                        Text.literal("  play.example.com=mypassword123"),
                        Text.literal("  192.168.1.10:25565=secret"))
                .setSaveConsumer(list -> {
                    config.servers.clear();
                    for (String entry : list) {
                        int sep = entry.indexOf('=');
                        if (sep > 0) {
                            String ip = entry.substring(0, sep).trim();
                            String pass = entry.substring(sep + 1); // keep password as-is
                            if (!ip.isEmpty() && !pass.isEmpty()) {
                                config.servers.put(ip, pass);
                            }
                        }
                    }
                })
                .build());

        // ── Behavior ────────────────────────────────────────────────────────
        ConfigCategory behavior = builder.getOrCreateCategory(Text.literal("Behavior"));

        behavior.addEntry(eb.startLongField(Text.literal("Login delay (ms)"), config.loginDelayMs)
                .setDefaultValue(300L)
                .setMin(0L)
                .setMax(10000L)
                .setTooltip(Text.literal("How long to wait (milliseconds) after detecting"),
                        Text.literal("the login prompt before sending the command."))
                .setSaveConsumer(val -> config.loginDelayMs = val)
                .build());

        // ── Trigger words ────────────────────────────────────────────────────
        ConfigCategory triggers = builder.getOrCreateCategory(Text.literal("Trigger Words"));

        triggers.addEntry(eb.startStrList(Text.literal("Trigger words / phrases"), config.triggerWords)
                .setDefaultValue(DEFAULT_TRIGGERS)
                .setTooltip(
                        Text.literal("If ANY of these strings appear inside an incoming"),
                        Text.literal("chat/system message, the login command is sent."),
                        Text.literal("Case-insensitive matching is used."))
                .setSaveConsumer(list -> config.triggerWords = list)
                .build());

        return builder.build();
    }
}
