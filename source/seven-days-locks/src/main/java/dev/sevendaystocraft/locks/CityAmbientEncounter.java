package dev.sevendaystocraft.locks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.nekoyuni.SimpleEnemyMod.entity.ai.roles.utils.UnitRole;
import net.nekoyuni.SimpleEnemyMod.entity.unit.USunitEntity;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Occasional distant militia-versus-infected encounters inside Lost Cities. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CityAmbientEncounter {
    private static final String AMBIENT_TAG = "7dtc_city_ambient_militia";
    private static final int SECTOR_SIZE = 64;
    private static final int CHANCE_PERCENT = 30;
    private static final int MAX_EVENTS_PER_CITY_PLAYER = 2;
    private static final int CITY_COOLDOWN_TICKS = 3600;
    private static final Map<Long, Long> LAST_EVENT = new HashMap<>();

    private CityAmbientEncounter() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        LAST_EVENT.clear();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
            || player.isSpectator() || player.tickCount % 100 != 0
            || !ModList.get().isLoaded("simpleenemymod") || !ModList.get().isLoaded("the_lost_city")) return;
        if (!(player.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        StructureStart city = CityRaid.cityStart(level, player.blockPosition());
        if (city == null) return;
        CityVisitData visits = CityVisitData.get(level);
        if (!visits.hasAmbush(player.getUUID(), city.getChunkPos())
            || visits.ambientCount(player.getUUID(), city.getChunkPos()) >= MAX_EVENTS_PER_CITY_PLAYER) return;

        long sector = (((long) Math.floorDiv(player.getBlockX(), SECTOR_SIZE)) << 32)
            ^ (Math.floorDiv(player.getBlockZ(), SECTOR_SIZE) & 0xffffffffL);
        if (visits.hasAmbientEvent(player.getUUID(), city.getChunkPos(), sector)) return;
        long seed = level.getSeed() ^ player.getUUID().getMostSignificantBits()
            ^ player.getUUID().getLeastSignificantBits() ^ city.getChunkPos().toLong() ^ sector;
        if (RandomSource.create(seed).nextInt(100) >= CHANCE_PERCENT) return;

        long cityKey = city.getChunkPos().toLong();
        long now = level.getGameTime();
        if (now - LAST_EVENT.getOrDefault(cityKey, Long.MIN_VALUE / 2) < CITY_COOLDOWN_TICKS) return;
        if (!level.getEntitiesOfClass(USunitEntity.class, player.getBoundingBox().inflate(96, 24, 96),
            unit -> unit.getTags().contains(AMBIENT_TAG)).isEmpty()) return;
        if (spawnDistantFirefight(level, player)) {
            visits.markAmbientEvent(player.getUUID(), city.getChunkPos(), sector);
            LAST_EVENT.put(cityKey, now);
        }
    }

    private static boolean spawnDistantFirefight(ServerLevel level, ServerPlayer player) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("simpleenemymod:usunit"));
        if (type == null) return false;
        for (int attempt = 0; attempt < 48; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            int radius = 34 + level.random.nextInt(19);
            int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * radius);
            int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * radius);
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (Math.abs(pos.getY() - player.getBlockY()) > 6 || !validStreet(level, pos)
                || !(type.create(level) instanceof USunitEntity unit)) continue;
            unit.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                level.random.nextFloat() * 360F, 0F);
            if (!level.noCollision(unit) || player.hasLineOfSight(unit)) continue;
            List<Mob> infected = level.getEntitiesOfClass(Mob.class, unit.getBoundingBox().inflate(25, 8, 25),
                mob -> mob.isAlive() && isCityZombie(mob) && unit.hasLineOfSight(mob));
            List<Zombie> added = new ArrayList<>();
            while (infected.size() + added.size() < 2) {
                Zombie zombie = spawnInfectedNear(level, unit, pos, player);
                if (zombie == null) break;
                added.add(zombie);
            }
            if (infected.size() + added.size() < 2) {
                added.forEach(Mob::discard);
                continue;
            }
            unit.addTag("7dtc_city_raid");
            unit.addTag(AMBIENT_TAG);
            unit.setRole(UnitRole.SQUAD_UNIT);
            unit.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            if (!level.addFreshEntity(unit)) {
                added.forEach(Mob::discard);
                continue;
            }
            return true;
        }
        return false;
    }

    static boolean isCityZombie(Mob mob) {
        if (!(mob instanceof Enemy)) return false;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return mob instanceof Zombie || id.getNamespace().equals("zombie_extreme")
            || (id.getNamespace().equals("born_in_chaos_v1") && id.getPath().contains("zombie"));
    }

    private static Zombie spawnInfectedNear(ServerLevel level, USunitEntity unit, BlockPos center,
                                            ServerPlayer player) {
        for (int attempt = 0; attempt < 24; attempt++) {
            int x = center.getX() + level.random.nextInt(25) - 12;
            int z = center.getZ() + level.random.nextInt(25) - 12;
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!validStreet(level, pos) || pos.distSqr(center) < 36
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < 24 * 24) continue;
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie == null) continue;
            zombie.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                level.random.nextFloat() * 360F, 0F);
            if (!level.noCollision(zombie) || !unit.hasLineOfSight(zombie)) continue;
            zombie.addTag("7dtc_city_raid");
            zombie.addTag("7dtc_city_ambient_infected");
            zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            if (level.addFreshEntity(zombie)) return zombie;
        }
        return null;
    }

    private static boolean validStreet(ServerLevel level, BlockPos pos) {
        return CityRaid.cityStart(level, pos) != null
            && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
            && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }
}
