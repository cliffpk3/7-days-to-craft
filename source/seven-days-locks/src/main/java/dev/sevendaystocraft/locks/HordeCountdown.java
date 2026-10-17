package dev.sevendaystocraft.locks;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.smileycorp.hordes.hordeevent.capability.HordeEvent;
import net.smileycorp.hordes.hordeevent.capability.HordeSavedData;
import net.smileycorp.hordes.config.HordeEventConfig;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HordeCountdown {
    private static final Map<UUID, Snapshot> LAST_ANNOUNCEMENT = new ConcurrentHashMap<>();
    private static final String WORLD_CALENDAR_MIGRATED = "7dtc_world_horde_calendar_v1";
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(SevenDaysLocks.MOD_ID, "horde_countdown"))
        .networkProtocolVersion(() -> PROTOCOL)
        .clientAcceptedVersions(PROTOCOL::equals)
        .serverAcceptedVersions(PROTOCOL::equals)
        .simpleChannel();
    private static boolean initialized;

    private HordeCountdown() {}

    static void init() {
        if (initialized) return;
        initialized = true;
        CHANNEL.registerMessage(0, CountdownPacket.class, CountdownPacket::encode,
            CountdownPacket::decode, CountdownPacket::handle);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LAST_ANNOUNCEMENT.remove(player.getUUID());
            migrateSchedule(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        ServerLevel level = player.server.getLevel(ServerLevel.OVERWORLD);
        if (level == null) return;

        HordeEvent horde = HordeSavedData.getData(level).getEvent(player);
        migrateSchedule(player);
        // Tick 0 is 06:00 in Minecraft. The +6000 shift advances the
        // calendar when the visible clock reaches 00:00.
        int currentDay = (int) Math.floorDiv(level.getDayTime() + 6000L, 24000L) + 1;
        int nextDay = horde.getNextDay();
        boolean active = horde.isActive();
        long hordeStart = (long) nextDay * HordeEventConfig.dayLength.get()
            + HordeEventConfig.hordeStartTime.get();
        int hordeDisplayDay = (int) Math.floorDiv(hordeStart + 6000L, 24000L) + 1;
        int daysLeft = Math.max(0, hordeDisplayDay - currentDay);

        Snapshot current = new Snapshot(currentDay, daysLeft, active);
        Snapshot previous = LAST_ANNOUNCEMENT.put(player.getUUID(), current);
        if (current.equals(previous)) return;
        showTitle(player, daysLeft, active);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_ANNOUNCEMENT.remove(event.getEntity().getUUID());
    }

    private static void migrateSchedule(ServerPlayer player) {
        if (player.getPersistentData().getBoolean(WORLD_CALENDAR_MIGRATED)) return;
        ServerLevel level = player.server.getLevel(ServerLevel.OVERWORLD);
        if (level == null) return;
        HordeSavedData data = HordeSavedData.getData(level);
        HordeEvent horde = data.getEvent(player);
        if (horde.isActive()) return;
        int worldDay = (int) Math.floorDiv(level.getDayTime(), 24000L);
        int nextWorldHorde = (Math.floorDiv(worldDay, 7) + 1) * 7;
        horde.setNextDay(nextWorldHorde);
        data.setDirty();
        player.getPersistentData().putBoolean(WORLD_CALENDAR_MIGRATED, true);
    }

    private static void showTitle(ServerPlayer player, int daysLeft, boolean active) {
        Component title;
        if (active) {
            title = Component.literal("THE HORDE HAS ARRIVED").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        } else if (daysLeft == 0) {
            title = Component.literal("HORDE TONIGHT").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        } else {
            ChatFormatting colour = daysLeft <= 2 ? ChatFormatting.GOLD : ChatFormatting.WHITE;
            title = Component.literal(daysLeft + (daysLeft == 1 ? " DAY TO HORDE" : " DAYS TO HORDE"))
                .withStyle(colour, ChatFormatting.BOLD);
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new CountdownPacket(title.getString(), active || daysLeft == 0 ? 0xFF5555 : daysLeft <= 2 ? 0xFFAA00 : 0xFFFFFF));
        // The chat copy survives loading screens and exposes server-side delivery
        // if another HUD mod prevents the overlay from rendering.
        player.displayClientMessage(title, false);
        System.out.println("[7DTC Horde Countdown] Sent '" + title.getString() + "' to " + player.getGameProfile().getName());
    }

    public record CountdownPacket(String title, int colour) {
        static void encode(CountdownPacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.title);
            buffer.writeInt(packet.colour);
        }
        static CountdownPacket decode(FriendlyByteBuf buffer) {
            return new CountdownPacket(buffer.readUtf(), buffer.readInt());
        }
        static void handle(CountdownPacket packet, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> ClientOverlay.show(packet));
            context.get().setPacketHandled(true);
        }
    }

    @Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ClientOverlay {
        private static final float TITLE_SCALE = 2.4F; // Vanilla title uses 4.0F.
        private static final float SUBTITLE_SCALE = 2.0F;
        private static final long FADE_IN = 500L;
        private static final long STAY = 5000L;
        private static final long FADE_OUT = 1000L;
        private static final int DISPLAY_TICKS = (int) ((FADE_IN + STAY + FADE_OUT) / 50L);
        private static int remainingTicks;
        private static CountdownPacket current;

        private ClientOverlay() {}

        private static void show(CountdownPacket packet) {
            current = packet;
            remainingTicks = DISPLAY_TICKS;
        }

        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || current == null) return;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.screen != null || minecraft.options.hideGui) return;
            if (--remainingTicks <= 0) current = null;
        }

        @SubscribeEvent
        public static void render(RenderGuiEvent.Post event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (current == null || minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) return;
            long elapsed = (DISPLAY_TICKS - remainingTicks) * 50L;
            float opacity = elapsed < FADE_IN ? elapsed / (float) FADE_IN
                : elapsed < FADE_IN + STAY ? 1.0F
                : (FADE_IN + STAY + FADE_OUT - elapsed) / (float) FADE_OUT;
            int alpha = Math.max(4, Math.min(255, (int) (opacity * 255.0F)));
            int width = event.getWindow().getGuiScaledWidth();
            int height = event.getWindow().getGuiScaledHeight();
            var graphics = event.getGuiGraphics();
            var pose = graphics.pose();

            pose.pushPose();
            pose.translate(width / 2.0F, height / 2.0F - 16.0F, 0);
            pose.scale(TITLE_SCALE, TITLE_SCALE, 1.0F);
            Component title = Component.literal(current.title).withStyle(ChatFormatting.BOLD);
            graphics.drawString(minecraft.font, title, -minecraft.font.width(title) / 2, -5,
                (alpha << 24) | current.colour, true);
            pose.popPose();

            pose.pushPose();
            pose.translate(width / 2.0F, height / 2.0F + 14.0F, 0);
            pose.scale(SUBTITLE_SCALE, SUBTITLE_SCALE, 1.0F);
            String subtitle = "Prepare your defenses";
            graphics.drawString(minecraft.font, subtitle, -minecraft.font.width(subtitle) / 2, -5,
                (alpha << 24) | 0x555555, true);
            pose.popPose();
        }
    }

    private record Snapshot(int currentDay, int daysLeft, boolean active) {}
}
