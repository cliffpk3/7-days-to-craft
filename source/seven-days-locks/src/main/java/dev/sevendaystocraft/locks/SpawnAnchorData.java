package dev.sevendaystocraft.locks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Keeps the worldgen distance origin stable when /setworldspawn is used later. */
final class SpawnAnchorData extends SavedData {
    private static final String FILE_NAME = "seven_days_locks_spawn_anchor";
    private final BlockPos anchor;

    private SpawnAnchorData(BlockPos anchor) {
        this.anchor = anchor.immutable();
    }

    static BlockPos getOrCreate(ServerLevel overworld) {
        return overworld.getDataStorage().computeIfAbsent(
            SpawnAnchorData::load,
            () -> {
                SpawnAnchorData data = new SpawnAnchorData(overworld.getSharedSpawnPos());
                data.setDirty();
                return data;
            },
            FILE_NAME
        ).anchor;
    }

    private static SpawnAnchorData load(CompoundTag tag) {
        return new SpawnAnchorData(new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("x", anchor.getX());
        tag.putInt("y", anchor.getY());
        tag.putInt("z", anchor.getZ());
        return tag;
    }
}
