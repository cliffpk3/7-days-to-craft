package dev.sevendaystocraft.locks.mixin;

import dev.sevendaystocraft.locks.CompassIntegration;
import dlovin.advancedcompass.utils.GroupUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Enforce each sensed entity's own range after Advanced Compass groups types. */
@Mixin(value = GroupUtils.class, remap = false)
public abstract class CompassSenseRangeMixin {
    @Inject(method = "hasLineOfSight", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sevenDays$limitSensedMobs(Entity entity, Vec3 viewer, boolean lineOfSight,
                                                   int maximumRange, CallbackInfoReturnable<Boolean> result) {
        if (!CompassIntegration.Client.visibleMob(entity, viewer)) result.setReturnValue(false);
    }
}
