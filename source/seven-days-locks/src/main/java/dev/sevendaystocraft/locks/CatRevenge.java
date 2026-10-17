package dev.sevendaystocraft.locks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Cat;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/** A domestic cat only becomes dangerous after a player attacks it. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CatRevenge {
    private static final String TARGET = "7dtc_cat_revenge_target";
    private static final String EXPIRES = "7dtc_cat_revenge_expires";
    private static final String NEXT_ATTACK = "7dtc_cat_revenge_next_attack";
    private static final long REVENGE_TICKS = 20L * 60L;

    private CatRevenge() {}

    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Cat cat) || !(cat.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof ServerPlayer attacker)
                || attacker.isCreative() || attacker.isSpectator()) return;

        CompoundTag data = cat.getPersistentData();
        boolean firstStrike = !data.hasUUID(TARGET);
        data.putUUID(TARGET, attacker.getUUID());
        data.putLong(EXPIRES, level.getGameTime() + REVENGE_TICKS);
        data.putLong(NEXT_ATTACK, level.getGameTime());
        cat.setOrderedToSit(false);
        cat.setTarget(attacker);
        cat.playSound(SoundEvents.CAT_HISS, 1.0F, 0.8F);

        // A single strong opening hit would otherwise kill the cat before it can retaliate.
        if (firstStrike) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Cat cat) || !(cat.level() instanceof ServerLevel level)) return;
        CompoundTag data = cat.getPersistentData();
        if (!data.hasUUID(TARGET)) return;

        UUID targetId = data.getUUID(TARGET);
        ServerPlayer target = level.getServer().getPlayerList().getPlayer(targetId);
        if (level.getGameTime() >= data.getLong(EXPIRES) || target == null || !target.isAlive()
                || target.level() != level || target.isCreative() || target.isSpectator()
                || cat.distanceToSqr(target) > 64.0D * 64.0D) {
            clear(cat, data);
            return;
        }

        cat.setTarget(target);
        cat.getNavigation().moveTo(target, 1.7D);
        if (cat.distanceToSqr(target) <= 4.0D && level.getGameTime() >= data.getLong(NEXT_ATTACK)) {
            cat.swing(InteractionHand.MAIN_HAND);
            target.hurt(cat.damageSources().mobAttack(cat), 1000.0F);
            data.putLong(NEXT_ATTACK, level.getGameTime() + 20L);
            if (!target.isAlive()) clear(cat, data);
        }
    }

    private static void clear(Cat cat, CompoundTag data) {
        data.remove(TARGET);
        data.remove(EXPIRES);
        data.remove(NEXT_ATTACK);
        cat.setTarget(null);
        cat.getNavigation().stop();
    }
}
