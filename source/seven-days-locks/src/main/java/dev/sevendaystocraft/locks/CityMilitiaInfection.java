package dev.sevendaystocraft.locks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.nekoyuni.SimpleEnemyMod.entity.unit.USunitEntity;

/** Reanimate city militiamen killed by infected without changing Simple Enemy's targeting AI. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CityMilitiaInfection {
    private static final String CONVERTED_TAG = "7dtc_reanimated_militia";
    private static final int MAX_REANIMATED_NEARBY = 4;
    private static final int MAX_HOSTILES_NEARBY = 30;

    private CityMilitiaInfection() {}

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof USunitEntity unit)
            || !(unit.level() instanceof ServerLevel level)
            || !ModList.get().isLoaded("zombie_extreme")
            || CityRaid.cityStart(level, unit.blockPosition()) == null
            || !(event.getSource().getEntity() instanceof Mob killer)
            || !CityAmbientEncounter.isCityZombie(killer)) return;

        // Zombie Extreme already reanimates victims when its own infection effect expires.
        // This path handles a direct zombie kill; no extra loot or equipment is copied.
        if (level.getEntitiesOfClass(Mob.class, unit.getBoundingBox().inflate(32, 12, 32),
            mob -> mob.getTags().contains(CONVERTED_TAG)).size() >= MAX_REANIMATED_NEARBY) return;
        if (level.getEntitiesOfClass(Mob.class, unit.getBoundingBox().inflate(32, 12, 32),
            CityAmbientEncounter::isCityZombie).size() >= MAX_HOSTILES_NEARBY) return;

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(
            new ResourceLocation("zombie_extreme:infected_military"));
        if (type == null) return;
        BlockPos origin = unit.blockPosition();
        for (int attempt = 0; attempt < 12; attempt++) {
            int x = origin.getX() + level.random.nextInt(7) - 3;
            int z = origin.getZ() + level.random.nextInt(7) - 3;
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            // Keep indoor deaths indoors when there is room; the surface is a fallback.
            BlockPos pos = new BlockPos(x, origin.getY(), z);
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
                pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(x, 0, z));
            }
            if (Math.abs(pos.getY() - origin.getY()) > 3
                || CityRaid.cityStart(level, pos) == null
                || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()
                || !(type.create(level) instanceof Mob infected)) continue;
            infected.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                level.random.nextFloat() * 360F, 0F);
            if (!level.noCollision(infected)) continue;
            infected.addTag("7dtc_city_raid");
            infected.addTag(CONVERTED_TAG);
            infected.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            if (level.addFreshEntity(infected)) return;
        }
    }
}
