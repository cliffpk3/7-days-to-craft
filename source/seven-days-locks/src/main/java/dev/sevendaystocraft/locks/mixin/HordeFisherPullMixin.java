package dev.sevendaystocraft.locks.mixin;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the EnhancedAI hook visual and timing, but bounds the horde role's pull. */
@Mixin(targets = "insane96mcp.enhancedai.modules.mobs.fisher.FishingHook", remap = false)
public abstract class HordeFisherPullMixin {
    @Inject(method = "pullEntity", at = @At("HEAD"), cancellable = true, remap = false)
    private void sevenDaysToCraft$boundedHordePull(Entity target, CallbackInfo callback) {
        Projectile hook = (Projectile) (Object) this;
        Entity owner = hook.getOwner();
        if (owner == null || !owner.getTags().contains("7dtc_role_fisher")) return;

        Vec3 towardOwner = owner.position().subtract(target.position());
        double horizontal = Math.hypot(towardOwner.x, towardOwner.z);
        if (horizontal < 0.01) {
            callback.cancel();
            return;
        }
        double force = Math.min(0.55, horizontal * 0.08);
        Vec3 impulse = new Vec3(towardOwner.x / horizontal * force,
            Mth.clamp(towardOwner.y * 0.04, -0.08, 0.18),
            towardOwner.z / horizontal * force);
        target.setDeltaMovement(target.getDeltaMovement().add(impulse));
        target.hurtMarked = true;
        callback.cancel();
    }
}
