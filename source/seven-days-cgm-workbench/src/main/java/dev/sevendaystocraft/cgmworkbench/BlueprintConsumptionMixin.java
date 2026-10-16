package dev.sevendaystocraft.cgmworkbench;

import com.mrcrayfish.guns.crafting.WorkbenchIngredient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Decrease one blueprint use when CGM consumes a Workbench ingredient. */
@Mixin(targets = "com.mrcrayfish.guns.util.InventoryUtil", remap = false)
public abstract class BlueprintConsumptionMixin {
    private static final String USES_TAG = "7dtc_blueprint_uses";

    @Inject(method = "removeWorkstationIngredient", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sevenDaysToCraft$consumeUse(Player player, WorkbenchIngredient ingredient,
                                                     CallbackInfoReturnable<Boolean> callback) {
        if (ingredient.getCount() != 1) return;
        Inventory inventory = player.m_150109_();
        for (int slot = 0; slot < inventory.m_6643_(); slot++) {
            ItemStack stack = inventory.m_8020_(slot);
            if (stack.m_41619_() || !ingredient.test(stack)) continue;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
            if (id == null || !id.toString().startsWith("seven_days_locks:blueprint_")) continue;
            int uses = stack.m_41782_() ? Math.max(1, stack.m_41783_().m_128451_(USES_TAG)) : 1;
            if (uses <= 1) {
                stack.m_41774_(1);
                if (stack.m_41619_()) inventory.m_6836_(slot, ItemStack.f_41583_);
            } else if (stack.m_41613_() == 1) {
                stack.m_41784_().m_128405_(USES_TAG, uses - 1);
            } else {
                // Compatibility with stacks obtained before blueprints became unstackable.
                ItemStack single = stack.m_41620_(1);
                single.m_41784_().m_128405_(USES_TAG, uses - 1);
                if (!inventory.m_36054_(single)) player.m_36176_(single, false);
            }
            inventory.m_6596_();
            callback.setReturnValue(true);
            return;
        }
    }
}
