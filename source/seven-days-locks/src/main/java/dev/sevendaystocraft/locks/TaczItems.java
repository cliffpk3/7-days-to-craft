package dev.sevendaystocraft.locks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** TaCZ's gun and attachment variants share a registry item and require NBT. */
final class TaczItems {
    private TaczItems() {}

    static ItemStack gun(String id) {
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation("tacz:modern_kinetic_gun"));
        if (item == Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putString("GunId", "tacz:" + id);
        stack.getOrCreateTag().putString("GunFireMode", id.equals("uzi") || id.equals("m249") ? "AUTO" : "SEMI");
        return stack;
    }

    static ItemStack attachment(String id) {
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation("tacz:attachment"));
        if (item == Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(item);
        stack.getOrCreateTag().putString("AttachmentId", "tacz:" + id);
        return stack;
    }
}
