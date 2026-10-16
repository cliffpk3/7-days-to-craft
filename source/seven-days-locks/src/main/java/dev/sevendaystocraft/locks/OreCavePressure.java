package dev.sevendaystocraft.locks;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Adds one bounded cave encounter near an ore-rich, unlit player area. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OreCavePressure {
    private static final TagKey<net.minecraft.world.level.block.Block> ORES =
        TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "ores"));
    private static final TagKey<Biome> ALEX_CAVES =
        TagKey.create(Registries.BIOME, new ResourceLocation("alexscaves", "alexs_caves_biomes"));
    private static final TagKey<Biome> NO_DEFAULT_MONSTERS =
        TagKey.create(Registries.BIOME, new ResourceLocation("forge", "no_default_monsters"));
    private static final ResourceLocation DEEP_DARK = new ResourceLocation("minecraft", "deep_dark");

    private OreCavePressure() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
            || player.tickCount % 200 != 0 || player.isSpectator()) return;
        ServerLevel level = player.serverLevel();
        if (level.dimension() != ServerLevel.OVERWORLD || level.getDifficulty() == Difficulty.PEACEFUL
            || player.getY() > 32 || level.canSeeSky(player.blockPosition())
            || excluded(level, player.blockPosition())) return;

        // Exposed ore, rather than buried veins, describes the visible reward in this cave.
        int ores = countExposedOre(level, player.blockPosition());
        if (ores < 3 || level.random.nextFloat() >= Math.min(0.45F, ores * 0.045F)) return;
        AABB neighborhood = player.getBoundingBox().inflate(56.0D, 20.0D, 56.0D);
        if (level.getEntitiesOfClass(Mob.class, neighborhood, mob -> mob instanceof Enemy).size() >= 10) return;

        for (int attempt = 0; attempt < 28; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int radius = 32 + level.random.nextInt(17);
            int x = player.blockPosition().getX() + (int)Math.round(Math.cos(angle) * radius);
            int z = player.blockPosition().getZ() + (int)Math.round(Math.sin(angle) * radius);
            int y = player.blockPosition().getY() - 8 + level.random.nextInt(17);
            BlockPos pos = new BlockPos(x, y, z);
            if (player.distanceToSqr(x + 0.5D, y, z + 0.5D) < 32.0D * 32.0D
                || !level.hasChunk(x >> 4, z >> 4) || !validPosition(level, pos)) continue;

            EntityType<?> type = chooseMob(level, y);
            if (type == null || !(type.create(level) instanceof Mob mob)) return;
            mob.moveTo(x + 0.5D, y, z + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(mob)) continue;
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
            level.addFreshEntity(mob);
            return;
        }
    }

    private static int countExposedOre(ServerLevel level, BlockPos center) {
        int count = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -8; dx <= 8; dx++) for (int dy = -4; dy <= 4; dy++) for (int dz = -8; dz <= 8; dz++) {
            pos.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
            if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4) || !level.getBlockState(pos).is(ORES)) continue;
            boolean exposed = false;
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (level.hasChunk(neighbor.getX() >> 4, neighbor.getZ() >> 4)
                    && level.getBlockState(neighbor).isAir()) { exposed = true; break; }
            }
            if (exposed && ++count >= 10) return count;
        }
        return count;
    }

    private static boolean validPosition(ServerLevel level, BlockPos pos) {
        if (pos.getY() < -64 || pos.getY() > 32 || excluded(level, pos)
            || level.canSeeSky(pos) || level.getBrightness(LightLayer.BLOCK, pos) != 0
            || level.getBrightness(LightLayer.SKY, pos) != 0) return false;
        BlockState floor = level.getBlockState(pos.below());
        return floor.isFaceSturdy(level, pos.below(), Direction.UP)
            && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    private static boolean excluded(ServerLevel level, BlockPos pos) {
        var biome = level.getBiome(pos);
        return biome.is(ALEX_CAVES) || biome.is(NO_DEFAULT_MONSTERS)
            || biome.unwrapKey().map(key -> key.location().equals(DEEP_DARK)).orElse(false);
    }

    private static EntityType<?> chooseMob(ServerLevel level, int y) {
        String id;
        int roll = level.random.nextInt(100);
        // Centipedes are a rare deep-cave encounter; Rocky Rollers remain
        // recognizable cave threats, but occur a little less often.
        if (y <= -57) id = roll < 30 ? "alexsmobs:centipede_head" : roll < 60 ? "realmrpg_demons:imp_guard" : "realmrpg_demons:demon";
        else if (y <= -41) id = roll < 65 ? "realmrpg_demons:imp_guard" : roll < 80 ? "alexsmobs:centipede_head" : "realmrpg_demons:demon";
        else if (y <= -32) id = roll < 52 ? "realmrpg_demons:imp" : roll < 80 ? "alexsmobs:rocky_roller" : "realmrpg_demons:demon";
        else if (y <= -25) id = roll < 56 ? "realmrpg_demons:imp" : roll < 80 ? "alexsmobs:rocky_roller" : "minecraft:cave_spider";
        else if (y <= -16) id = roll < 50 ? "realmrpg_demons:chort" : roll < 80 ? "realmrpg_demons:imp" : "alexsmobs:rocky_roller";
        else if (y <= -8) id = roll < 70 ? "realmrpg_demons:chort" : roll < 90 ? "alexsmobs:rocky_roller" : "realmrpg_demons:imp";
        else if (y <= 16) id = roll < 55 ? "realmrpg_demons:chort" : roll < 75 ? "alexsmobs:rocky_roller" : "minecraft:zombie";
        else id = roll < 45 ? "alexsmobs:rocky_roller" : "minecraft:zombie";
        return ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
    }
}
