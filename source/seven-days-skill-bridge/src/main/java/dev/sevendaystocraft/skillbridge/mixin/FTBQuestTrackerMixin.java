package dev.sevendaystocraft.skillbridge.mixin;

import dev.ftb.mods.ftbquests.client.FTBQuestsClientEventHandler;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FTBQuestsClientEventHandler.class, remap = false)
public abstract class FTBQuestTrackerMixin {
    @Inject(method = "onScreenRender", at = @At("HEAD"), cancellable = true)
    private void sevenDays$replaceNativeTracker(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        ci.cancel();
    }
}
