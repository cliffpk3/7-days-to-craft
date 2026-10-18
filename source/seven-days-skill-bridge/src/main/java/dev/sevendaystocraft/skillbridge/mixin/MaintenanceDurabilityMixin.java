package dev.sevendaystocraft.skillbridge.mixin;

import dev.sevendaystocraft.skillbridge.SkillBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies Maintenance before vanilla's Unbreaking roll, preserving their independent chances. */
@Mixin(ItemStack.class)
public abstract class MaintenanceDurabilityMixin {
    @Inject(method = "hurt(ILnet/minecraft/util/RandomSource;Lnet/minecraft/server/level/ServerPlayer;)Z",
            at = @At("HEAD"), cancellable = true)
    private void sevenDays$preserveDurability(int amount, RandomSource random, ServerPlayer player,
                                              CallbackInfoReturnable<Boolean> result) {
        if (player == null || amount <= 0 || !((ItemStack) (Object) this).isDamageableItem()) return;
        int rank = SkillBridge.getBasicSkillRank(player, "maintenance", 4);
        if (rank > 0 && random.nextFloat() < 0.10F * rank) result.setReturnValue(false);
    }
}
