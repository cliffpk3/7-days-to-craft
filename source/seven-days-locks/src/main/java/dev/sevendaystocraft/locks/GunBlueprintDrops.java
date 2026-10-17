package dev.sevendaystocraft.locks;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One roll per player kill, then one blueprint from that threat tier's pool. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
final class GunBlueprintDrops {
    private static final Path CONFIG = Path.of("config", "7dtc_gun_blueprints.json");
    private static final Gson GSON = new Gson();
    private static double[] chances = {0.0, 0.0025, 0.005, 0.01, 0.015};
    private static String[][] pools = {
        {},
        {"tacz_glock_17", "tacz_m1911", "tacz_db_short"},
        {"tacz_p320", "tacz_hk_mp5a5", "tacz_uzi", "tacz_m870", "tacz_sks_tactical"},
        {"tacz_m4a1", "tacz_ak47", "tacz_scar_l", "tacz_m1014", "tacz_m700", "tacz_m249"},
        {"tacz_deagle", "tacz_hk416d", "tacz_rpk", "tacz_m107"}
    };

    private GunBlueprintDrops() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!Files.isRegularFile(CONFIG)) return;
        try (Reader reader = Files.newBufferedReader(CONFIG)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            double[] nextChances = chances.clone();
            String[][] nextPools = pools.clone();
            for (int tier = 1; tier <= 4; tier++) {
                JsonObject setting = root.getAsJsonObject("tier" + tier);
                if (setting == null) continue;
                if (setting.has("chance")) {
                    double chance = setting.get("chance").getAsDouble();
                    if (!Double.isFinite(chance) || chance < 0 || chance > 1) {
                        throw new IllegalArgumentException("Invalid tier" + tier + " chance");
                    }
                    nextChances[tier] = chance;
                }
                if (setting.has("blueprints")) {
                    JsonArray entries = setting.getAsJsonArray("blueprints");
                    String[] pool = new String[entries.size()];
                    for (int index = 0; index < entries.size(); index++) {
                        pool[index] = entries.get(index).getAsString();
                        if (!SevenDaysLocks.GUN_BLUEPRINTS.containsKey(pool[index])) {
                            throw new IllegalArgumentException("Unknown gun blueprint: " + pool[index]);
                        }
                    }
                    nextPools[tier] = pool;
                }
            }
            chances = nextChances;
            pools = nextPools;
        } catch (Exception error) {
            System.err.println("[7DTC] Invalid gun blueprint configuration: " + error.getMessage());
        }
    }

    static void apply(LivingDropsEvent event) {
        LivingEntity mob = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) return;
        if (mob.getTags().contains("7dtc_summoned")) return;
        int classifiedTier = ThreatSense.threatTier(mob);
        if (classifiedTier <= 0 && !(mob instanceof Enemy)) return;
        int tier = Math.max(1, Math.min(4, classifiedTier));
        String[] pool = pools[tier];
        if (pool.length == 0 || mob.getRandom().nextDouble() >= chances[tier]) return;
        String id = "seven_days_locks:blueprint_" + pool[mob.getRandom().nextInt(pool.length)];
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
        if (item != Items.AIR) mob.spawnAtLocation(WeaponBlueprintItem.rolled(item, mob.getRandom()));
    }
}
