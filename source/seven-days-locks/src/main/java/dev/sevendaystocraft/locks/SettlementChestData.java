package dev.sevendaystocraft.locks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class SettlementChestData extends SavedData {
    private static final String FILE_NAME = "seven_days_locks_settlement_chests";
    private final Map<String, Set<Long>> freeChests = new HashMap<>();
    private final Map<String, Long> specialChests = new HashMap<>();

    static SettlementChestData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SettlementChestData::load, SettlementChestData::new, FILE_NAME);
    }

    static SettlementChestData load(CompoundTag tag) {
        SettlementChestData data = new SettlementChestData();
        ListTag settlements = tag.getList("settlements", Tag.TAG_COMPOUND);
        for (int i = 0; i < settlements.size(); i++) {
            CompoundTag entry = settlements.getCompound(i);
            Set<Long> positions = new HashSet<>();
            for (long position : entry.getLongArray("positions")) positions.add(position);
            if (!entry.getString("key").isBlank()) data.freeChests.put(entry.getString("key"), positions);
        }
        ListTag specials = tag.getList("special_chests", Tag.TAG_COMPOUND);
        for (int i = 0; i < specials.size(); i++) {
            CompoundTag entry = specials.getCompound(i);
            String key = entry.getString("key");
            if (!key.isBlank() && entry.contains("position", Tag.TAG_LONG)) {
                data.specialChests.put(key, entry.getLong("position"));
            }
        }
        return data;
    }

    boolean claim(String settlementKey, long chestKey, int limit) {
        Set<Long> positions = freeChests.computeIfAbsent(settlementKey, ignored -> new HashSet<>());
        if (positions.contains(chestKey)) return true;
        if (positions.size() >= limit) return false;
        positions.add(chestKey);
        setDirty();
        return true;
    }

    /**
     * Selects at most one physical chest (a double chest counts as one) for a
     * settlement. Returning true for the already selected position lets both
     * halves of a double chest receive the same tier when they load separately.
     */
    boolean claimSpecial(String settlementKey, long chestKey) {
        Long selected = specialChests.get(settlementKey);
        if (selected != null) return selected == chestKey;
        specialChests.put(settlementKey, chestKey);
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag settlements = new ListTag();
        freeChests.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            CompoundTag saved = new CompoundTag();
            saved.putString("key", entry.getKey());
            saved.putLongArray("positions", entry.getValue().stream().sorted().mapToLong(Long::longValue).toArray());
            settlements.add(saved);
        });
        tag.put("settlements", settlements);
        ListTag specials = new ListTag();
        specialChests.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            CompoundTag saved = new CompoundTag();
            saved.putString("key", entry.getKey());
            saved.putLong("position", entry.getValue());
            specials.add(saved);
        });
        tag.put("special_chests", specials);
        return tag;
    }
}
