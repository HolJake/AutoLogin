package com.autologin.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    private static final String[] AUTH_CMDS = {"/login ", "/l ", "/register ", "/reg "};

    @Shadow private EditBox input;

    @Inject(method = "render", at = @At("TAIL"))
    private void autologin$renderPasswordMask(GuiGraphics graphics, int mouseX, int mouseY,
                                              float partialTick, CallbackInfo ci) {
        String text = this.input.getValue();
        String lower = text.toLowerCase();
        int passStart = -1;
        for (String cmd : AUTH_CMDS) {
            if (lower.startsWith(cmd)) {
                passStart = cmd.length();
                break;
            }
        }
        if (passStart < 0 || passStart >= text.length()) return;

        Minecraft mc = Minecraft.getInstance();
        String cmdPart  = text.substring(0, passStart);
        String passPart = text.substring(passStart);
        String stars    = "*".repeat(passPart.length());

        // EditBox renders text at x+4 (inner padding), vertically centred for 8-px font
        int textX    = this.input.getX() + 4;
        int textY    = this.input.getY() + (this.input.getHeight() - 8) / 2;
        int cmdWidth = mc.font.width(cmdPart);
        int pwWidth  = mc.font.width(passPart);

        // Cover the real password glyphs with the EditBox black background
        graphics.fill(textX + cmdWidth,     textY - 1,
                      textX + cmdWidth + pwWidth + 1, textY + 9,
                      0xFF000000);

        // Draw grey stars in their place
        graphics.drawString(mc.font, stars, textX + cmdWidth, textY, 0xFF999999, false);
    }
}
