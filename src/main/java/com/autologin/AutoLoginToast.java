package com.autologin;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class AutoLoginToast {

    private static final long DISPLAY_MS = 4000L;
    private static final long FADE_MS    = 400L;
    private static final int  WIDTH      = 220;
    private static final int  HEIGHT     = 44;

    private static final List<Notification> queue = new ArrayList<>();

    public static void init() {
        HudRenderCallback.EVENT.register(AutoLoginToast::renderHud);
    }

    /** Thread-safe: schedules display on the main thread. */
    public static void show(Component message) {
        Minecraft.getInstance().execute(() -> queue.add(new Notification(message)));
    }

    private static void renderHud(GuiGraphics ctx, DeltaTracker delta) {
        queue.removeIf(Notification::expired);
        if (queue.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int y = 4;

        for (Notification n : queue) {
            int x = screenW - WIDTH - 4;
            float alpha = n.alpha();
            int a = (int)(alpha * 255);

            // Background + left accent stripe + top line
            ctx.fill(x,     y, x + WIDTH, y + HEIGHT, argb(0x11, 0x11, 0x11, (int)(alpha * 0xF0)));
            ctx.fill(x,     y, x + 3,     y + HEIGHT, argb(0x44, 0xBB, 0x44, a));
            ctx.fill(x + 3, y, x + WIDTH, y + 1,      argb(0x44, 0xBB, 0x44, a / 3));

            ctx.drawString(mc.font, Component.translatable("autologin.toast.title"),
                x + 8, y + 8,  argb(0x88, 0xFF, 0x88, a), false);
            ctx.drawString(mc.font, n.message,
                x + 8, y + 24, argb(0xDD, 0xDD, 0xDD, a), false);

            y += HEIGHT + 2;
        }
    }

    private static int argb(int r, int g, int b, int a) {
        return (Mth.clamp(a, 0, 255) << 24) | (r << 16) | (g << 8) | b;
    }

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
