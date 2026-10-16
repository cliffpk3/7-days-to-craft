package dev.sevendaystocraft.locks;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** Per-entity elite awareness; ordinary mobs keep their original AI. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
final class EliteAwareness {
    private static final double SIGHT_RANGE = 24.0D;

    private EliteAwareness() {}

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()
                || mob.tickCount % 20 != 0 || !ModList.get().isLoaded("infernalmobs")) return;
        if (InfernalMobsCore.getMobModifiers(mob) == null) return;

        // Infernal Mobs assigns a custom name on spawn. An older 7DTC hook
        // hid it every second; restore it for both new and existing elites.
        if (mob.getCustomName() == null) {
            mob.setCustomName(Component.literal("Elite ").append(mob.getType().getDescription()));
        }
        if (!mob.isCustomNameVisible()) mob.setCustomNameVisible(true);

        // Find an unoccluded player before the usual short-range AI notices
        // them. This grants awareness without x-ray vision or extra equipment.
        if (mob.getTarget() != null) return;
        ServerPlayer nearest = null;
        double nearestDistance = SIGHT_RANGE * SIGHT_RANGE;
        for (ServerPlayer player : mob.level().getEntitiesOfClass(ServerPlayer.class,
                mob.getBoundingBox().inflate(SIGHT_RANGE))) {
            if (player.isSpectator() || player.isCreative() || !mob.hasLineOfSight(player)) continue;
            double distance = mob.distanceToSqr(player);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        if (nearest != null) mob.setTarget(nearest);
    }
}
