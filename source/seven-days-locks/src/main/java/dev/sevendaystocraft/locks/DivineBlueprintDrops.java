package dev.sevendaystocraft.locks;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One blueprint roll per kill; material odds are not multiplied by weapon count. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
final class DivineBlueprintDrops {
    private static final Path CONFIG = Path.of("config", "7dtc_divine_blueprints.json");
    private static final String[] MATERIALS = {"iron", "diamond", "netherite"};
    private static volatile double[][] chances = {
        {0, 0, 0}, {0.0015, 0, 0}, {0.005, 0, 0},
        {0.008, 0.0015, 0}, {0.008, 0.0035, 0.10}
    };
    private DivineBlueprintDrops() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!Files.isRegularFile(CONFIG)) return;
        try (Reader reader = Files.newBufferedReader(CONFIG)) {
            JsonObject root = new Gson().fromJson(reader, JsonObject.class);
            double[][] next = new double[5][3];
            for (int tier = 1; tier <= 4; tier++) {
                JsonObject values = root.getAsJsonObject("tier" + tier);
                if (values == null) throw new IllegalArgumentException("Missing tier" + tier);
                double total = 0;
                for (int material = 0; material < MATERIALS.length; material++) {
                    String name = MATERIALS[material];
                    if (!values.has(name)) throw new IllegalArgumentException("Missing tier" + tier + "." + name);
                    double chance = values.get(name).getAsDouble();
                    if (!Double.isFinite(chance) || chance < 0 || chance > 1) {
                        throw new IllegalArgumentException("Invalid tier" + tier + "." + name);
                    }
                    total += chance;
                    next[tier][material] = chance;
                }
                if (total > 1) throw new IllegalArgumentException("Tier " + tier + " total chance exceeds 100%");
            }
            chances = next;
        } catch (Exception error) {
            System.err.println("[7DTC] Invalid Divine blueprint configuration: " + error.getMessage());
        }
    }

    static void apply(LivingDropsEvent event) {
        LivingEntity mob = event.getEntity();
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) return;
        if (mob.getTags().contains("7dtc_summoned")) return;
        int classifiedTier = ThreatSense.threatTier(mob);
        if (classifiedTier <= 0 && !(mob instanceof Enemy)) return;
        int tier = Math.max(1, Math.min(4, classifiedTier));
        double roll = mob.getRandom().nextDouble();
        String material = null;
        double cumulative = 0;
        for (int index = 0; index < MATERIALS.length; index++) {
            cumulative += chances[tier][index];
            if (roll < cumulative) {
                material = MATERIALS[index];
                break;
            }
        }
        if (material == null) return;
        String[] types = {"battleaxe", "cutlass", "dagger", "glaive", "hammer", "katana",
            "longsword", "rapier", "scythe", "shuriken", "sickle", "spear"};
        String weapon = material + "_" + types[mob.getRandom().nextInt(types.length)];
        Item item = SevenDaysLocks.DIVINE_BLUEPRINTS.get(weapon).get();
        mob.spawnAtLocation(WeaponBlueprintItem.rolled(item, mob.getRandom()));
    }
}
