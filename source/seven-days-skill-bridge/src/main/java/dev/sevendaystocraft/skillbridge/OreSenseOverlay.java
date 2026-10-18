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
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-side Ore Sense state consumed by the Glowing Ores compatibility mixin. */
@Mod.EventBusSubscriber(modid = SkillBridge.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OreSenseOverlay {
    // Minecraft light levels are integers; 6 * 1.30 = 7.8, rounded to 8.
    public static final int LIGHT_LEVEL = 8;
    private static final int BASE_RANGE = 20;
    private static final int EXTENDED_RANGE = Integer.MAX_VALUE;
    private static final int SECTIONS_PER_TICK = 12;
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "ores"));
    private static volatile int range;
    private static ClientLevel trackedLevel;
    private static BlockPos trackedPosition;
    private static final java.util.ArrayDeque<BlockPos> SECTION_QUEUE = new java.util.ArrayDeque<>();

    private OreSenseOverlay() {
    }

    public static void setRange(int requested) {
        int next = requested == EXTENDED_RANGE ? EXTENDED_RANGE : requested >= BASE_RANGE ? BASE_RANGE : 0;
        if (range == next) return;
        int previous = range;
        range = next;
        if (previous == EXTENDED_RANGE || next == EXTENDED_RANGE) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.level != null) {
                scheduleLoadedOreSections();
            }
        } else refreshNearbyOreLight();
    }

    public static boolean isUnlocked() {
        return range > 0;
    }

    public static int lightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        if (!state.is(ORES)) return state.getLightEmission(level, pos);
        Minecraft minecraft = Minecraft.getInstance();
        if (range == 0 || minecraft.player == null || minecraft.level == null) return 0;
        // ChunkAccess scans palettes with BlockPos.ZERO before visiting real
        // positions. Report a possible source there, then enforce the range
        // for each actual ore block. ClientLevel calls use the same limit.
        if (level instanceof ChunkAccess) {
            if (pos == BlockPos.ZERO) return LIGHT_LEVEL;
        } else if (minecraft.level != level) {
            return 0;
        }
        return range == EXTENDED_RANGE || minecraft.player.blockPosition().distSqr(pos) <= (long) range * range
            ? LIGHT_LEVEL : 0;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            trackedLevel = null;
            trackedPosition = null;
            SECTION_QUEUE.clear();
            return;
        }
        BlockPos current = minecraft.player.blockPosition();
        if (trackedLevel != minecraft.level) {
            trackedLevel = minecraft.level;
            trackedPosition = current;
            if (range == EXTENDED_RANGE) {
                scheduleLoadedOreSections();
            } else if (range > 0) refreshSphere(current);
        } else if (!current.equals(trackedPosition)) {
            BlockPos previous = trackedPosition;
            trackedPosition = current;
            if (range == BASE_RANGE && previous != null) refreshBoundary(previous, current);
        }
        if (range == EXTENDED_RANGE) {
            scanSections();
        } else if (!SECTION_QUEUE.isEmpty()) scanSections(); // Clear stale level-II lights after a reset.
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
        for (int x = -BASE_RANGE; x <= BASE_RANGE; x++) {
            for (int z = -BASE_RANGE; z <= BASE_RANGE; z++) {
                for (int y = -BASE_RANGE; y <= BASE_RANGE; y++) {
                    if (x * x + y * y + z * z > BASE_RANGE * BASE_RANGE) continue;
                    cursor.setWithOffset(center, x, y, z);
                    refreshOre(cursor);
                }
            }
        }
        minecraft.levelRenderer.allChanged();
    }

    private static void refreshBoundary(BlockPos previous, BlockPos current) {
        int minX = Math.min(previous.getX(), current.getX()) - BASE_RANGE;
        int maxX = Math.max(previous.getX(), current.getX()) + BASE_RANGE;
        int minY = Math.min(previous.getY(), current.getY()) - BASE_RANGE;
        int maxY = Math.max(previous.getY(), current.getY()) + BASE_RANGE;
        int minZ = Math.min(previous.getZ(), current.getZ()) - BASE_RANGE;
        int maxZ = Math.max(previous.getZ(), current.getZ()) + BASE_RANGE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    boolean wasNear = distanceSquared(previous, x, y, z) <= BASE_RANGE * BASE_RANGE;
                    boolean nowNear = distanceSquared(current, x, y, z) <= BASE_RANGE * BASE_RANGE;
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

    private static void scheduleLoadedOreSections() {
        Minecraft minecraft = Minecraft.getInstance();
        SECTION_QUEUE.clear();
        if (minecraft.level == null || minecraft.player == null) return;
        BlockPos center = minecraft.player.blockPosition();
        int renderChunks = minecraft.options.renderDistance().get();
        java.util.ArrayList<BlockPos> origins = new java.util.ArrayList<>();
        for (int cx = (center.getX() >> 4) - renderChunks; cx <= (center.getX() >> 4) + renderChunks; cx++) {
            for (int cz = (center.getZ() >> 4) - renderChunks; cz <= (center.getZ() >> 4) + renderChunks; cz++) {
                if (!minecraft.level.hasChunk(cx, cz)) continue;
                for (int sy = minecraft.level.getMinSection(); sy < minecraft.level.getMaxSection(); sy++)
                    origins.add(new BlockPos(cx << 4, sy << 4, cz << 4));
            }
        }
        origins.sort(java.util.Comparator.comparingDouble(origin -> origin.distSqr(center)));
        SECTION_QUEUE.addAll(origins);
    }

    private static void scanSections() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        for (int budget = 0; budget < SECTIONS_PER_TICK && !SECTION_QUEUE.isEmpty(); budget++) {
            BlockPos origin = SECTION_QUEUE.removeFirst();
            if (!minecraft.level.hasChunk(origin.getX() >> 4, origin.getZ() >> 4)) continue;
            LevelChunk chunk = minecraft.level.getChunk(origin.getX() >> 4, origin.getZ() >> 4);
            int index = minecraft.level.getSectionIndex(origin.getY());
            if (index < 0 || index >= chunk.getSections().length) continue;
            LevelChunkSection section = chunk.getSections()[index];
            if (section == null || section.hasOnlyAir() || !section.getStates().maybeHas(state -> state.is(ORES))) continue;
            boolean changed = false;
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                if (!section.getBlockState(x, y, z).is(ORES)) continue;
                BlockPos ore = origin.offset(x, y, z);
                minecraft.level.getChunkSource().getLightEngine().checkBlock(ore);
                changed = true;
            }
            if (changed) minecraft.levelRenderer.setSectionDirtyWithNeighbors(
                origin.getX() >> 4, origin.getY() >> 4, origin.getZ() >> 4);
        }
    }

    private static void refreshOre(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.hasChunkAt(pos)
            || !minecraft.level.getBlockState(pos).is(ORES)) return;
        minecraft.level.getChunkSource().getLightEngine().checkBlock(pos.immutable());
        minecraft.levelRenderer.setSectionDirtyWithNeighbors(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }
}
