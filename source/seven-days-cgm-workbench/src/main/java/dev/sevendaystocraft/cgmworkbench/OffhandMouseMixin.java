package dev.sevendaystocraft.cgmworkbench;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MouseHandler.class, remap = false)
public abstract class OffhandMouseMixin {
    @Shadow
    @Final
    private Minecraft f_91503_;

    @Inject(method = "m_91530_", at = @At("HEAD"), cancellable = true, remap = false)
    private void sevenDaysToCraft$useOffhand(long window, int button, int action, int modifiers, CallbackInfo ci) {
        if (button != 2 || f_91503_.f_91080_ != null) return;
        ci.cancel();
        if (action != 1) return;

        LocalPlayer player = f_91503_.f_91074_;
        MultiPlayerGameMode gameMode = f_91503_.f_91072_;
        if (player == null || gameMode == null) return;

        InteractionHand hand = InteractionHand.OFF_HAND;
        InteractionResult result = InteractionResult.PASS;
        HitResult hit = f_91503_.f_91077_;

        if (hit != null && hit.m_6662_() == HitResult.Type.BLOCK) {
            result = gameMode.m_233732_(player, hand, (BlockHitResult) hit);
        } else if (hit != null && hit.m_6662_() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hit;
            Entity entity = entityHit.m_82443_();
            result = gameMode.m_105230_(player, entity, entityHit, hand);
            if (!result.m_19077_()) result = gameMode.m_105226_(player, entity, hand);
        } else {
            result = gameMode.m_233721_(player, hand);
        }

        if (result.m_19077_()) player.m_6674_(hand);
    }
}