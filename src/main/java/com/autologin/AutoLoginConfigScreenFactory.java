package com.autologin;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

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

            // Servers — "ip=password; ip2=password2" format
            String serverStr = config.servers.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("; "));
            cat.addEntry(eb.startTextField(
                    Component.translatable("autologin.config.servers"), serverStr)
                .setDefaultValue("")
                .setSaveConsumer(s -> {
                    config.servers.clear();
                    for (String part : s.split(";")) {
                        part = part.trim();
                        int sep = part.indexOf('=');
                        if (sep > 0) {
                            String ip = part.substring(0, sep).trim();
                            String pass = part.substring(sep + 1);
                            if (!ip.isEmpty()) config.servers.put(ip, pass);
                        }
                    }
                })
                .build());

            // Trigger words — comma-separated
            String triggerStr = String.join(", ", config.triggerWords);
            cat.addEntry(eb.startTextField(
                    Component.translatable("autologin.triggers.title"), triggerStr)
                .setDefaultValue("Please, /login")
                .setSaveConsumer(s -> config.triggerWords = Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(t -> !t.isEmpty())
                    .collect(Collectors.toCollection(ArrayList::new)))
                .build());

            builder.setSavingRunnable(config::save);

            return builder.build();
        };
    }
}
