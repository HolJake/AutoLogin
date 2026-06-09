package com.autologin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public class ServerEditScreen extends Screen {

    private static final int PANEL_W = 260;
    private static final int PANEL_H = 138;

    private final AutoLoginConfigScreen parent;
    private final String originalIp;
    private final String originalPass;

    private EditBox ipField;
    private EditBox passField;
    private boolean showPassword = false;

    public ServerEditScreen(AutoLoginConfigScreen parent, String existingIp, String existingPass) {
        super(Component.translatable(existingIp == null ? "autologin.server_edit.title_add" : "autologin.server_edit.title_edit"));
        this.parent = parent;
        this.originalIp = existingIp;
        this.originalPass = existingPass;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;
        int fieldX = cx - PANEL_W / 2 + 10;
        int fieldW = PANEL_W - 20;

        ipField = new EditBox(font, fieldX, cy - 28, fieldW, 18, Component.empty());
        ipField.setMaxLength(255);
        ipField.setHint(Component.translatable("autologin.server_edit.ip_placeholder"));
        if (originalIp != null) ipField.setValue(originalIp);
        addRenderableWidget(ipField);

        int passW = fieldW - 24;
        passField = new EditBox(font, fieldX, cy + 12, passW, 18, Component.empty());
        passField.setMaxLength(128);
        passField.setHint(Component.translatable("autologin.server_edit.pass_placeholder"));
        if (originalPass != null) passField.setValue(originalPass);
        applyMask();
        addRenderableWidget(passField);

        addRenderableWidget(Button.builder(Component.literal("👁"), btn -> {
            showPassword = !showPassword;
            applyMask();
        }).bounds(fieldX + passW + 2, cy + 12, 22, 18).build());

        addRenderableWidget(Button.builder(Component.translatable("autologin.server_edit.save"), btn -> trySave())
            .bounds(cx - 83, cy + 40, 78, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("autologin.server_edit.cancel"), btn ->
            minecraft.setScreen(parent)
        ).bounds(cx + 5, cy + 40, 78, 20).build());

        setInitialFocus(ipField);
    }

    private void applyMask() {
        passField.setFormatter((text, firstChar) -> {
            String visible = text.length() > firstChar ? text.substring(firstChar) : "";
            String display = showPassword ? visible : "●".repeat(visible.length());
            return FormattedCharSequence.forward(display, Style.EMPTY);
        });
    }

    private void trySave() {
        String ip = ipField.getValue().trim();
        String pass = passField.getValue();
        if (ip.isEmpty() || pass.isEmpty()) return;

        if (originalIp != null && !originalIp.equals(ip)) {
            parent.editedServers.remove(originalIp);
        }
        parent.editedServers.put(ip, pass);
        parent.refresh();
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x80000000);

        int cx = width / 2;
        int cy = height / 2;
        int px = cx - PANEL_W / 2;
        int py = cy - PANEL_H / 2;

        ctx.fill(px + 3, py + 3, px + PANEL_W + 3, py + PANEL_H + 3, 0x55000000);
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, 0xF0111111);
        ctx.fill(px, py, px + PANEL_W, py + 24, 0xFF1A2233);
        ctx.fill(px,              py,              px + PANEL_W,     py + 1,           0xFF445566);
        ctx.fill(px,              py + PANEL_H - 1, px + PANEL_W,   py + PANEL_H,      0xFF445566);
        ctx.fill(px,              py,              px + 1,           py + PANEL_H,      0xFF445566);
        ctx.fill(px + PANEL_W - 1, py,             px + PANEL_W,    py + PANEL_H,      0xFF445566);

        ctx.drawCenteredString(font, title, cx, py + 8, 0xFFFFFF);

        ctx.drawString(font, Component.translatable("autologin.server_edit.ip_label"),
            px + 10, cy - 40, 0xCCCCCC);
        ctx.drawString(font, Component.translatable("autologin.server_edit.pass_label"),
            px + 10, cy, 0xCCCCCC);

        if (ipField != null && passField != null) {
            boolean ipOk = !ipField.getValue().trim().isEmpty();
            boolean passOk = !passField.getValue().isEmpty();
            if (!ipOk || !passOk) {
                Component hint = !ipOk
                    ? Component.translatable("autologin.server_edit.error_ip")
                    : Component.translatable("autologin.server_edit.error_pass");
                ctx.drawCenteredString(font, hint, cx, py + PANEL_H + 4, 0xFF6666);
            }
        }

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { trySave(); return true; }
        if (keyCode == 256) { minecraft.setScreen(parent); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
