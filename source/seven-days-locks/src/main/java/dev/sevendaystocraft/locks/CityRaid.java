package dev.sevendaystocraft.locks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/** Infected city patrols. The police station's militia is supplied by its template spawners. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CityRaid {
    private static final TagKey<Structure> CITIES = TagKey.create(Registries.STRUCTURE,
        new ResourceLocation(SevenDaysLocks.MOD_ID, "city_raids"));
    private static final String[] COMMON = {"infected", "crawler", "runner", "clicker", "revived", "infected_police"};
    private static final String[] SPECIAL = {"boomer", "explosive_infected", "infected_military", "infected_hazmat", "faceless", "divided", "inflated"};
    private static final String[] BOSS = {"infected_juggernaut", "demolisher", "chainsaw", "night_hunter", "rat_king", "zero_patient"};
    private static final String[] BLUEPRINTS = {"tacz_glock_17", "tacz_db_short", "tacz_hk_mp5a5", "tacz_m870", "tacz_m4a1", "tacz_ak47", "tacz_m700", "tacz_hk416d"};
    private static final String[] ATTACHMENTS = {"scope_retro_2x", "scope_acog_ta31", "muzzle_compensator_trident", "grip_vertical_military", "extended_mag_1", "laser_compact"};
    private static final String[] ARMOR = {"fracturepoint:insurgency_commander_helmet", "fracturepoint:insurgency_commander_chestplate",
        "fracturepoint:insurgency_commander_leggings", "fracturepoint:insurgency_commander_boots",
        "fracturepoint:fsb_squad_leader_helmet", "fracturepoint:fsb_squad_leader_chestplate",
        "fracturepoint:fsb_leggings", "fracturepoint:fsb_boots"};
    private static final Map<Long, Long> LAST_PATROL = new HashMap<>();
    private static final int PATROL_INTERVAL = 400;
    private static final int LOCAL_CAP = 26;

    private CityRaid() {}

    private static boolean cityAt(ServerLevel level, BlockPos pos) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, CITIES);
        return start != null && start.isValid();
    }

    private static String entityId(Mob mob) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        return id == null ? "" : id.toString();
    }

    private static boolean cityMob(Mob mob) {
        String id = entityId(mob);
        return id.startsWith("zombie_extreme:") || id.startsWith("simpleenemymod:");
    }

    @SubscribeEvent
    public static void onNaturalSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        Mob mob = event.getEntity();
        MobSpawnType type = event.getSpawnType();
        if (cityMob(mob)) {
            // The city garrison comes from its own building spawners. Random
            // Simple Enemy events would mix armies inside the same building.
            if (entityId(mob).startsWith("simpleenemymod:")
                && type != MobSpawnType.SPAWNER && type != MobSpawnType.COMMAND
                && type != MobSpawnType.SPAWN_EGG) {
                event.setCanceled(true);
                return;
            }
            if (!mob.getTags().contains("7dtc_city_raid")
                && !mob.getTags().contains("7dtc_allow_test_spawn")
                && type != MobSpawnType.COMMAND && type != MobSpawnType.SPAWN_EGG
                && (!level.dimension().equals(Level.OVERWORLD) || !cityAt(level, mob.blockPosition()))) {
                event.setCanceled(true);
            }
            return;
        }
        if (!(mob instanceof Enemy)) return;
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION && type != MobSpawnType.SPAWNER) return;
        if (!cityAt(level, mob.blockPosition())) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 200 != 0) return;
        if (!ModList.get().isLoaded("the_lost_city") || !ModList.get().isLoaded("zombie_extreme")) return;
        if (!(player.level() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) return;
        if (!cityAt(level, player.blockPosition())) return;
        long region = (((long) (player.blockPosition().getX() >> 5)) << 32) ^ ((player.blockPosition().getZ() >> 5) & 0xffffffffL);
        long now = level.getGameTime();
        if (now - LAST_PATROL.getOrDefault(region, Long.MIN_VALUE / 2) < PATROL_INTERVAL) return;
        int nearby = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48, 16, 48),
            mob -> cityMob(mob) || mob.getTags().contains("7dtc_city_raid")).size();
        if (nearby >= LOCAL_CAP) return;
        LAST_PATROL.put(region, now);
        int members = Math.min(4 + level.random.nextInt(2), LOCAL_CAP - nearby);
        for (int i = 0; i < members; i++) {
            int roll = level.random.nextInt(100);
            String id = roll < 25 ? (level.random.nextInt(4) == 0 ? "minecraft:husk" : "minecraft:zombie")
                : "zombie_extreme:" + (roll < 78 ? COMMON[level.random.nextInt(COMMON.length)]
                    : roll < 97 ? SPECIAL[level.random.nextInt(SPECIAL.length)]
                    : BOSS[level.random.nextInt(BOSS.length)]);
            spawnNear(level, player, id);
        }
    }

    private static void spawnNear(ServerLevel level, ServerPlayer player, String id) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            int distance = 16 + level.random.nextInt(23);
            int x = player.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = player.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, player.getBlockY(), z))) continue;
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (Math.abs(pos.getY() - player.getBlockY()) > 5 || !cityAt(level, pos)) continue;
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(id));
            if (!(type.create(level) instanceof Mob mob)) return;
            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0F);
            mob.addTag("7dtc_city_raid");
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            level.addFreshEntity(mob);
            return;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level) || !cityMob(mob)) return;
        // Keep the city's reward ladder under 7DTC control; later normal-priority
        // handlers may add existing elite/key/Divine rewards.
        event.getDrops().clear();
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) return;
        boolean militia = entityId(mob).startsWith("simpleenemymod:");
        String path = entityId(mob);
        boolean major = path.contains("juggernaut") || path.contains("demolisher") || path.contains("night_hunter")
            || path.contains("zero_patient") || path.contains("rat_king") || path.contains("chainsaw");
        if (!militia) {
            mob.spawnAtLocation(new ItemStack(Items.ROTTEN_FLESH, major ? 3 : 1));
        }
        if (militia && level.random.nextDouble() < 0.035 || major && level.random.nextDouble() < 0.04) {
            String weapon = BLUEPRINTS[level.random.nextInt(BLUEPRINTS.length)];
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation("seven_days_locks:blueprint_" + weapon));
            if (item != Items.AIR) mob.spawnAtLocation(WeaponBlueprintItem.rolled(item, level.random));
        }
        if (militia && level.random.nextDouble() < 0.01) {
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(ARMOR[level.random.nextInt(ARMOR.length)]));
            if (item != Items.AIR) mob.spawnAtLocation(new ItemStack(item));
        }
        if (militia && level.random.nextDouble() < 0.01) {
            ItemStack attachment = TaczItems.attachment(ATTACHMENTS[level.random.nextInt(ATTACHMENTS.length)]);
            if (!attachment.isEmpty()) mob.spawnAtLocation(attachment);
        }
    }
}
