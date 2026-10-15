package dev.sevendaystocraft.skillbridge.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps Quark's backpack out of the vanilla chest armor slot. */
@Mixin(targets = "org.violetmoon.quark.addons.oddities.item.BackpackItem", remap = false)
public abstract class QuarkBackpackEquipMixin {
    @Inject(method = "canEquipZeta", at = @At("HEAD"), cancellable = true)
    private void sevenDays$curiosBackSlotOnly(ItemStack stack, EquipmentSlot slot, Entity entity,
                                               CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
