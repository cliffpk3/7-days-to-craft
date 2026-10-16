package dev.sevendaystocraft.locks;

import com.google.gson.Gson;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** One Rocky Roller encounter per nearby cave area during a five-minute window. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RockyRollerCooldown {
    private static final ResourceLocation ROCKY_ROLLER = new ResourceLocation("alexsmobs", "rocky_roller");
    private static final Path CONFIG = Path.of("config", "7dtc_rocky_roller_cooldown.json");
    private static final Map<ResourceKey<Level>, List<SpawnRecord>> RECENT = new HashMap<>();
    private static Settings settings = new Settings();

    private RockyRollerCooldown() {}

    private static final class Settings {
        int cooldownTicks = 6000;
        int horizontalRadius = 96;
        int verticalRadius = 32;
    }

    private record SpawnRecord(BlockPos pos, long tick) {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        RECENT.clear();
        Settings next = new Settings();
        if (Files.isRegularFile(CONFIG)) {
            try (Reader reader = Files.newBufferedReader(CONFIG)) {
                next = new Gson().fromJson(reader, Settings.class);
                if (next == null || next.cooldownTicks < 0 || next.horizontalRadius < 1
                    || next.verticalRadius < 1) throw new IllegalArgumentException("Invalid cooldown values");
            } catch (Exception error) {
                System.err.println("[7DTC] Invalid Rocky Roller cooldown configuration: " + error.getMessage());
                next = new Settings();
            }
        }
        settings = next;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
            || !ROCKY_ROLLER.equals(ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()))
            || event.getEntity().getTags().contains("7dtc_summoned")) return;

        Settings config = settings;
        long now = level.getGameTime();
        BlockPos position = event.getEntity().blockPosition();
        List<SpawnRecord> recent = RECENT.computeIfAbsent(level.dimension(), ignored -> new ArrayList<>());
        recent.removeIf(record -> now < record.tick || now - record.tick >= config.cooldownTicks);

        // Loaded entities must remain in the world. They still reserve their
        // surrounding area so another Rocky Roller cannot appear immediately.
        if (!event.loadedFromDisk()) {
            long radiusSquared = (long) config.horizontalRadius * config.horizontalRadius;
            for (SpawnRecord record : recent) {
                long dx = (long) position.getX() - record.pos.getX();
                long dz = (long) position.getZ() - record.pos.getZ();
                if (Math.abs(position.getY() - record.pos.getY()) <= config.verticalRadius
                    && dx * dx + dz * dz <= radiusSquared) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
        recent.add(new SpawnRecord(position.immutable(), now));
    }
}
