package dev.sevendaystocraft.skillbridge.mixin;

import dev.sevendaystocraft.skillbridge.SkillBridge;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes Aggro Indicator part of the Threat Sense milestone. */
@Pseudo
@Mixin(targets = "com.github.raverbury.aggroindicator.event.ClientEventHandler", remap = false)
public abstract class AggroIndicatorMilestoneMixin {
    @Inject(method = "shouldDrawAlert", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sevenDays$requireThreatSense(LivingEntity entity, CallbackInfoReturnable<Boolean> callback) {
        if (!SkillBridge.ClientOverlay.hasThreatSense()) {
            callback.setReturnValue(false);
        }
    }
}
