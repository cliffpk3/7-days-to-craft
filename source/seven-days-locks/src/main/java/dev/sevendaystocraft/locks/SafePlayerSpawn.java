package dev.sevendaystocraft.locks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Moves tree-top spawns to nearby ground, without changing established bases. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SafePlayerSpawn {
    private static final int SEARCH_RADIUS = 24;

    private SafePlayerSpawn() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // Existing players may intentionally live in trees; only correct their
        // first spawn in this world.
        if (player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) > 20) return;
        player.server.execute(() -> correctTreeSpawn(player));
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered() || !(event.getEntity() instanceof ServerPlayer player)) return;
        player.server.execute(() -> correctTreeSpawn(player));
    }

    private static void correctTreeSpawn(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        var support = level.getBlockState(origin.below());
        if (!support.is(BlockTags.LEAVES) && !support.is(BlockTags.LOGS)) return;

        for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) continue;
                    BlockPos feet = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        new BlockPos(x, 0, z));
                    if (!isSafe(level, feet)) continue;
                    player.teleportTo(level, x + 0.5D, feet.getY(), z + 0.5D,
                        player.getYRot(), player.getXRot());
                    player.fallDistance = 0;
                    return;
                }
            }
        }
    }

    private static boolean isSafe(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        var floor = level.getBlockState(below);
        if (floor.is(BlockTags.LEAVES) || floor.is(BlockTags.LOGS)
            || floor.is(Blocks.MAGMA_BLOCK) || floor.is(Blocks.CACTUS)
            || floor.is(Blocks.CAMPFIRE) || floor.is(Blocks.SOUL_CAMPFIRE)
            || floor.is(Blocks.POWDER_SNOW)) return false;
        return floor.isFaceSturdy(level, below, Direction.UP)
            && level.getBlockState(feet).isAir()
            && level.getBlockState(feet.above()).isAir();
    }
}
