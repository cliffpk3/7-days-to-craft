package dev.sevendaystocraft.locks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.smileycorp.hordes.common.event.HordeEndEvent;
import net.smileycorp.hordes.common.event.HordeFindSpawnPosEvent;
import net.smileycorp.hordes.common.event.HordeStartWaveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Keeps horde waves from materializing beside or in front of players. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HordeSpawnPlacement {
    private static final int MIN_DISTANCE = 80;
    private static final int MAX_DISTANCE = 112;
    private static final int MIN_OTHER_PLAYER_DISTANCE = 64;
    private static final int ATTEMPTS = 96;
    private static final Map<UUID, Placement> PLACEMENTS = new HashMap<>();

    private HordeSpawnPlacement() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWave(HordeStartWaveEvent event) {
        ServerPlayer player = event.getPlayer();
        ServerLevel level = player.serverLevel();
        BlockPos position = findConcealedPosition(level, player, event);
        if (position == null) {
            // The Hordes normally falls back to the player's block when its
            // search fails. Skip this wave instead of spawning in plain view.
            PLACEMENTS.remove(player.getUUID());
            event.setCount(0);
            return;
        }
        PLACEMENTS.put(player.getUUID(), new Placement(position, level.getGameTime()));
    }

    @SubscribeEvent
    public static void onFindPosition(HordeFindSpawnPosEvent event) {
        ServerPlayer player = event.getPlayer();
        Placement placement = PLACEMENTS.get(player.getUUID());
        if (placement != null && placement.tick == player.level().getGameTime()) {
            event.setPos(placement.position);
        }
    }

    @SubscribeEvent
    public static void onEnd(HordeEndEvent event) {
        PLACEMENTS.remove(event.getPlayer().getUUID());
    }

    private static BlockPos findConcealedPosition(ServerLevel level, ServerPlayer player,
                                                  HordeStartWaveEvent event) {
        BlockPos outOfViewFallback = null;
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            double angle = event.getRandom().nextDouble() * Math.PI * 2.0D;
            int distance = MIN_DISTANCE + event.getRandom().nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
            int x = player.blockPosition().getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = player.blockPosition().getZ() + (int) Math.round(Math.sin(angle) * distance);
            if (!level.hasChunk(x >> 4, z >> 4)) continue;

            BlockPos position = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, 0, z));
            if (event.getSpawnData() == null || event.getSpawnData().getSpawnType() == null
                || !event.getSpawnData().getSpawnType().canSpawn(level, position)) continue;
            int concealment = concealment(level, position);
            if (concealment == 2) return position;
            if (concealment == 1 && outOfViewFallback == null) outOfViewFallback = position;
        }
        return outOfViewFallback;
    }

    // 0: visible/too close; 1: behind every observer; 2: blocked by solid cover.
    private static int concealment(ServerLevel level, BlockPos base) {
        boolean allCovered = true;
        for (ServerPlayer observer : level.players()) {
            if (observer.isSpectator()) continue;
            for (int dx : new int[]{0, 9}) {
                for (int dz : new int[]{0, 9}) {
                    int x = base.getX() + dx;
                    int z = base.getZ() + dz;
                    if (!level.hasChunk(x >> 4, z >> 4)) return 0;
                    // The Hordes samples these columns with MOTION_BLOCKING.
                    // Check the same surface, including a roof above the base.
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    Vec3 point = new Vec3(x + 0.5D, y + 1.2D, z + 0.5D);
                    Vec3 toSpawn = point.subtract(observer.getEyePosition());
                    if (toSpawn.lengthSqr() < MIN_OTHER_PLAYER_DISTANCE * MIN_OTHER_PLAYER_DISTANCE)
                        return 0;
                    if (toSpawn.lengthSqr() > 160 * 160) continue;
                    // An unobstructed spawn must at least be well behind the
                    // observer's field of view. A building or hill also hides it.
                    Vec3 look = observer.getLookAngle();
                    if (look.x * toSpawn.x + look.z * toSpawn.z <= 0.0D) {
                        allCovered = false;
                        continue;
                    }
                    HitResult hit = level.clip(new ClipContext(observer.getEyePosition(), point,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, observer));
                    if (hit.getType() != HitResult.Type.BLOCK
                        || !level.getBlockState(((BlockHitResult) hit).getBlockPos()).canOcclude()) return 0;
                }
            }
        }
        return allCovered ? 2 : 1;
    }

    private record Placement(BlockPos position, long tick) {}
}
