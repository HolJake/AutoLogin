package com.autologin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class ServerEditScreen extends Screen {

    private static final int PANEL_W = 260;
    private static final int PANEL_H = 138;

    private final AutoLoginConfigScreen parent;
    private final String originalIp;
    private final String originalPass;

    private TextFieldWidget ipField;
    private TextFieldWidget passField;
    private boolean showPassword = false;

    /**
     * @param existingIp   null when adding a new server, non-null when editing
     * @param existingPass null when adding a new server
     */
    public ServerEditScreen(AutoLoginConfigScreen parent, String existingIp, String existingPass) {
        super(Text.translatable(existingIp == null ? "autologin.server_edit.title_add" : "autologin.server_edit.title_edit"));
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

        // IP field
        ipField = new TextFieldWidget(textRenderer, fieldX, cy - 28, fieldW, 18, Text.empty());
        ipField.setMaxLength(255);
        ipField.setPlaceholder(Text.translatable("autologin.server_edit.ip_placeholder"));
        if (originalIp != null) ipField.setText(originalIp);
        addDrawableChild(ipField);

        // Password field (narrower — leaves room for the toggle button)
        int passW = fieldW - 24;
        passField = new TextFieldWidget(textRenderer, fieldX, cy + 12, passW, 18, Text.empty());
        passField.setMaxLength(128);
        passField.setPlaceholder(Text.translatable("autologin.server_edit.pass_placeholder"));
        if (originalPass != null) passField.setText(originalPass);
        applyMask();
        addDrawableChild(passField);

        // Show / hide password
        addDrawableChild(ButtonWidget.builder(Text.literal("👁"), btn -> {
            showPassword = !showPassword;
            applyMask();
        }).dimensions(fieldX + passW + 2, cy + 12, 22, 18).build());

        // Save / Cancel
        addDrawableChild(ButtonWidget.builder(Text.translatable("autologin.server_edit.save"), btn -> trySave())
            .dimensions(cx - 83, cy + 40, 78, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("autologin.server_edit.cancel"), btn ->
            client.setScreen(parent)
        ).dimensions(cx + 5, cy + 40, 78, 20).build());

        setFocused(ipField);
    }

    private void applyMask() {
        passField.setRenderTextProvider((text, firstChar) -> {
            String visible = text.length() > firstChar ? text.substring(firstChar) : "";
            String display = showPassword ? visible : "●".repeat(visible.length());
            return Text.literal(display).asOrderedText();
        });
    }

    private void trySave() {
        String ip = ipField.getText().trim();
        String pass = passField.getText();
        if (ip.isEmpty() || pass.isEmpty()) return;

        // When editing, remove old key if IP changed
        if (originalIp != null && !originalIp.equals(ip)) {
            parent.editedServers.remove(originalIp);
        }
        parent.editedServers.put(ip, pass);
        parent.refresh();
        client.setScreen(parent);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx, mx, my, delta);

        int cx = width / 2;
        int cy = height / 2;
        int px = cx - PANEL_W / 2;
        int py = cy - PANEL_H / 2;

        // Panel shadow
        ctx.fill(px + 3, py + 3, px + PANEL_W + 3, py + PANEL_H + 3, 0x55000000);
        // Panel background
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, 0xF0111111);
        // Title bar
        ctx.fill(px, py, px + PANEL_W, py + 24, 0xFF1A2233);
        // Border
        ctx.fill(px,              py,              px + PANEL_W,     py + 1,           0xFF445566);
        ctx.fill(px,              py + PANEL_H - 1, px + PANEL_W,   py + PANEL_H,      0xFF445566);
        ctx.fill(px,              py,              px + 1,           py + PANEL_H,      0xFF445566);
        ctx.fill(px + PANEL_W - 1, py,             px + PANEL_W,    py + PANEL_H,      0xFF445566);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer, title, cx, py + 8, 0xFFFFFF);

        // Labels
        ctx.drawTextWithShadow(textRenderer, Text.translatable("autologin.server_edit.ip_label"),
            px + 10, cy - 40, 0xCCCCCC);
        ctx.drawTextWithShadow(textRenderer, Text.translatable("autologin.server_edit.pass_label"),
            px + 10, cy, 0xCCCCCC);

        // Validation hint — show only when both fields have been touched
        if (ipField != null && passField != null) {
            boolean ipOk = !ipField.getText().trim().isEmpty();
            boolean passOk = !passField.getText().isEmpty();
            if (!ipOk || !passOk) {
                Text hint = !ipOk
                    ? Text.translatable("autologin.server_edit.error_ip")
                    : Text.translatable("autologin.server_edit.error_pass");
                ctx.drawCenteredTextWithShadow(textRenderer, hint, cx, py + PANEL_H + 4, 0xFF6666);
            }
        }

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) { trySave(); return true; }  // Enter
        if (keyCode == 256) { client.setScreen(parent); return true; }      // Escape
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
