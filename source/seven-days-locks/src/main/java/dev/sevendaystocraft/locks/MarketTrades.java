package dev.sevendaystocraft.locks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MarketTrades {
    private MarketTrades() {
    }

    @SubscribeEvent
    public static void addMarketTrades(VillagerTradesEvent event) {
        VillagerProfession profession = event.getType();

        if (profession == VillagerProfession.WEAPONSMITH) {
            add(event, 1,
                trade("cgm:basic_bullet", 8, 16, 4, 7, 4, 2, 0.90F),
                trade("minecraft:gunpowder", 6, 12, 4, 7, 4, 2, 0.85F),
                trade("minecraft:iron_ingot", 5, 9, 5, 8, 4, 2, 0.80F),
                trade("iron_repair_kits:copper_repair_kit", 1, 1, 12, 18, 2, 5, 0.35F));
            add(event, 2,
                trade("cgm:shell", 4, 8, 8, 13, 3, 10, 0.70F),
                trade("cgm:advanced_bullet", 4, 7, 10, 16, 2, 10, 0.55F),
                trade("minecraft:redstone", 8, 16, 5, 9, 4, 5, 0.75F),
                trade("minecraft:slime_ball", 2, 5, 7, 12, 3, 5, 0.55F));
            add(event, 3,
                trade("cgm:basic_bullet", 16, 28, 10, 16, 3, 15, 0.80F),
                trade("minecraft:ender_pearl", 1, 1, 18, 26, 1, 20, 0.30F),
                trade("minecraft:diamond", 1, 1, 22, 30, 1, 20, 0.20F),
                trade("minecraft:amethyst_shard", 3, 6, 10, 16, 2, 15, 0.45F));
            add(event, 4,
                trade("cgm:advanced_bullet", 7, 12, 18, 26, 2, 25, 0.65F),
                trade("cgm:shell", 8, 14, 16, 24, 2, 25, 0.65F),
                trade("cgm:stun_grenade", 1, 1, 28, 38, 1, 30, 0.22F),
                trade("minecraft:echo_shard", 1, 1, 36, 48, 1, 30, 0.12F));
            add(event, 5,
                trade("cgm:grenade", 1, 1, 40, 56, 1, 40, 0.18F),
                trade("cgm:advanced_bullet", 12, 18, 28, 40, 1, 35, 0.55F),
                trade("minecraft:diamond", 1, 2, 34, 48, 1, 35, 0.25F));
        }

        if (profession == VillagerProfession.TOOLSMITH) {
            add(event, 1,
                trade("minecraft:andesite", 12, 24, 3, 6, 5, 2, 0.90F),
                trade("minecraft:iron_nugget", 16, 32, 4, 7, 5, 2, 0.90F),
                trade("minecraft:copper_ingot", 5, 10, 5, 8, 4, 2, 0.80F));
            add(event, 2,
                trade("create:andesite_alloy", 1, 3, 9, 15, 3, 10, 0.45F),
                trade("minecraft:redstone", 8, 16, 5, 9, 4, 5, 0.75F),
                trade("minecraft:smooth_stone", 8, 16, 4, 7, 4, 5, 0.70F));
            add(event, 3,
                trade("create:shaft", 2, 4, 10, 16, 2, 15, 0.40F),
                trade("create:cogwheel", 1, 3, 12, 18, 2, 15, 0.35F),
                trade("minecraft:iron_bars", 8, 16, 8, 13, 3, 15, 0.65F));
            add(event, 4,
                trade("create:andesite_casing", 1, 2, 18, 28, 2, 25, 0.28F),
                trade("create:large_cogwheel", 1, 2, 20, 30, 1, 25, 0.25F),
                trade("create:whisk", 1, 1, 30, 42, 1, 30, 0.15F));
        }

        if (profession == VillagerProfession.LEATHERWORKER) {
            add(event, 1,
                trade("minecraft:leather", 5, 10, 4, 7, 5, 2, 0.90F),
                trade("minecraft:string", 8, 16, 3, 6, 5, 2, 0.85F),
                trade("minecraft:rabbit_hide", 4, 8, 5, 9, 4, 2, 0.70F));
            add(event, 2,
                trade("minecraft:black_dye", 4, 8, 4, 8, 4, 5, 0.70F),
                trade("minecraft:gray_dye", 4, 8, 4, 8, 4, 5, 0.70F),
                trade("quark:ravager_hide", 1, 1, 24, 34, 1, 15, 0.18F));
            add(event, 3,
                trade("minecraft:leather", 12, 20, 10, 16, 3, 15, 0.75F),
                trade("minecraft:feather", 8, 16, 5, 9, 4, 10, 0.70F),
                trade("quark:ravager_hide", 1, 2, 30, 44, 1, 20, 0.22F));
        }

        if (profession == VillagerProfession.CLERIC) {
            add(event, 1,
                trade("minecraft:redstone", 6, 12, 4, 7, 4, 2, 0.85F),
                trade("minecraft:coal", 8, 16, 3, 6, 5, 2, 0.80F));
            add(event, 2,
                trade("minecraft:slime_ball", 2, 5, 7, 12, 3, 10, 0.55F),
                trade("minecraft:amethyst_shard", 3, 6, 8, 14, 3, 10, 0.55F),
                trade("minecraft:golden_apple", 1, 1, 22, 30, 1, 15, 0.22F));
            add(event, 3,
                trade("minecraft:ender_pearl", 1, 1, 18, 26, 1, 20, 0.30F),
                trade("minecraft:tinted_glass", 2, 4, 10, 16, 2, 15, 0.45F),
                trade("minecraft:comparator", 1, 2, 12, 18, 2, 15, 0.45F));
            add(event, 4,
                trade("minecraft:echo_shard", 1, 1, 38, 52, 1, 30, 0.12F),
                trade("fracturepoint:nvg_battery", 1, 1, 32, 46, 1, 30, 0.16F));
        }

        if (profession == VillagerProfession.BUTCHER) {
            add(event, 1,
                trade("minecraft:cooked_beef", 4, 8, 3, 6, 5, 2, 0.85F),
                trade("minecraft:cooked_porkchop", 4, 8, 3, 6, 5, 2, 0.85F),
                trade("minecraft:bread", 6, 12, 3, 5, 6, 2, 0.90F));
            add(event, 2,
                trade("minecraft:cooked_chicken", 6, 12, 4, 7, 5, 5, 0.75F),
                trade("minecraft:baked_potato", 8, 16, 4, 7, 5, 5, 0.75F),
                trade("minecraft:golden_carrot", 2, 4, 8, 13, 3, 10, 0.35F));
        }
    }

    @SafeVarargs
    private static void add(VillagerTradesEvent event, int level, VillagerTrades.ItemListing... offers) {
        List<VillagerTrades.ItemListing> trades = event.getTrades().get(level);
        if (trades != null) trades.addAll(List.of(offers));
    }

    private static VillagerTrades.ItemListing trade(String result, int minCount, int maxCount,
                                                     int minEmeralds, int maxEmeralds,
                                                     int maxUses, int xp, float availability) {
        return new LimitedMarketOffer(new ResourceLocation(result), minCount, maxCount,
            minEmeralds, maxEmeralds, maxUses, xp, availability);
    }

    private record LimitedMarketOffer(ResourceLocation resultId, int minCount, int maxCount,
                                      int minEmeralds, int maxEmeralds,
                                      int maxUses, int xp, float availability)
        implements VillagerTrades.ItemListing {

        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            if (random.nextFloat() > availability) return null;
            Item result = BuiltInRegistries.ITEM.get(resultId);
            if (result == Items.AIR) return null;
            int count = minCount + random.nextInt(maxCount - minCount + 1);
            int emeralds = minEmeralds + random.nextInt(maxEmeralds - minEmeralds + 1);
            return new MerchantOffer(new ItemStack(Items.EMERALD, emeralds),
                new ItemStack(result, count), maxUses, xp, 0.08F);
        }
    }
}