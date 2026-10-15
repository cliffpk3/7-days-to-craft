package dev.sevendaystocraft.locks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.smileycorp.hordes.hordeevent.capability.HordeEvent;
import net.smileycorp.hordes.hordeevent.capability.HordeSavedData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HordeCountdown {
    private static final Map<UUID, Snapshot> LAST_ANNOUNCEMENT = new ConcurrentHashMap<>();
    private static final String WORLD_CALENDAR_MIGRATED = "7dtc_world_horde_calendar_v1";

    private HordeCountdown() {}

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) migrateSchedule(player);
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
        int currentDay = (int) Math.floorDiv(level.getDayTime() + 6000L, 24000L);
        int nextDay = horde.getNextDay();
        boolean active = horde.isActive();
        int daysLeft = Math.max(0, nextDay - 1 - currentDay);

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
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 100, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
            Component.literal("Prepare your defenses").withStyle(ChatFormatting.DARK_GRAY)
        ));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    private record Snapshot(int currentDay, int daysLeft, boolean active) {}
}
