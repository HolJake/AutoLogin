package com.autologin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class ServerListWidget extends AbstractSelectionList<ServerListWidget.ServerEntry> {

    private final AutoLoginConfigScreen screen;

    public ServerListWidget(AutoLoginConfigScreen screen, Minecraft mc,
                            int width, int height, int top, int itemHeight) {
        super(mc, width, height, top, itemHeight);
        this.screen = screen;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {}

    public void refresh() {
        clearEntries();
        for (Map.Entry<String, String> e : screen.editedServers.entrySet()) {
            addEntry(new ServerEntry(e.getKey(), e.getValue()));
        }
    }

    public class ServerEntry extends AbstractSelectionList.Entry<ServerEntry> {

        private final String ip;
        private final String password;
        private final Button editBtn;
        private final Button deleteBtn;

        ServerEntry(String ip, String password) {
            this.ip = ip;
            this.password = password;
            this.editBtn = Button.builder(Component.translatable("autologin.server_list.edit"), b ->
                screen.openEditScreen(ip, password)
            ).bounds(0, 0, 48, 16).build();
            this.deleteBtn = Button.builder(Component.translatable("autologin.server_list.delete"), b ->
                screen.deleteServer(ip)
            ).bounds(0, 0, 18, 16).build();
        }

        @Override
        public void render(GuiGraphics ctx, int index, int y, boolean hovered, float delta) {
            int x = ServerListWidget.this.getRowLeft();
            int entryWidth = ServerListWidget.this.getRowWidth();

            Minecraft mc = Minecraft.getInstance();
            int mx = (int)(mc.mouseHandler.xpos() / mc.getWindow().getGuiScale());
            int my = (int)(mc.mouseHandler.ypos() / mc.getWindow().getGuiScale());

            if (hovered) {
                ctx.fill(x, y, x + entryWidth, y + 26, 0x1AFFFFFF);
            }
            if (index > 0) {
                ctx.fill(x + 4, y, x + entryWidth - 4, y + 1, 0x22FFFFFF);
            }

            ctx.drawString(minecraft.font, ip, x + 8, y + 7, 0xFFFFFF);

            String masked = "●".repeat(Math.min(password.length(), 14));
            int maskedX = x + entryWidth - 82 - minecraft.font.width(masked);
            ctx.drawString(minecraft.font, masked, maskedX, y + 7, 0x666666);

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
