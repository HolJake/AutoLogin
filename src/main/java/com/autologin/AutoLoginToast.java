package com.autologin;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AutoLoginToast {

    private static final long DISPLAY_MS = 7000L;  // 7 секунд
    private static final long FADE_MS    = 500L;
    private static final int  WIDTH      = 230;
    private static final int  HEIGHT     = 44;
    private static final int  MARGIN     = 4;

    private static final List<Notification> queue = new ArrayList<>();

    public static void init() {
        // Register at LAST position — renders on top of vanilla HUD elements
        HudRenderCallback.EVENT.register(AutoLoginToast::renderHud);
    }

    /** Thread-safe: schedules display on the main thread. */
    public static void show(Component message) {
        Minecraft.getInstance().execute(() -> queue.add(new Notification(message)));
    }

    private static void renderHud(GuiGraphics ctx, DeltaTracker delta) {
        queue.removeIf(Notification::expired);
        if (queue.isEmpty()) return;

        Minecraft mc   = Minecraft.getInstance();
        AutoLoginConfig cfg = AutoLoginMod.getConfig();

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        boolean onRight  = cfg.notificationCorner == AutoLoginConfig.Corner.TOP_RIGHT
                        || cfg.notificationCorner == AutoLoginConfig.Corner.BOTTOM_RIGHT;
        boolean onBottom = cfg.notificationCorner == AutoLoginConfig.Corner.BOTTOM_RIGHT
                        || cfg.notificationCorner == AutoLoginConfig.Corner.BOTTOM_LEFT;

        int x  = onRight  ? screenW - WIDTH - MARGIN : MARGIN;
        int y0 = onBottom ? screenH - HEIGHT - MARGIN : MARGIN;
        int dy = onBottom ? -(HEIGHT + 2) : (HEIGHT + 2);

        // Bottom corners: stack upward, so reverse list to keep newest at edge
        List<Notification> ordered = onBottom ? reversed(queue) : queue;

        int y = y0;
        for (Notification n : ordered) {
            drawNotification(ctx, mc, n, x, y);
            y += dy;
        }
    }

    private static void drawNotification(GuiGraphics ctx, Minecraft mc,
                                         Notification n, int x, int y) {
        float alpha = n.alpha();
        int a  = (int)(alpha * 255);
        int aB = (int)(alpha * 0xF0);  // slightly more opaque background

        // Background
        ctx.fill(x,     y, x + WIDTH, y + HEIGHT, argb(0x0E, 0x0E, 0x0E, aB));
        // Left accent stripe
        ctx.fill(x,     y, x + 3,     y + HEIGHT, argb(0x3D, 0xC2, 0x3D, a));
        // Thin top line (same green, dimmer)
        ctx.fill(x + 3, y, x + WIDTH, y + 1,      argb(0x3D, 0xC2, 0x3D, a / 3));

        ctx.drawString(mc.font, Component.translatable("autologin.toast.title"),
            x + 8, y + 8,  argb(0x7F, 0xFF, 0x7F, a), false);
        ctx.drawString(mc.font, n.message,
            x + 8, y + 24, argb(0xE0, 0xE0, 0xE0, a), false);
    }

    private static List<Notification> reversed(List<Notification> src) {
        List<Notification> copy = new ArrayList<>(src);
        Collections.reverse(copy);
        return copy;
    }

    private static int argb(int r, int g, int b, int a) {
        return (Mth.clamp(a, 0, 255) << 24) | (r << 16) | (g << 8) | b;
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private static class Notification {
        final Component message;
        final long shownAt = System.currentTimeMillis();

        Notification(Component message) { this.message = message; }

        long age()        { return System.currentTimeMillis() - shownAt; }
        boolean expired() { return age() >= DISPLAY_MS; }

        float alpha() {
            long age = age();
            if (age < FADE_MS) return (float) age / FADE_MS;
            long remaining = DISPLAY_MS - age;
            if (remaining < FADE_MS) return Math.max(0f, (float) remaining / FADE_MS);
            return 1f;
        }
    }
}
