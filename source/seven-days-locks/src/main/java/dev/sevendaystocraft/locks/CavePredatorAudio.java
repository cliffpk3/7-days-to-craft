package dev.sevendaystocraft.locks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Comparator;

/**
 * Gives the pack's fast subterranean predators an audible approach cue.
 * Their native movement lets them cover the normal spawn-safe radius before
 * a distracted player sees them, especially through cave walls and ceilings.
 */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CavePredatorAudio {
    private static final ResourceLocation ROCKY_ROLLER = new ResourceLocation("alexsmobs", "rocky_roller");
    private static final ResourceLocation CENTIPEDE = new ResourceLocation("alexsmobs", "centipede_head");
    private static final ResourceLocation ROCKY_SOUND = new ResourceLocation("alexsmobs", "rocky_roller_idle");
    private static final ResourceLocation CENTIPEDE_SOUND = new ResourceLocation("alexsmobs", "centipede_walk");
    private static final String NEXT_CUE = "7dtc_next_cave_predator_cue";

    private CavePredatorAudio() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;

        Mob predator = player.serverLevel().getEntitiesOfClass(
            Mob.class,
            player.getBoundingBox().inflate(24.0D),
            mob -> mob.isAlive() && isPredator(mob) && (mob.getTarget() == player || mob.distanceToSqr(player) <= 18.0D * 18.0D)
        ).stream().min(Comparator.comparingDouble(mob -> mob.distanceToSqr(player))).orElse(null);

        if (predator == null) return;
        long now = player.level().getGameTime();
        if (now < predator.getPersistentData().getLong(NEXT_CUE)) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(predator.getType());
        ResourceLocation soundId = ROCKY_ROLLER.equals(id) ? ROCKY_SOUND : CENTIPEDE_SOUND;
        SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(soundId);
        if (sound == null) return;

        player.serverLevel().playSound(null, predator.blockPosition(), sound, SoundSource.HOSTILE, 1.35F, ROCKY_ROLLER.equals(id) ? 0.82F : 0.92F);
        predator.getPersistentData().putLong(NEXT_CUE, now + 80L);
    }

    private static boolean isPredator(Mob mob) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        return ROCKY_ROLLER.equals(id) || CENTIPEDE.equals(id);
    }
}
