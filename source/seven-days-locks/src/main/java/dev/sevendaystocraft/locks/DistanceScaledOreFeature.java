package dev.sevendaystocraft.locks;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Scales only the ore feature's size; its placement attempts and height ranges stay intact. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DistanceScaledOreFeature extends Feature<OreConfiguration> {
    private static final Path CONFIG = Path.of("config", "7dtc_iron_distance.json");
    private static final Gson GSON = new Gson();
    private static volatile Settings settings = loadSettings();
    private static volatile BlockPos spawnAnchor;

    private record Settings(int[] maximumDistances, double[] sizeMultipliers, int minimumSize) {
        static Settings defaults() {
            return new Settings(new int[] {512, 1024, 1536}, new double[] {0.50, 0.65, 0.80, 1.00}, 1);
        }
    }

    public DistanceScaledOreFeature(Codec<OreConfiguration> codec) { super(codec); }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        settings = loadSettings();
        spawnAnchor = SpawnAnchorData.getOrCreate(event.getServer().overworld());
    }

    private static Settings loadSettings() {
        Settings fallback = Settings.defaults();
        if (!Files.isRegularFile(CONFIG)) return fallback;
        try (Reader reader = Files.newBufferedReader(CONFIG)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            JsonArray distances = root.getAsJsonArray("maximumDistances");
            JsonArray multipliers = root.getAsJsonArray("sizeMultipliers");
            if (distances.size() != 3 || multipliers.size() != 4) throw new IllegalArgumentException("Expected three distances and four multipliers");
            int[] limits = new int[3];
            double[] scales = new double[4];
            for (int i = 0; i < 3; i++) {
                limits[i] = distances.get(i).getAsInt();
                if (limits[i] <= 0 || (i > 0 && limits[i] <= limits[i - 1])) throw new IllegalArgumentException("Distances must increase");
            }
            for (int i = 0; i < 4; i++) {
                scales[i] = multipliers.get(i).getAsDouble();
                if (!Double.isFinite(scales[i]) || scales[i] <= 0 || scales[i] > 1) throw new IllegalArgumentException("Multipliers must be between zero and one");
            }
            int minimum = root.has("minimumSize") ? root.get("minimumSize").getAsInt() : 1;
            if (minimum < 1) throw new IllegalArgumentException("minimumSize must be positive");
            return new Settings(limits, scales, minimum);
        } catch (Exception error) {
            System.err.println("[7DTC] Invalid iron distance configuration; using defaults: " + error.getMessage());
            return fallback;
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<OreConfiguration> context) {
        OreConfiguration original = context.config();
        Settings active = settings;
        BlockPos spawn = spawnAnchor != null ? spawnAnchor : context.level().getLevel().getSharedSpawnPos();
        BlockPos origin = context.origin();
        long dx = (long) origin.getX() - spawn.getX();
        long dz = (long) origin.getZ() - spawn.getZ();
        long squared = dx * dx + dz * dz;
        int band = 0;
        while (band < active.maximumDistances.length
            && squared > (long) active.maximumDistances[band] * active.maximumDistances[band]) band++;

        double expected = original.size * active.sizeMultipliers[band];
        int size = (int) Math.floor(expected);
        if (context.random().nextDouble() < expected - size) size++;
        size = Math.max(active.minimumSize, size);
        OreConfiguration scaled = new OreConfiguration(original.targetStates, size, original.discardChanceOnAirExposure);
        return Feature.ORE.place(new FeaturePlaceContext<>(context.topFeature(), context.level(),
            context.chunkGenerator(), context.random(), origin, scaled));
    }
}
