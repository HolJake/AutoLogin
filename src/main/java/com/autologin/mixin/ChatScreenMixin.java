package com.autologin.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    private static final String[] AUTH_CMDS = {"/login ", "/l ", "/register ", "/reg "};

    @Shadow private EditBox input;

    @Inject(method = "init", at = @At("TAIL"))
    private void autologin$maskPassword(CallbackInfo ci) {
        this.input.setFormatter((text, cursorPos) -> {
            String lower = text.toLowerCase();
            int passStart = -1;
            for (String cmd : AUTH_CMDS) {
                if (lower.startsWith(cmd)) {
                    passStart = cmd.length();
                    break;
                }
            }
            if (passStart < 0 || passStart >= text.length()) {
                return Language.getInstance().getVisualOrder(Component.literal(text));
            }
            String cmdPart = text.substring(0, passStart);
            String stars   = "*".repeat(text.length() - passStart);
            return Language.getInstance().getVisualOrder(
                Component.literal(cmdPart)
                    .append(Component.literal(stars)
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x999999))))
            );
        });
    }
}
