package dev.sevendaystocraft.locks;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ThreatSense {
    private static final String SKILL_TAG = "7dtc_threat_sense";
    private static final String PROTOCOL = "2";
    private static final Gson GSON = new Gson();
    private static final Path REGISTRY = FMLPaths.GAMEDIR.get().resolve("kubejs/config/7dtc_mob_registry.json");
    private static final Map<ResourceLocation, Integer> THREAT_TIERS = new HashMap<>();
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(SevenDaysLocks.MOD_ID, "threat_sense"))
        .networkProtocolVersion(() -> PROTOCOL)
        .clientAcceptedVersions(PROTOCOL::equals)
        .serverAcceptedVersions(PROTOCOL::equals)
        .simpleChannel();
    private static boolean initialized;

    private ThreatSense() {}

    static void init() {
        if (initialized) return;
        initialized = true;
        CHANNEL.registerMessage(0, AlertPacket.class, AlertPacket::encode, AlertPacket::decode, AlertPacket::handle);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        loadRegistry();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;

        if (!player.getTags().contains(SKILL_TAG)) {
            player.getPersistentData().remove("7dtc_threat_sense_target");
            player.getPersistentData().remove("7dtc_threat_sense_aggro");
            return;
        }

        if (player.tickCount % 10 != 0) return;

        Mob aggro = player.serverLevel().getEntitiesOfClass(
            Mob.class,
            player.getBoundingBox().inflate(32.0D),
            mob -> mob.isAlive() && mob.getTarget() == player
        ).stream().min(Comparator.comparingDouble(mob -> mob.distanceToSqr(player))).orElse(null);
        var data = player.getPersistentData();
        boolean wasAggroed = data.getBoolean("7dtc_threat_sense_aggro");
        if (aggro == null) data.remove("7dtc_threat_sense_aggro");
        else {
            data.putBoolean("7dtc_threat_sense_aggro", true);
            if (!wasAggroed) {
                // Aggro Indicator itself has no audio. This adds one subtle cue at the start of an encounter.
                player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 0.20F, 0.78F);
            }
        }

        Mob threat = player.serverLevel().getEntitiesOfClass(
            Mob.class,
            player.getBoundingBox().inflate(48.0D),
            mob -> mob.isAlive() && mob.getTarget() == player && threatTier(mob) >= 3
        ).stream().max(Comparator
            .comparingInt(ThreatSense::threatTier)
            .thenComparingDouble(mob -> -mob.distanceToSqr(player)))
            .orElse(null);

        if (threat == null) {
            data.remove("7dtc_threat_sense_target");
            return;
        }

        UUID id = threat.getUUID();
        String previous = data.getString("7dtc_threat_sense_target");
        long now = player.level().getGameTime();
        if (previous.equals(id.toString()) || now < data.getLong("7dtc_threat_sense_cooldown")) return;

        int tier = threatTier(threat);
        data.putString("7dtc_threat_sense_target", id.toString());
        data.putLong("7dtc_threat_sense_cooldown", now + 100L);
        player.displayClientMessage(Component.literal(tier >= 4 ? "BOSS ALERT: " : "DANGER: ").append(threat.getDisplayName()), true);
        var advancement = player.server.getAdvancements().getAdvancement(
            new ResourceLocation("seven_days_to_craft", "training/threat_sense_alert")
        );
        if (advancement != null) {
            var progress = player.getAdvancements().getOrStartProgress(advancement);
            for (String criterion : progress.getRemainingCriteria()) player.getAdvancements().award(advancement, criterion);
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new AlertPacket(tier));
    }

    static int threatTier(LivingEntity mob) {
        if (mob.getTags().contains("7dtc_horde_boss")) return 4;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        int registered = id == null ? 0 : THREAT_TIERS.getOrDefault(id, 0);
        if (ModList.get().isLoaded("infernalmobs") && InfernalMobsCore.getMobModifiers(mob) != null) {
            return Math.max(3, registered);
        }
        return registered;
    }

    /** Null means this mob has no explicit classification; hearing may estimate one locally. */
    static Integer registeredTier(LivingEntity mob) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        return id == null ? null : THREAT_TIERS.get(id);
    }

    private static void loadRegistry() {
        THREAT_TIERS.clear();
        if (!Files.isRegularFile(REGISTRY)) return;
        try (Reader reader = Files.newBufferedReader(REGISTRY)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            JsonObject mobs = root.getAsJsonObject("mobs");
            if (mobs == null) return;
            for (Map.Entry<String, JsonElement> entry : mobs.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                JsonObject threat = entry.getValue().getAsJsonObject().getAsJsonObject("threat");
                if (threat == null || !threat.has("tier")) continue;
                int tier = Math.max(0, Math.min(4, threat.get("tier").getAsInt()));
                if (tier > 0) THREAT_TIERS.put(new ResourceLocation(entry.getKey()), tier);
            }
            System.out.println("[7DTC] Loaded threat classifications for " + THREAT_TIERS.size() + " mobs.");
        } catch (Exception error) {
            System.err.println("[7DTC] Could not load mob threat classifications: " + error.getMessage());
        }
    }

    public record AlertPacket(int tier) {
        static void encode(AlertPacket packet, FriendlyByteBuf buffer) { buffer.writeVarInt(packet.tier); }
        static AlertPacket decode(FriendlyByteBuf buffer) { return new AlertPacket(buffer.readVarInt()); }
        static void handle(AlertPacket packet, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> ClientOverlay.show(packet.tier));
            context.get().setPacketHandled(true);
        }
    }

    @Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ClientOverlay {
        private static long visibleUntil;
        private static int tier;

        private ClientOverlay() {}

        private static void show(int alertTier) {
            tier = alertTier;
            visibleUntil = System.currentTimeMillis() + 2200L;
        }

        @SubscribeEvent
        public static void render(RenderGuiEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.options.hideGui || minecraft.player == null) return;
            int width = event.getWindow().getGuiScaledWidth();
            int height = event.getWindow().getGuiScaledHeight();
            var graphics = event.getGuiGraphics();

            long remaining = visibleUntil - System.currentTimeMillis();
            if (remaining <= 0L) return;
            double fade = Math.min(1.0D, remaining / 700.0D);
            double pulse = 0.65D + 0.35D * Math.sin(System.currentTimeMillis() / 95.0D);
            int alpha = (int) ((tier >= 4 ? 105 : 72) * fade * pulse);
            int colour = (alpha << 24) | 0x00AA0000;
            int edge = tier >= 4 ? 18 : 12;
            graphics.fill(0, 0, width, edge, colour);
            graphics.fill(0, height - edge, width, height, colour);
            graphics.fill(0, edge, edge, height - edge, colour);
            graphics.fill(width - edge, edge, width, height - edge, colour);
        }
    }
}
