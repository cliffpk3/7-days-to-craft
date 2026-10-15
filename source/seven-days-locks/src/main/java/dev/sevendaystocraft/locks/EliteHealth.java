package dev.sevendaystocraft.locks;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import atomicstryker.infernalmobs.common.MobModifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** Keeps even the weakest Infernal elite at three times its species' native HP. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EliteHealth {
    private static final String CHECKED = "7dtc_infernal_min_hp_v1";

    private EliteHealth() {}

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity mob = event.getEntity();
        if (mob.level().isClientSide() || mob.tickCount != 2 || mob.getPersistentData().getBoolean(CHECKED)) return;
        if (!ModList.get().isLoaded("infernalmobs")) return;
        MobModifier modifiers = InfernalMobsCore.getMobModifiers(mob);
        if (modifiers == null || !DefaultAttributes.hasSupplier(mob.getType())) return;

        mob.getPersistentData().putBoolean(CHECKED, true);
        @SuppressWarnings("unchecked")
        EntityType<? extends LivingEntity> type = (EntityType<? extends LivingEntity>) mob.getType();
        double nativeHealth = DefaultAttributes.getSupplier(type).getBaseValue(Attributes.MAX_HEALTH);
        double oldMax = mob.getMaxHealth();
        double requiredMax = nativeHealth * 3.0D;
        if (oldMax >= requiredMax || nativeHealth <= 0.0D) return;

        var healthAttribute = mob.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttribute == null) return;
        double healthFraction = Math.max(0.0D, Math.min(1.0D, mob.getHealth() / oldMax));

        healthAttribute.setBaseValue(healthAttribute.getBaseValue() + requiredMax - oldMax);
        float newMax = mob.getMaxHealth();
        float newHealth = (float) (newMax * healthFraction);
        modifiers.setActualHealth(newHealth, newMax);
        mob.getPersistentData().putFloat("infernalMaxHealth", newMax);
        mob.setHealth(newHealth);
        InfernalMobsCore.instance().sendHealthPacket(mob);
    }
}
