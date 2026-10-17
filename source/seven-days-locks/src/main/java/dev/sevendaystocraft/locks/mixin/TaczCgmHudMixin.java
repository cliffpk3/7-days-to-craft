package dev.sevendaystocraft.locks.mixin;

import dev.sevendaystocraft.locks.TaczAmmoTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Match TaCZ's reserve counter to the same CGM ammo consumed by reloads. */
@Mixin(targets = "com.tacz.guns.client.gui.overlay.GunHudOverlay", remap = false)
public abstract class TaczCgmHudMixin {
    @Shadow(remap = false)
    private static int cacheInventoryAmmoCount;

    @Inject(method = "handleInventoryAmmo", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sevenDaysToCraft$countCgmAmmo(ItemStack gun, Inventory inventory, CallbackInfo callback) {
        if (!gun.hasTag()) return;
        String ammoId = TaczAmmoTypes.ammoId(gun.getTag().getString("GunId"));
        if (ammoId.isEmpty()) return;
        Item ammo = BuiltInRegistries.ITEM.get(new ResourceLocation(ammoId));
        if (ammo == Items.AIR) return;
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ammo)) count += stack.getCount();
        }
        cacheInventoryAmmoCount = count;
        callback.cancel();
    }
}
