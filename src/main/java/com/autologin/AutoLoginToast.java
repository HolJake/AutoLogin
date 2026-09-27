package com.autologin;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Small, monochrome HUD notifications that fit the current GUI scale. */
public final class AutoLoginToast {
    private static final long DISPLAY_MS = 5200L;
    private static final long ENTER_MS = 280L;
    private static final long EXIT_MS = 360L;
    private static final int MAX_WIDTH = 280;
    private static final int HEIGHT = 56;
    private static final int MARGIN = 8;
    private static final int GAP = 6;
    private static final List<Notification> queue = new ArrayList<>();

    private AutoLoginToast() {}

    public static void init() {
        HudRenderCallback.EVENT.register(AutoLoginToast::renderHud);
    }

    public static void show(Component message) {
        Minecraft.getInstance().execute(() -> {
            if (queue.size() == 6) queue.remove(0);
            queue.add(new Notification(message));
        });
    }

    private static void renderHud(GuiGraphics gui, DeltaTracker delta) {
        queue.removeIf(Notification::expired);
        if (queue.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        AutoLoginConfig config = AutoLoginMod.getConfig();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int width = Math.min(MAX_WIDTH, screenW - MARGIN * 2);
        if (width < 60 || screenH < HEIGHT + MARGIN * 2) return;

        boolean right = config.notificationCorner == AutoLoginConfig.Corner.TOP_RIGHT
                || config.notificationCorner == AutoLoginConfig.Corner.BOTTOM_RIGHT;
        boolean bottom = config.notificationCorner == AutoLoginConfig.Corner.BOTTOM_RIGHT
                || config.notificationCorner == AutoLoginConfig.Corner.BOTTOM_LEFT;
        int baseX = right ? screenW - width - MARGIN : MARGIN;
        int baseY = bottom ? screenH - HEIGHT - MARGIN : MARGIN;
        int visible = Math.max(1, (screenH - MARGIN * 2 + GAP) / (HEIGHT + GAP));
        int first = Math.max(0, queue.size() - visible);

        for (int index = queue.size() - 1, slot = 0; index >= first; index--, slot++) {
            int y = baseY + (bottom ? -slot : slot) * (HEIGHT + GAP);
            drawNotification(gui, mc.font, queue.get(index), baseX, y, width, right);
        }
    }

    private static void drawNotification(GuiGraphics gui, Font font, Notification notice,
                                         int baseX, int y, int width, boolean right) {
        long age = notice.age();
        float entrance = easeOut(Math.min(1f, age / (float) ENTER_MS));
        float exit = age > DISPLAY_MS - EXIT_MS
                ? 1f - easeIn(Math.min(1f, (age - DISPLAY_MS + EXIT_MS) / (float) EXIT_MS)) : 1f;
        float opacity = Math.max(0f, entrance * exit);
        if (opacity < 0.04f) return;
        int offset = Math.round((1f - entrance) * 22f) * (right ? 1 : -1);
        int x = baseX + offset;

        gui.fill(x, y, x + width, y + HEIGHT, color(0x141414, Math.round(opacity * 194)));
        gui.fill(x, y, x + width, y + 1, color(0xFFFFFF, Math.round(opacity * 62)));
        gui.fill(x, y + HEIGHT - 1, x + width, y + HEIGHT, color(0xFFFFFF, Math.round(opacity * 42)));
        gui.fill(x + 10, y + 11, x + 12, y + 25, color(0xFFFFFF, Math.round(opacity * 205)));

        LegacyTextRenderer.draw(gui, font, Component.translatable("autologin.toast.title"),
                x + 19, y + 12, color(0xFFFFFF, Math.round(opacity * 255)), false);
        String message = notice.message.getString();
        int lineWidth = width - 24;
        String first = font.plainSubstrByWidth(message, lineWidth);
        String remaining = message.substring(first.length()).stripLeading();
        String second = font.plainSubstrByWidth(remaining, lineWidth);
        if (second.length() < remaining.length() && !second.isEmpty()) {
            second = font.plainSubstrByWidth(second, lineWidth - font.width("…")) + "…";
        }
        LegacyTextRenderer.draw(gui, font, Component.literal(first),
                x + 12, y + 30, color(0xF0F0F0, Math.round(opacity * 245)), false);
        if (!second.isEmpty()) LegacyTextRenderer.draw(gui, font, Component.literal(second),
                x + 12, y + 42, color(0xD0D0D0, Math.round(opacity * 220)), false);
    }

    private static float easeOut(float progress) {
        return 1f - (float) Math.pow(1f - progress, 3);
    }

    private static float easeIn(float progress) {
        return progress * progress * progress;
    }

    private static int color(int rgb, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | rgb;
    }

    private static final class Notification {
        private final Component message;
        private final long start = System.nanoTime();

        private Notification(Component message) { this.message = message; }
        private long age() { return (System.nanoTime() - start) / 1_000_000L; }
        private boolean expired() { return age() >= DISPLAY_MS; }
    }
}
