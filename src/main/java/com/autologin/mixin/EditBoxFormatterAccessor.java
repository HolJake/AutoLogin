package com.autologin.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Function;

@Mixin(EditBox.class)
public interface EditBoxFormatterAccessor {
    @Accessor("formatter")
    void autologin$setFormatter(Function<String, FormattedCharSequence> formatter);
}
