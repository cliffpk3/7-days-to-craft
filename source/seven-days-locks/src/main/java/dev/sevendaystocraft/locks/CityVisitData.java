package dev.sevendaystocraft.locks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** One city-entry ambush per player and Lost City structure start, persisted with the world. */
final class CityVisitData extends SavedData {
    private static final String FILE_NAME = "seven_days_locks_city_visits";
    private final Set<String> ambushed = new HashSet<>();
    private final Set<String> ambientEvents = new HashSet<>();
    private final Map<String, Integer> ambientCounts = new HashMap<>();

    static CityVisitData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(CityVisitData::load, CityVisitData::new, FILE_NAME);
    }

    static CityVisitData load(CompoundTag tag) {
        CityVisitData data = new CityVisitData();
        ListTag list = tag.getList("ambushed", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) data.ambushed.add(list.getString(i));
        ListTag ambient = tag.getList("ambient_events", Tag.TAG_STRING);
        for (int i = 0; i < ambient.size(); i++) {
            String key = ambient.getString(i);
            if (data.ambientEvents.add(key)) {
                int split = key.lastIndexOf(':');
                if (split > 0) data.ambientCounts.merge(key.substring(0, split), 1, Integer::sum);
            }
        }
        return data;
    }

    boolean hasAmbush(UUID player, ChunkPos city) {
        return ambushed.contains(key(player, city));
    }

    void markAmbushed(UUID player, ChunkPos city) {
        if (ambushed.add(key(player, city))) setDirty();
    }

    private static String key(UUID player, ChunkPos city) {
        return player + ":" + city.x + ":" + city.z;
    }

    boolean hasAmbientEvent(UUID player, ChunkPos city, long sector) {
        return ambientEvents.contains(key(player, city) + ":" + sector);
    }

    int ambientCount(UUID player, ChunkPos city) {
        return ambientCounts.getOrDefault(key(player, city), 0);
    }

    void markAmbientEvent(UUID player, ChunkPos city, long sector) {
        String cityKey = key(player, city);
        if (ambientEvents.add(cityKey + ":" + sector)) {
            ambientCounts.merge(cityKey, 1, Integer::sum);
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        ambushed.stream().sorted().map(StringTag::valueOf).forEach(list::add);
        tag.put("ambushed", list);
        ListTag ambient = new ListTag();
        ambientEvents.stream().sorted().map(StringTag::valueOf).forEach(ambient::add);
        tag.put("ambient_events", ambient);
        return tag;
    }
}
