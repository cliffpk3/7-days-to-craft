package dev.sevendaystocraft.locks;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import atomicstryker.infernalmobs.common.MobModifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.ModList;

/** Controlled Divine Weaponry drops for the depth and threat progression. */
final class DivineWeaponDrops {
    private static final String[] STONE = pool("stone");
    private static final String[] IRON = pool("iron");
    private static final String[] DIAMOND = pool("diamond");
    private static final String[] LEGENDARY = {
        "divine_weaponry:amaterasu_shuriken",
        "divine_weaponry:apophis_katana",
        "divine_weaponry:cernunnos_sickle",
        "divine_weaponry:chaos_sword",
        "divine_weaponry:erebus_scythe",
        "divine_weaponry:fujindagger",
        "divine_weaponry:hyperion_longsword",
        "divine_weaponry:jotunn_battleaxe",
        "divine_weaponry:mjollnir_hammer",
        "divine_weaponry:om_spear",
        "divine_weaponry:sobek_rapier",
        "divine_weaponry:tunupa_glaive",
        "divine_weaponry:zeus_cutlass"
    };

    private DivineWeaponDrops() {}

    static void dropGuaranteedIron(LivingDropsEvent event) {
        event.getDrops().removeIf(DivineWeaponDrops::isDivineWeapon);
        spawnWeapon(event.getEntity(), IRON, false);
    }

    static void apply(LivingDropsEvent event) {
        LivingEntity mob = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) return;

        // No native or equipment drop may bypass the centralized probabilities.
        event.getDrops().removeIf(DivineWeaponDrops::isDivineWeapon);

        int threatTier = ThreatSense.threatTier(mob);
        boolean hordeBoss = mob.getTags().contains("7dtc_horde_boss");
        MobModifier modifiers = ModList.get().isLoaded("infernalmobs")
            ? InfernalMobsCore.getMobModifiers(mob)
            : null;
        boolean infernal = modifiers != null;

        double legendaryChance = hordeBoss || threatTier >= 4
            ? 0.04D
            : infernal ? Math.min(0.03D, 0.004D * Math.max(1, modifiers.getModSize())) : 0.0D;
        double roll = mob.getRandom().nextDouble();

        String[] selected = null;
        boolean legendary = false;
        if (roll < legendaryChance) {
            selected = LEGENDARY;
            legendary = true;
        } else {
            double standard = (roll - legendaryChance) / Math.max(0.0001D, 1.0D - legendaryChance);
            // Elites remain a better source of equipment even after the global
            // reduction of direct weapon drops.
            double eliteMultiplier = infernal ? 1.5D : 1.0D;
            if (threatTier >= 4) {
                if (standard < 0.06D * eliteMultiplier) selected = DIAMOND;
                else if (standard < 0.10D * eliteMultiplier) selected = IRON;
            } else if (threatTier >= 2) {
                if (standard < 0.06D * eliteMultiplier) selected = IRON;
                else if (standard < 0.0675D * eliteMultiplier) selected = DIAMOND;
            } else {
                if (standard < 0.06D * eliteMultiplier) selected = STONE;
                else if (standard < 0.0675D * eliteMultiplier) selected = IRON;
            }
        }

        if (selected == null) return;
        spawnWeapon(mob, selected, legendary);
    }

    private static void spawnWeapon(LivingEntity mob, String[] pool, boolean legendary) {
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(pool[mob.getRandom().nextInt(pool.length)]));
        if (item == Items.AIR) return;
        ItemStack reward = new ItemStack(item);
        MobModifier modifiers = ModList.get().isLoaded("infernalmobs") ? InfernalMobsCore.getMobModifiers(mob) : null;
        if (modifiers != null) {
            int tier = Math.max(1, Math.min(4, ThreatSense.threatTier(mob)));
            int enchantLevel = 8 + tier * 5 + Math.min(5, modifiers.getModSize()) * 2;
            reward = EnchantmentHelper.enchantItem(mob.getRandom(), reward, enchantLevel, false);
            // Some Divine throwable items have no ordinary enchanting-table
            // candidates. Preserve the elite reward promise for damageable items.
            if (reward.isDamageableItem() && !reward.isEnchanted()) {
                int unbreaking = Math.min(3, 1 + tier / 2 + modifiers.getModSize() / 5);
                reward.enchant(Enchantments.UNBREAKING, unbreaking);
            }
        }
        applyFoundDurability(reward, mob, legendary);
        mob.spawnAtLocation(reward);
    }

    private static boolean isDivineWeapon(ItemEntity entity) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(entity.getItem().getItem());
        return id != null && id.getNamespace().equals("divine_weaponry");
    }

    private static void applyFoundDurability(ItemStack stack, LivingEntity mob, boolean legendary) {
        if (!stack.isDamageableItem()) return;
        // Standard finds retain 35-80% durability. Legendary finds retain
        // 70-95%, keeping both tiers inside the repair economy.
        double minimumDamage = legendary ? 0.05D : 0.20D;
        double maximumDamage = legendary ? 0.30D : 0.65D;
        double fraction = minimumDamage + mob.getRandom().nextDouble() * (maximumDamage - minimumDamage);
        int damage = Math.max(1, Math.min(stack.getMaxDamage() - 1, (int) Math.round(stack.getMaxDamage() * fraction)));
        stack.setDamageValue(damage);
    }

    private static String[] pool(String material) {
        String[] types = {
            "battleaxe", "cutlass", "dagger", "glaive", "hammer", "katana",
            "longsword", "rapier", "scythe", "shuriken", "sickle", "spear"
        };
        String[] result = new String[types.length];
        for (int index = 0; index < types.length; index++) {
            result[index] = "divine_weaponry:" + material + "_" + types[index];
        }
        return result;
    }
}
