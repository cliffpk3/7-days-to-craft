package dev.sevendaystocraft.locks;

import dlovin.advancedcompass.AdvancedCompass;
import dlovin.advancedcompass.utils.Color;
import dlovin.advancedcompass.utils.GroupUtils;
import dlovin.advancedcompass.utils.StringUtils;
import dlovin.advancedcompass.utils.waypoints.Waypoint;
import dlovin.advancedcompass.utils.waypoints.WaypointIcon;
import dlovin.advancedcompass.utils.waypoints.WaypointJsonUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.smileycorp.hordes.common.entities.PlayerZombie;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CompassIntegration {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(SevenDaysLocks.MOD_ID, "compass_integration"))
        .networkProtocolVersion(() -> PROTOCOL)
        .clientAcceptedVersions(PROTOCOL::equals)
        .serverAcceptedVersions(PROTOCOL::equals)
        .simpleChannel();
    private static boolean initialized;

    private CompassIntegration() {}

    static void init() {
        if (initialized) return;
        initialized = true;
        CHANNEL.registerMessage(0, WorldIdentityPacket.class, WorldIdentityPacket::encode, WorldIdentityPacket::decode, WorldIdentityPacket::handle);
        CHANNEL.registerMessage(1, ZombieGravePacket.class, ZombieGravePacket::encode, ZombieGravePacket::decode, ZombieGravePacket::handle);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        String identity = Long.toUnsignedString(level.getSeed(), 16) + ":" + level.getServer().getWorldData().getLevelName();
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new WorldIdentityPacket(identity));
    }

    @SubscribeEvent
    public static void onPlayerZombieDeath(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity.level().isClientSide() || !(entity instanceof PlayerZombie<?> zombie)) return;
        Optional<UUID> ownerId = zombie.getPlayerUUID();
        if (ownerId.isEmpty() || entity.getServer() == null) return;
        ServerPlayer owner = entity.getServer().getPlayerList().getPlayer(ownerId.get());
        if (owner == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> owner), new ZombieGravePacket(
            entity.getX(), entity.getY(), entity.getZ(), entity.level().dimension().location().toString(), owner.getGameProfile().getName()
        ));
    }

    public record WorldIdentityPacket(String identity) {
        static void encode(WorldIdentityPacket packet, FriendlyByteBuf buffer) { buffer.writeUtf(packet.identity); }
        static WorldIdentityPacket decode(FriendlyByteBuf buffer) { return new WorldIdentityPacket(buffer.readUtf()); }
        static void handle(WorldIdentityPacket packet, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> Client.syncWorld(packet.identity));
            context.get().setPacketHandled(true);
        }
    }

    public record ZombieGravePacket(double x, double y, double z, String dimension, String owner) {
        static void encode(ZombieGravePacket packet, FriendlyByteBuf buffer) {
            buffer.writeDouble(packet.x);
            buffer.writeDouble(packet.y);
            buffer.writeDouble(packet.z);
            buffer.writeUtf(packet.dimension);
            buffer.writeUtf(packet.owner);
        }
        static ZombieGravePacket decode(FriendlyByteBuf buffer) {
            return new ZombieGravePacket(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readUtf(), buffer.readUtf());
        }
        static void handle(ZombieGravePacket packet, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> Client.addZombieGrave(packet));
            context.get().setPacketHandled(true);
        }
    }

    @Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Client {
        private Client() {}

        // Advanced Compass maintains a runtime set of visible mob types. Clearing
        // it before HUD rendering keeps its separate player markers intact and
        // also covers mobs introduced by new mods or regenerated config files.
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void hideMobMarkers(RenderGuiOverlayEvent.Pre event) {
            if (GroupUtils.types != null) GroupUtils.types.clear();
        }

        private static void syncWorld(String identity) {
            try {
                String serverName = serverName();
                if (serverName == null) return;
                Path marker = FMLPaths.CONFIGDIR.get().resolve("AdvancedCompass").resolve("waypoints")
                    .resolve(StringUtils.removeIllegal(serverName)).resolve("7dtc-world-id.txt");
                String previous = Files.isRegularFile(marker) ? Files.readString(marker, StandardCharsets.UTF_8).trim() : "";
                if (!identity.equals(previous)) clearDeathWaypoints(serverName);
                Files.createDirectories(marker.getParent());
                Files.writeString(marker, identity, StandardCharsets.UTF_8);
            } catch (Exception error) {
                AdvancedCompass.logger.warn("Could not synchronize death waypoints with the current 7DTC world", error);
            }
        }

        private static void clearDeathWaypoints(String serverName) {
            List<Waypoint> current = new ArrayList<>(WaypointJsonUtils.getWaypoints(serverName));
            for (Waypoint waypoint : current) {
                String name = waypoint.getName();
                if (waypoint.getIcon() != WaypointIcon.SKULL || name == null
                    || (!name.startsWith("DP(") && !name.startsWith("ZG("))) continue;
                WaypointJsonUtils.deleteWaypoint(serverName, waypoint);
                if (AdvancedCompass.getInstance().getCompassGui() != null) {
                    AdvancedCompass.getInstance().getCompassGui().removeWaypoint(waypoint);
                }
            }
        }

        private static void addZombieGrave(ZombieGravePacket packet) {
            try {
                String serverName = serverName();
                if (serverName == null) return;
                String name = "ZG(" + packet.owner + "_" + (int) packet.x + "_" + (int) packet.y + "_" + (int) packet.z + ")";
                for (Waypoint waypoint : WaypointJsonUtils.getWaypoints(serverName)) {
                    if (name.equals(waypoint.getName())) return;
                }
                Waypoint waypoint = new Waypoint(name, new Vec3(packet.x, packet.y, packet.z), packet.dimension,
                    WaypointIcon.SKULL, new Color(0xFFFFFF), 0, 0, true);
                WaypointJsonUtils.addWaypoint(serverName, waypoint, false);
                if (AdvancedCompass.getInstance().getCompassGui() != null) {
                    AdvancedCompass.getInstance().getCompassGui().addWaypoint(waypoint);
                }
            } catch (Exception error) {
                AdvancedCompass.logger.warn("Could not add the player-zombie grave waypoint", error);
            }
        }

        private static String serverName() {
            Minecraft minecraft = Minecraft.getInstance();
            ServerData server = minecraft.getCurrentServer();
            if (server != null) return StringUtils.trimPort(server.ip);
            if (minecraft.getSingleplayerServer() == null) return null;
            return StringUtils.removeIllegal(minecraft.getSingleplayerServer().getWorldData().getLevelName());
        }
    }
}
