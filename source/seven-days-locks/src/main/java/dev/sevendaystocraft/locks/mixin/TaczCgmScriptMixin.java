package dev.sevendaystocraft.locks.mixin;

import dev.sevendaystocraft.locks.TaczAmmoTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** TaCZ's gun scripts perform a second ammo check after canReload. */
@Mixin(targets = "com.tacz.guns.item.ModernKineticGunScriptAPI", remap = false)
public abstract class TaczCgmScriptMixin {
    @Shadow(remap = false)
    public abstract LivingEntity getShooter();

    @Shadow(remap = false)
    public abstract ItemStack getItemStack();

    @Inject(method = "hasAmmoToConsume", at = @At("RETURN"), cancellable = true, remap = false)
    private void sevenDaysToCraft$acceptCgmAmmo(CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue() || !(getShooter() instanceof Player player)) return;
        ItemStack gun = getItemStack();
        if (!gun.hasTag()) return;
        String ammoId = TaczAmmoTypes.ammoId(gun.getTag().getString("GunId"));
        if (ammoId.isEmpty()) return;
        Item ammo = BuiltInRegistries.ITEM.get(new ResourceLocation(ammoId));
        if (ammo == Items.AIR) return;
        boolean hasAmmo = player.getCapability(ForgeCapabilities.ITEM_HANDLER).map(inventory -> {
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                if (inventory.getStackInSlot(slot).is(ammo)) return true;
            }
            return false;
        }).orElse(false);
        if (hasAmmo) callback.setReturnValue(true);
    }
}
