package com.autologin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;

public class AutoLoginToast implements Toast {

    private static final int   WIDTH      = 220;
    private static final int   HEIGHT     = 44;
    private static final long  DISPLAY_MS = 4000L;

    private static final int BG     = 0xF0111111;
    private static final int ACCENT = 0xFF44BB44;
    private static final int CTITLE = 0xFF88FF88;
    private static final int CMSG   = 0xFFDDDDDD;

    private final Component message;

    private AutoLoginToast(Component message) {
        this.message = message;
    }

    /** Call from any thread — schedules the toast on the main thread. */
    public static void show(Component message) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> mc.getToasts().addToast(new AutoLoginToast(message)));
    }

    @Override
    public Visibility render(GuiGraphics ctx, ToastComponent toasts, long timeSinceLastVisible) {
        ctx.fill(0, 0, WIDTH, HEIGHT, BG);
        ctx.fill(0, 0, 3,     HEIGHT, ACCENT);

        var font = toasts.getMinecraft().font;
        ctx.drawString(font, Component.translatable("autologin.toast.title"), 8,  8, CTITLE, false);
        ctx.drawString(font, message,                                          8, 24, CMSG,   false);

        return timeSinceLastVisible >= DISPLAY_MS ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override public int width()  { return WIDTH;  }
    @Override public int height() { return HEIGHT; }
}
