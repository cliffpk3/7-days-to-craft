package dev.sevendaystocraft.skillbridge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-side Ore Sense state consumed by the Glowing Ores compatibility mixin. */
@Mod.EventBusSubscriber(modid = SkillBridge.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OreSenseOverlay {
    public static final int LIGHT_LEVEL = 6;
    public static final int RANGE = 20;
    private static final int RANGE_SQUARED = RANGE * RANGE;
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "ores"));
    private static volatile boolean unlocked;
    private static ClientLevel trackedLevel;
    private static BlockPos trackedPosition;

    private OreSenseOverlay() {
    }

    public static void setUnlocked(boolean value) {
        if (unlocked == value) return;
        unlocked = value;
        refreshNearbyOreLight();
    }

    public static boolean isUnlocked() {
        return unlocked;
    }

    public static int lightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        if (!state.is(ORES)) return state.getLightEmission(level, pos);
        Minecraft minecraft = Minecraft.getInstance();
        if (!unlocked || minecraft.player == null || minecraft.level != level) return 0;
        return minecraft.player.blockPosition().distSqr(pos) <= RANGE_SQUARED ? LIGHT_LEVEL : 0;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            trackedLevel = null;
            trackedPosition = null;
            return;
        }
        BlockPos current = minecraft.player.blockPosition();
        if (trackedLevel != minecraft.level) {
            trackedLevel = minecraft.level;
            trackedPosition = current;
            if (unlocked) refreshSphere(current);
        } else if (!current.equals(trackedPosition)) {
            BlockPos previous = trackedPosition;
            trackedPosition = current;
            if (unlocked && previous != null) refreshBoundary(previous, current);
        }
    }

    private static void refreshNearbyOreLight() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return;
        BlockPos center = minecraft.player.blockPosition();
        trackedLevel = minecraft.level;
        trackedPosition = center;
        refreshSphere(center);
    }

    private static void refreshSphere(BlockPos center) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -RANGE; x <= RANGE; x++) {
            for (int z = -RANGE; z <= RANGE; z++) {
                for (int y = -RANGE; y <= RANGE; y++) {
                    if (x * x + y * y + z * z > RANGE_SQUARED) continue;
                    cursor.setWithOffset(center, x, y, z);
                    refreshOre(cursor);
                }
            }
        }
        minecraft.levelRenderer.allChanged();
    }

    private static void refreshBoundary(BlockPos previous, BlockPos current) {
        int minX = Math.min(previous.getX(), current.getX()) - RANGE;
        int maxX = Math.max(previous.getX(), current.getX()) + RANGE;
        int minY = Math.min(previous.getY(), current.getY()) - RANGE;
        int maxY = Math.max(previous.getY(), current.getY()) + RANGE;
        int minZ = Math.min(previous.getZ(), current.getZ()) - RANGE;
        int maxZ = Math.max(previous.getZ(), current.getZ()) + RANGE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    boolean wasNear = distanceSquared(previous, x, y, z) <= RANGE_SQUARED;
                    boolean nowNear = distanceSquared(current, x, y, z) <= RANGE_SQUARED;
                    if (wasNear == nowNear) continue;
                    cursor.set(x, y, z);
                    refreshOre(cursor);
                }
            }
        }
    }

    private static long distanceSquared(BlockPos center, int x, int y, int z) {
        long dx = (long) x - center.getX();
        long dy = (long) y - center.getY();
        long dz = (long) z - center.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static void refreshOre(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.hasChunkAt(pos)
            || !minecraft.level.getBlockState(pos).is(ORES)) return;
        minecraft.level.getChunkSource().getLightEngine().checkBlock(pos.immutable());
        minecraft.levelRenderer.setSectionDirtyWithNeighbors(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }
}
