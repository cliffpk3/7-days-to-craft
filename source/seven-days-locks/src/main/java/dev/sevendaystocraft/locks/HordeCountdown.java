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

    private HordeCountdown() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        HordeEvent horde = HordeSavedData.getData(level).getEvent(player);
        int currentDay = horde.getCurrentDay(player);
        int nextDay = horde.getNextDay();
        boolean active = horde.isActive();
        int daysLeft = Math.max(0, nextDay - currentDay);

        Snapshot current = new Snapshot(currentDay, daysLeft, active);
        Snapshot previous = LAST_ANNOUNCEMENT.put(player.getUUID(), current);
        if (current.equals(previous)) return;
        showTitle(player, daysLeft, active);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_ANNOUNCEMENT.remove(event.getEntity().getUUID());
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
