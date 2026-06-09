package com.autologin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Map;

public class AutoLoginConfigScreen extends Screen {

    static final int HEADER_H = 33;
    static final int FOOTER_H = 52;

    private final Screen parent;
    private final AutoLoginConfig config;
    final Map<String, String> editedServers;

    private ServerListWidget serverList;
    private EditBox delayField;

    public AutoLoginConfigScreen(Screen parent) {
        super(Component.translatable("autologin.config.title"));
        this.parent = parent;
        this.config = AutoLoginMod.getConfig();
        this.editedServers = new LinkedHashMap<>(config.servers);
    }

    @Override
    protected void init() {
        int listH = height - HEADER_H - FOOTER_H;

        serverList = new ServerListWidget(this, minecraft, width, listH, HEADER_H, 26);
        serverList.refresh();
        addRenderableWidget(serverList);

        int y1 = height - FOOTER_H + 4;
        addRenderableWidget(Button.builder(Component.translatable("autologin.config.add_server"), btn ->
            minecraft.setScreen(new ServerEditScreen(this, null, null))
        ).bounds(width / 2 - 80, y1, 160, 20).build());

        int y2 = height - FOOTER_H + 28;

        addRenderableWidget(Button.builder(Component.translatable("autologin.config.trigger_words"), btn ->
            minecraft.setScreen(new TriggerWordsScreen(this, config))
        ).bounds(width / 2 - 156, y2, 110, 20).build());

        delayField = new EditBox(font, width / 2 - 27, y2 + 2, 42, 16, Component.empty());
        delayField.setValue(String.valueOf(config.loginDelayMs));
        delayField.setFilter(s -> s.isEmpty() || s.matches("\\d{0,5}"));
        addRenderableWidget(delayField);

        addRenderableWidget(Button.builder(Component.translatable("autologin.config.save"), btn -> {
            applyAndSave();
            minecraft.setScreen(parent);
        }).bounds(width / 2 + 22, y2, 58, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("autologin.config.cancel"), btn ->
            minecraft.setScreen(parent)
        ).bounds(width / 2 + 84, y2, 70, 20).build());
    }

    void refresh() {
        if (serverList != null) serverList.refresh();
    }

    void openEditScreen(String ip, String password) {
        minecraft.setScreen(new ServerEditScreen(this, ip, password));
    }

    void deleteServer(String ip) {
        editedServers.remove(ip);
        refresh();
    }

    void applyAndSave() {
        config.servers.clear();
        config.servers.putAll(editedServers);
        try {
            if (!delayField.getValue().isEmpty())
                config.loginDelayMs = Long.parseLong(delayField.getValue());
        } catch (NumberFormatException ignored) {}
        config.save();
    }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xB0000000);
        super.render(ctx, mx, my, delta);

        ctx.drawCenteredString(font, title, width / 2, 11, 0xFFFFFF);

        ctx.fill(0, HEADER_H - 2, width, HEADER_H - 1, 0x55FFFFFF);
        ctx.fill(0, height - FOOTER_H, width, height - FOOTER_H + 1, 0x55FFFFFF);

        int y2 = height - FOOTER_H + 28;
        ctx.drawString(font, Component.translatable("autologin.config.delay_label"), width / 2 - 82, y2 + 5, 0xAAAAAA);
        ctx.drawString(font, Component.translatable("autologin.config.delay_unit"), width / 2 + 18, y2 + 5, 0x888888);

        if (editedServers.isEmpty()) {
            int midY = HEADER_H + (height - HEADER_H - FOOTER_H) / 2;
            ctx.drawCenteredString(font,
                Component.translatable("autologin.config.no_servers"), width / 2, midY - 8, 0x888888);
            ctx.drawCenteredString(font,
                Component.translatable("autologin.config.no_servers_hint"), width / 2, midY + 6, 0x666666);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
