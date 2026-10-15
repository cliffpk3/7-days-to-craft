package dev.sevendaystocraft.locks;

import com.google.gson.Gson;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** A bounded, seed-stable cave population layered over native cave spawning. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CaveEcosystems {
    private static final Path CONFIG_PATH = Path.of("config", "7dtc_cave_ecosystems.json");
    private static final TagKey<Biome> ALEX_CAVES = TagKey.create(Registries.BIOME,
        new ResourceLocation("alexscaves", "alexs_caves_biomes"));
    private static final TagKey<Biome> NO_DEFAULT_MONSTERS = TagKey.create(Registries.BIOME,
        new ResourceLocation("forge", "no_default_monsters"));
    private static final ResourceLocation DEEP_DARK = new ResourceLocation("minecraft", "deep_dark");
    private static final String SPAWN_TAG = "7dtc_ecosystem_spawn";
    private static volatile Settings settings = new Settings();
    private static final Map<Long, Long> sectorLastEncounter = new HashMap<>();

    private CaveEcosystems() {}

    private static final class Settings {
        boolean enabled = false;
        int checkIntervalTicks = 200;
        double encounterChance = 0.5;
        int sectorSize = 96;
        int minimumDistance = 32;
        int maximumDistance = 56;
        int maximumHostilesNearby = 12;
        int maximumEcosystemMobsNearby = 6;
        int searchAttempts = 28;
        int sectorCooldownTicks = 1200;
        List<Family> families = List.of();
    }

    private static final class Family {
        String id;
        int minY;
        int maxY;
        double weight;
        Map<String, Double> biomeBias = Map.of();
        List<Encounter> encounters = List.of();
    }

    private static final class Encounter {
        double weight;
        int minY;
        int maxY;
        List<String> mobs = List.of();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        sectorLastEncounter.clear();
        Settings next = new Settings();
        if (Files.isRegularFile(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                next = new Gson().fromJson(reader, Settings.class);
                validate(next);
            } catch (Exception error) {
                System.err.println("[7DTC] Cave ecosystems disabled: " + error.getMessage());
                next = new Settings();
            }
        }
        settings = next;
    }

    private static void validate(Settings config) {
        if (config == null || config.checkIntervalTicks < 20 || config.sectorSize < 32
            || config.minimumDistance < 32 || config.maximumDistance < config.minimumDistance
            || config.encounterChance < 0 || config.encounterChance > 1
            || config.maximumHostilesNearby < 1 || config.maximumEcosystemMobsNearby < 1
            || config.searchAttempts < 1 || config.sectorCooldownTicks < 0
            || config.families == null || config.families.isEmpty()) {
            throw new IllegalArgumentException("Invalid top-level settings");
        }
        for (Family family : config.families) {
            if (family.id == null || family.id.isBlank() || family.minY > family.maxY
                || family.weight <= 0 || family.encounters == null || family.encounters.isEmpty()) {
                throw new IllegalArgumentException("Invalid family: " + (family.id == null ? "<missing>" : family.id));
            }
            for (Encounter encounter : family.encounters) {
                if (encounter.weight <= 0 || encounter.minY > encounter.maxY
                    || encounter.mobs == null || encounter.mobs.isEmpty() || encounter.mobs.size() > 3) {
                    throw new IllegalArgumentException("Invalid encounter in " + family.id);
                }
                for (String id : encounter.mobs) {
                    ResourceLocation key = ResourceLocation.tryParse(id);
                    if (key == null || !ForgeRegistries.ENTITY_TYPES.containsKey(key)) {
                        throw new IllegalArgumentException("Unknown cave mob: " + id);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
            || player.isSpectator()) return;
        Settings config = settings;
        if (!config.enabled || player.tickCount % config.checkIntervalTicks != 0) return;
        ServerLevel level = player.serverLevel();
        if (level.dimension() != ServerLevel.OVERWORLD || level.getDifficulty() == Difficulty.PEACEFUL
            || player.getY() > 32 || level.canSeeSky(player.blockPosition())
            || excluded(level, player.blockPosition())
            || level.random.nextDouble() >= config.encounterChance) return;

        AABB neighborhood = player.getBoundingBox().inflate(56, 20, 56);
        List<Mob> nearby = level.getEntitiesOfClass(Mob.class, neighborhood);
        if (nearby.stream().filter(m -> m instanceof Enemy).count() >= config.maximumHostilesNearby
            || nearby.stream().filter(m -> m.getTags().contains(SPAWN_TAG)).count() >= config.maximumEcosystemMobsNearby) return;

        for (int attempt = 0; attempt < config.searchAttempts; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            int radius = config.minimumDistance + level.random.nextInt(config.maximumDistance - config.minimumDistance + 1);
            int x = player.blockPosition().getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = player.blockPosition().getZ() + (int) Math.round(Math.sin(angle) * radius);
            int y = player.blockPosition().getY() - 12 + level.random.nextInt(25);
            BlockPos pos = new BlockPos(x, y, z);
            if (player.distanceToSqr(x + 0.5, y, z + 0.5) < (double) config.minimumDistance * config.minimumDistance
                || !level.hasChunk(x >> 4, z >> 4) || !valid(level, pos)) continue;
            long sectorKey = sectorKey(pos, config.sectorSize);
            Long previous = sectorLastEncounter.get(sectorKey);
            if (previous != null && level.getGameTime() - previous < config.sectorCooldownTicks) continue;
            Family family = chooseFamily(level, pos, config);
            if (family == null) continue;
            Encounter encounter = chooseEncounter(level, family, y);
            if (encounter == null) continue;
            if (spawnEncounter(level, player, pos, encounter, config) > 0) {
                sectorLastEncounter.put(sectorKey, level.getGameTime());
                if (sectorLastEncounter.size() > 4096) sectorLastEncounter.entrySet().removeIf(
                    entry -> level.getGameTime() - entry.getValue() > config.sectorCooldownTicks);
                return;
            }
        }
    }

    private static long sectorKey(BlockPos pos, int sectorSize) {
        long x = Math.floorDiv(pos.getX(), sectorSize) & 0x3ffffffL;
        long z = Math.floorDiv(pos.getZ(), sectorSize) & 0x3ffffffL;
        long band = pos.getY() > 0 ? 0 : pos.getY() > -24 ? 1 : pos.getY() > -40 ? 2 : 3;
        return (x << 38) | (z << 12) | band;
    }

    private static Family chooseFamily(ServerLevel level, BlockPos pos, Settings config) {
        double cellX = (double) pos.getX() / config.sectorSize - 0.5;
        double cellZ = (double) pos.getZ() / config.sectorSize - 0.5;
        int baseX = (int) Math.floor(cellX);
        int baseZ = (int) Math.floor(cellZ);
        double fracX = cellX - baseX;
        double fracZ = cellZ - baseZ;
        double[] weights = {(1-fracX)*(1-fracZ), fracX*(1-fracZ), (1-fracX)*fracZ, fracX*fracZ};
        Family[] options = new Family[4];
        double total = 0;
        for (int index = 0; index < 4; index++) {
            int sx = baseX + (index & 1), sz = baseZ + (index >> 1);
            int centerX = sx * config.sectorSize + config.sectorSize / 2;
            int centerZ = sz * config.sectorSize + config.sectorSize / 2;
            BlockPos center = new BlockPos(centerX, pos.getY(), centerZ);
            if (!level.hasChunk(centerX >> 4, centerZ >> 4)) center = pos;
            if (excluded(level, center)) continue;
            ResourceLocation biome = level.getBiome(center).unwrapKey().map(key -> key.location()).orElse(null);
            options[index] = seededFamily(level.getSeed(), sx, sz, pos.getY(), biome, config);
            if (options[index] != null) total += weights[index];
        }
        if (total <= 0) return null;
        double roll = level.random.nextDouble() * total;
        for (int index = 0; index < 4; index++) {
            if (options[index] == null) continue;
            roll -= weights[index];
            if (roll < 0) return options[index];
        }
        return options[3];
    }

    private static Family seededFamily(long seed, int x, int z, int y, ResourceLocation biome, Settings config) {
        int band = y > 0 ? 0 : y > -24 ? 1 : y > -40 ? 2 : 3;
        long mixed = seed ^ (x * 341873128712L) ^ (z * 132897987541L)
            ^ (band * 42317861L) ^ (biome == null ? 0 : biome.hashCode());
        Random random = new Random(mixed);
        String biomeName = biome == null ? "" : biome.toString();
        double total = 0;
        for (Family family : config.families) total += familyWeight(family, y, biomeName);
        if (total <= 0) return null;
        double roll = random.nextDouble() * total;
        for (Family family : config.families) {
            double weight = familyWeight(family, y, biomeName);
            roll -= weight;
            if (weight > 0 && roll < 0) return family;
        }
        return null;
    }

    private static double familyWeight(Family family, int y, String biome) {
        if (y < family.minY || y > family.maxY) return 0;
        double multiplier = 1;
        if (family.biomeBias != null) {
            for (var bias : family.biomeBias.entrySet()) {
                if (biome.contains(bias.getKey())) multiplier *= bias.getValue();
            }
        }
        return Math.max(0, family.weight * multiplier);
    }

    private static Encounter chooseEncounter(ServerLevel level, Family family, int y) {
        double total = 0;
        for (Encounter encounter : family.encounters) {
            if (y >= encounter.minY && y <= encounter.maxY) total += encounter.weight;
        }
        if (total <= 0) return null;
        double roll = level.random.nextDouble() * total;
        for (Encounter encounter : family.encounters) {
            if (y < encounter.minY || y > encounter.maxY) continue;
            roll -= encounter.weight;
            if (roll < 0) return encounter;
        }
        return null;
    }

    private static int spawnEncounter(ServerLevel level, ServerPlayer player, BlockPos origin,
                                      Encounter encounter, Settings config) {
        int spawned = 0;
        for (String id : encounter.mobs) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
            if (type == null) continue;
            for (int attempt = 0; attempt < 8; attempt++) {
                BlockPos pos = origin.offset(level.random.nextInt(11) - 5, level.random.nextInt(5) - 2,
                    level.random.nextInt(11) - 5);
                if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                    || player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)
                        < (double) config.minimumDistance * config.minimumDistance
                    || !valid(level, pos) || !(type.create(level) instanceof Mob mob)) continue;
                mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
                if (!level.noCollision(mob)) continue;
                mob.addTag(SPAWN_TAG);
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
                if (level.addFreshEntity(mob)) { spawned++; break; }
            }
        }
        return spawned;
    }

    private static boolean valid(ServerLevel level, BlockPos pos) {
        if (pos.getY() < -64 || pos.getY() > 32 || excluded(level, pos) || level.canSeeSky(pos)
            || level.getBrightness(LightLayer.BLOCK, pos) != 0
            || level.getBrightness(LightLayer.SKY, pos) != 0) return false;
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
            && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    private static boolean excluded(ServerLevel level, BlockPos pos) {
        var biome = level.getBiome(pos);
        return biome.is(ALEX_CAVES) || biome.is(NO_DEFAULT_MONSTERS)
            || biome.unwrapKey().map(key -> key.location().equals(DEEP_DARK)).orElse(false);
    }
}
