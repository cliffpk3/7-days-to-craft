package dev.sevendaystocraft.locks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FallenAdventurerKeys {
    private static final String[] KEYS = {
        "",
        "wrd:keystrongholdchest_iron",
        "wrd:keystrongholdchest_gold",
        "wrd:keystrongholdchest_diamond",
        "wrd:keystrongholdchest_treasure"
    };
    private static final double[] CHANCES = {0.0, 0.60, 0.30, 0.20, 0.10};

    private FallenAdventurerKeys() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide() || event.getPlayer().getAbilities().instabuild) return;

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
        if (!blockId.getNamespace().equals("realmrpg_skeletons") || !blockId.getPath().endsWith("_skeleton")) return;
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, event.getPlayer().getMainHandItem()) > 0) return;

        // Check the rarest key first and stop after the first success. The old
        // independent rolls could award several different keys for one skeleton.
        for (int tier = 4; tier >= 1; tier--) {
            double chance = CHANCES[tier];
            if (event.getPlayer().getTags().contains("7dtc_key_hunter")) {
                chance += switch (tier) { case 1 -> 0.08; case 2 -> 0.06; case 3, 4 -> 0.05; default -> 0.0; };
            }
            if (event.getPlayer().getRandom().nextDouble() < chance) {
                Item key = BuiltInRegistries.ITEM.get(new ResourceLocation(KEYS[tier]));
                Block.popResource(event.getPlayer().level(), event.getPos(), new ItemStack(key));
                return;
            }
        }
    }
}
