package com.autologin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.LinkedHashMap;
import java.util.Map;

public class AutoLoginConfigScreen extends Screen {

    static final int HEADER_H = 33;
    static final int FOOTER_H = 52;

    private final Screen parent;
    private final AutoLoginConfig config;
    final Map<String, String> editedServers;

    private ServerListWidget serverList;
    private TextFieldWidget delayField;

    public AutoLoginConfigScreen(Screen parent) {
        super(Text.translatable("autologin.config.title"));
        this.parent = parent;
        this.config = AutoLoginMod.getConfig();
        this.editedServers = new LinkedHashMap<>(config.servers);
    }

    @Override
    protected void init() {
        int listH = height - HEADER_H - FOOTER_H;

        serverList = new ServerListWidget(this, client, width, listH, HEADER_H, 26);
        serverList.refresh();
        addDrawableChild(serverList);

        // ── Footer row 1: Add Server ──────────────────────────────────────────
        int y1 = height - FOOTER_H + 4;
        addDrawableChild(ButtonWidget.builder(Text.translatable("autologin.config.add_server"), btn ->
            client.setScreen(new ServerEditScreen(this, null, null))
        ).dimensions(width / 2 - 80, y1, 160, 20).build());

        // ── Footer row 2: Trigger Words | Delay | Save | Cancel ───────────────
        int y2 = height - FOOTER_H + 28;

        addDrawableChild(ButtonWidget.builder(Text.translatable("autologin.config.trigger_words"), btn ->
            client.setScreen(new TriggerWordsScreen(this, config))
        ).dimensions(width / 2 - 156, y2, 110, 20).build());

        delayField = new TextFieldWidget(textRenderer, width / 2 - 27, y2 + 2, 42, 16, Text.empty());
        delayField.setText(String.valueOf(config.loginDelayMs));
        delayField.setTextPredicate(s -> s.isEmpty() || s.matches("\\d{0,5}"));
        addDrawableChild(delayField);

        addDrawableChild(ButtonWidget.builder(Text.translatable("autologin.config.save"), btn -> {
            applyAndSave();
            client.setScreen(parent);
        }).dimensions(width / 2 + 22, y2, 58, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("autologin.config.cancel"), btn ->
            client.setScreen(parent)
        ).dimensions(width / 2 + 84, y2, 70, 20).build());
    }

    void refresh() {
        if (serverList != null) serverList.refresh();
    }

    void openEditScreen(String ip, String password) {
        client.setScreen(new ServerEditScreen(this, ip, password));
    }

    void deleteServer(String ip) {
        editedServers.remove(ip);
        refresh();
    }

    void applyAndSave() {
        config.servers.clear();
        config.servers.putAll(editedServers);
        try {
            if (!delayField.getText().isEmpty())
                config.loginDelayMs = Long.parseLong(delayField.getText());
        } catch (NumberFormatException ignored) {}
        config.save();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);
        super.render(ctx, mx, my, delta);

        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 11, 0xFFFFFF);

        // Dividers
        ctx.fill(0, HEADER_H - 2, width, HEADER_H - 1, 0x55FFFFFF);
        ctx.fill(0, height - FOOTER_H, width, height - FOOTER_H + 1, 0x55FFFFFF);

        // Delay label (inline with field on footer row 2)
        int y2 = height - FOOTER_H + 28;
        ctx.drawTextWithShadow(textRenderer, Text.translatable("autologin.config.delay_label"), width / 2 - 82, y2 + 5, 0xAAAAAA);
        ctx.drawTextWithShadow(textRenderer, Text.translatable("autologin.config.delay_unit"), width / 2 + 18, y2 + 5, 0x888888);

        // Empty state
        if (editedServers.isEmpty()) {
            int midY = HEADER_H + (height - HEADER_H - FOOTER_H) / 2;
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("autologin.config.no_servers"), width / 2, midY - 8, 0x888888);
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("autologin.config.no_servers_hint"), width / 2, midY + 6, 0x666666);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
