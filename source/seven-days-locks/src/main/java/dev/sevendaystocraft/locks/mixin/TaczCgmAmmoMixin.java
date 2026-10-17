package dev.sevendaystocraft.locks.mixin;

import dev.sevendaystocraft.locks.TaczAmmoTypes;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.PlayerInvWrapper;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reuse CGM's three ammunition items when reloading the curated TaCZ guns. */
@Mixin(targets = "com.tacz.guns.api.item.gun.AbstractGunItem", remap = false)
public abstract class TaczCgmAmmoMixin {
    private static Item ammoFor(ItemStack gun) {
        if (!gun.hasTag()) return Items.AIR;
        String id = gun.getTag().getString("GunId");
        String ammo = TaczAmmoTypes.ammoId(id);
        if (ammo.isEmpty()) return Items.AIR;
        return BuiltInRegistries.ITEM.get(new ResourceLocation(ammo));
    }

    @Inject(method = "canReload", at = @At("RETURN"), cancellable = true, remap = false)
    private void sevenDaysToCraft$allowCgmReload(net.minecraft.world.entity.LivingEntity shooter, ItemStack gun,
                                                   CallbackInfoReturnable<Boolean> callback) {
        if (!(shooter instanceof Player) || !(gun.getItem() instanceof IGun gunItem)) return;
        Item ammo = ammoFor(gun);
        if (ammo == Items.AIR || gunItem.useInventoryAmmo(gun)) return;
        boolean hasRoom = TimelessAPI.getCommonGunIndex(gunItem.getGunId(gun))
            .map(index -> gunItem.getCurrentAmmoCount(gun)
                < AttachmentDataUtils.getAmmoCountWithAttachment(gun, index.getGunData()))
            .orElse(false);
        if (!hasRoom) {
            callback.setReturnValue(false);
            return;
        }
        boolean hasAmmo = shooter.getCapability(ForgeCapabilities.ITEM_HANDLER).map(inventory -> {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                if (inventory.getStackInSlot(slot).is(ammo)) return true;
            }
            return false;
        }).orElse(false);
        callback.setReturnValue(hasAmmo);
    }

    @Inject(method = "findAndExtractInventoryAmmo", at = @At("HEAD"), cancellable = true, remap = false)
    private void sevenDaysToCraft$consumeCgmAmmo(IItemHandler inventory, ItemStack gun, int requested,
                                                  CallbackInfoReturnable<Integer> callback) {
        Item ammo = ammoFor(gun);
        // Simple Enemy soldiers use TaCZ's own AI inventory. Preserve their
        // reload behavior; the CGM substitution is for players' ammunition.
        if (ammo == Items.AIR || requested <= 0 || !(inventory instanceof PlayerInvWrapper)) return;
        int remaining = requested;
        for (int slot = 0; slot < inventory.getSlots() && remaining > 0; slot++) {
            if (!inventory.getStackInSlot(slot).is(ammo)) continue;
            remaining -= inventory.extractItem(slot, remaining, false).getCount();
        }
        callback.setReturnValue(requested - remaining);
    }

    @Inject(method = "dropAllAmmo", at = @At("HEAD"), cancellable = true, remap = false)
    private void sevenDaysToCraft$returnCgmAmmo(Player player, ItemStack gun, CallbackInfo callback) {
        Item ammo = ammoFor(gun);
        if (ammo == Items.AIR) return;
        int count = gun.getOrCreateTag().getInt("GunCurrentAmmoCount");
        gun.getOrCreateTag().putInt("GunCurrentAmmoCount", 0);
        gun.getOrCreateTag().putBoolean("HasBulletInBarrel", false);
        while (count > 0) {
            int split = Math.min(count, ammo.getMaxStackSize());
            ItemStack returned = new ItemStack(ammo, split);
            if (!player.addItem(returned)) player.drop(returned, false);
            count -= split;
        }
        callback.cancel();
    }
}
