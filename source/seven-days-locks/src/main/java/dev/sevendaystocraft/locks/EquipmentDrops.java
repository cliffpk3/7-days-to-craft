package dev.sevendaystocraft.locks;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import atomicstryker.infernalmobs.common.MobModifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.ModList;

/** Low-volume equipment finds, separate from Divine Weaponry and native drops. */
final class EquipmentDrops {
    private static final String[][] ARMOR = {
        {},
        {
            "minecraft:leather_helmet", "minecraft:leather_chestplate", "minecraft:leather_leggings", "minecraft:leather_boots",
            "minecraft:chainmail_helmet", "minecraft:chainmail_boots"
        },
        {
            "minecraft:chainmail_helmet", "minecraft:chainmail_chestplate", "minecraft:chainmail_leggings", "minecraft:chainmail_boots",
            "minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots"
        },
        {
            "minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots",
            "minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots",
            "minecraft:diamond_helmet", "minecraft:diamond_chestplate", "minecraft:diamond_leggings", "minecraft:diamond_boots"
        },
        {
            "minecraft:diamond_helmet", "minecraft:diamond_chestplate", "minecraft:diamond_leggings", "minecraft:diamond_boots"
        }
    };

    private static final String[][] FIREARMS = {
        {},
        {"glock_17", "m1911", "db_short"},
        {"p320", "hk_mp5a5", "uzi", "m870", "sks_tactical"},
        {"m4a1", "ak47", "scar_l", "m1014", "m700"},
        {"deagle", "hk416d", "rpk", "m107"}
    };

    private EquipmentDrops() {}

    static void apply(LivingDropsEvent event) {
        LivingEntity mob = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) return;

        int tier = Math.max(1, Math.min(4, ThreatSense.threatTier(mob)));
        double armorChance = switch (tier) {
            case 1 -> 0.020D;
            case 2 -> 0.035D;
            case 3 -> 0.055D;
            default -> 0.080D;
        };
        if (mob.getRandom().nextDouble() < armorChance) {
            spawnFoundItem(mob, ARMOR[tier][mob.getRandom().nextInt(ARMOR[tier].length)], 0.25D, 0.75D);
        }

        MobModifier modifiers = ModList.get().isLoaded("infernalmobs")
            ? InfernalMobsCore.getMobModifiers(mob)
            : null;
        boolean boss = mob.getTags().contains("7dtc_horde_boss") || tier >= 4;
        double firearmChance = boss
            ? 0.010D
            : modifiers == null ? 0.0D : Math.min(0.015D, 0.0015D * Math.max(1, modifiers.getModSize()));
        if (mob.getRandom().nextDouble() < firearmChance) {
            String[] pool = FIREARMS[tier];
            ItemStack gun = TaczItems.gun(pool[mob.getRandom().nextInt(pool.length)]);
            if (!gun.isEmpty()) mob.spawnAtLocation(gun);
        }
    }

    private static void spawnFoundItem(LivingEntity mob, String id, double minimumDamage, double maximumDamage) {
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
        if (item == Items.AIR) return;
        ItemStack stack = new ItemStack(item);
        if (stack.isDamageableItem()) {
            double fraction = minimumDamage + mob.getRandom().nextDouble() * (maximumDamage - minimumDamage);
            int damage = Math.max(1, Math.min(stack.getMaxDamage() - 1, (int) Math.round(stack.getMaxDamage() * fraction)));
            stack.setDamageValue(damage);
        }
        mob.spawnAtLocation(stack);
    }
}
