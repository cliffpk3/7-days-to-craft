package dev.sevendaystocraft.skillbridge.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Makes Curios consider the backpack before its vanilla chestplate route. */
@Mixin(targets = {
    "top.theillusivec4.curios.common.inventory.container.CuriosContainer",
    "top.theillusivec4.curios.common.inventory.container.CuriosContainerV2"
}, remap = false)
public abstract class CuriosBackpackQuickMoveMixin {
    @Redirect(
        method = "m_7648_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Mob;m_147233_(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/EquipmentSlot;",
            remap = false
        ),
        remap = false
    )
    private EquipmentSlot sevenDays$preferCuriosBackSlot(ItemStack stack) {
        if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("quark:backpack")) {
            return EquipmentSlot.MAINHAND;
        }
        return net.minecraft.world.entity.Mob.getEquipmentSlotForItem(stack);
    }
}
