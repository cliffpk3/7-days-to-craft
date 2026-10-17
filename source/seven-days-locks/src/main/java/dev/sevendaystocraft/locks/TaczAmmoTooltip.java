package dev.sevendaystocraft.locks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Show the CGM ammunition actually consumed by curated TaCZ guns. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TaczAmmoTooltip {
    private TaczAmmoTooltip() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !stack.hasTag()) return;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!new ResourceLocation("tacz", "modern_kinetic_gun").equals(itemId)) return;
        String ammoId = TaczAmmoTypes.ammoId(stack.getTag().getString("GunId"));
        if (ammoId.isEmpty()) return;
        Item ammo = BuiltInRegistries.ITEM.get(new ResourceLocation(ammoId));
        if (ammo == Items.AIR) return;
        event.getToolTip().add(Component.translatable("tooltip.seven_days_locks.tacz_ammo",
            ammo.getDescription()).withStyle(ChatFormatting.GRAY));
    }
}
