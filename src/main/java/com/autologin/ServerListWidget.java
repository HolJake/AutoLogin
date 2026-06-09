package com.autologin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Text;

import java.util.Map;

public class ServerListWidget extends EntryListWidget<ServerListWidget.ServerEntry> {

    private final AutoLoginConfigScreen screen;

    public ServerListWidget(AutoLoginConfigScreen screen, MinecraftClient client,
                            int width, int height, int top, int itemHeight) {
        super(client, width, height, top, itemHeight);
        this.screen = screen;
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder) {}

    public void refresh() {
        clearEntries();
        for (Map.Entry<String, String> e : screen.editedServers.entrySet()) {
            addEntry(new ServerEntry(e.getKey(), e.getValue()));
        }
    }

    public class ServerEntry extends EntryListWidget.Entry<ServerEntry> {

        private final String ip;
        private final String password;
        private final ButtonWidget editBtn;
        private final ButtonWidget deleteBtn;

        ServerEntry(String ip, String password) {
            this.ip = ip;
            this.password = password;
            this.editBtn = ButtonWidget.builder(Text.translatable("autologin.server_list.edit"), b ->
                screen.openEditScreen(ip, password)
            ).dimensions(0, 0, 48, 16).build();
            this.deleteBtn = ButtonWidget.builder(Text.translatable("autologin.server_list.delete"), b ->
                screen.deleteServer(ip)
            ).dimensions(0, 0, 18, 16).build();
        }

        @Override
        public void render(DrawContext ctx, int index, int y, boolean hovered, float delta) {
            int x = ServerListWidget.this.getRowLeft();
            int entryWidth = ServerListWidget.this.getRowWidth();

            MinecraftClient mc = MinecraftClient.getInstance();
            int mx = (int)(mc.mouse.getX() / mc.getWindow().getScaleFactor());
            int my = (int)(mc.mouse.getY() / mc.getWindow().getScaleFactor());

            if (hovered) {
                ctx.fill(x, y, x + entryWidth, y + 26, 0x1AFFFFFF);
            }
            if (index > 0) {
                ctx.fill(x + 4, y, x + entryWidth - 4, y + 1, 0x22FFFFFF);
            }

            ctx.drawTextWithShadow(client.textRenderer, ip, x + 8, y + 7, 0xFFFFFF);

            String masked = "●".repeat(Math.min(password.length(), 14));
            int maskedX = x + entryWidth - 82 - client.textRenderer.getWidth(masked);
            ctx.drawTextWithShadow(client.textRenderer, masked, maskedX, y + 7, 0x666666);

            editBtn.setX(x + entryWidth - 74);
            editBtn.setY(y + 5);
            editBtn.render(ctx, mx, my, delta);

            deleteBtn.setX(x + entryWidth - 23);
            deleteBtn.setY(y + 5);
            deleteBtn.render(ctx, mx, my, delta);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            return editBtn.mouseClicked(mx, my, btn) || deleteBtn.mouseClicked(mx, my, btn);
        }
    }
}
