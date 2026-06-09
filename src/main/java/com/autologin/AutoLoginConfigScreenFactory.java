package com.autologin;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutoLoginConfigScreenFactory implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            AutoLoginConfig config = AutoLoginMod.getConfig();

            ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("autologin.config.title"));

            ConfigEntryBuilder eb = builder.entryBuilder();
            ConfigCategory cat = builder.getOrCreateCategory(Component.translatable("autologin.config.category"));

            // Login delay (ms)
            cat.addEntry(eb.startLongField(
                    Component.translatable("autologin.config.delay"), config.loginDelayMs)
                .setDefaultValue(1000L)
                .setMin(0L)
                .setSaveConsumer(v -> config.loginDelayMs = v)
                .build());

            // Servers — one entry per line in "ip=password" format
            List<String> serverLines = new ArrayList<>();
            for (Map.Entry<String, String> e : config.servers.entrySet()) {
                serverLines.add(e.getKey() + "=" + e.getValue());
            }
            cat.addEntry(eb.startStringList(
                    Component.translatable("autologin.config.servers"), serverLines)
                .setDefaultValue(List.of())
                .setSaveConsumer(list -> {
                    config.servers.clear();
                    for (String line : list) {
                        int sep = line.indexOf('=');
                        if (sep > 0) {
                            String ip = line.substring(0, sep).trim();
                            String pass = line.substring(sep + 1);
                            if (!ip.isEmpty()) config.servers.put(ip, pass);
                        }
                    }
                })
                .build());

            // Trigger words
            cat.addEntry(eb.startStringList(
                    Component.translatable("autologin.triggers.title"),
                    new ArrayList<>(config.triggerWords))
                .setDefaultValue(List.of("Please", "/login"))
                .setSaveConsumer(list -> config.triggerWords = new ArrayList<>(list))
                .build());

            builder.setSavingRunnable(config::save);

            return builder.build();
        };
    }
}
