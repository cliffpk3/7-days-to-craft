package dev.sevendaystocraft.skillbridge;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.FTBQuestsClient;
import dev.ftb.mods.ftbquests.client.FTBQuestsClientEventHandler;
import dev.ftb.mods.ftbquests.net.TogglePinnedMessage;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(modid = SkillBridge.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class QuestHud {
    public static final KeyMapping HIDE_QUESTS = new KeyMapping(
        "key.seven_days_skill_bridge.hide_quests",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_I,
        "key.categories.seven_days_to_craft"
    );

    private static boolean autoPinRequested;
    private static boolean hiddenThisSession;
    private static final FTBQuestsClientEventHandler QUEST_TEXT_READER = new FTBQuestsClientEventHandler();
    private static final Method COLLECT_PINNED_QUESTS;
    private static final Field PINNED_QUEST_TEXT;

    static {
        Method collect = null;
        Field text = null;
        try {
            collect = FTBQuestsClientEventHandler.class.getDeclaredMethod("collectPinnedQuests", ClientQuestFile.class);
            collect.setAccessible(true);
            text = FTBQuestsClientEventHandler.class.getDeclaredField("pinnedQuestText");
            text.setAccessible(true);
        } catch (ReflectiveOperationException ignored) {
        }
        COLLECT_PINNED_QUESTS = collect;
        PINNED_QUEST_TEXT = text;
    }

    private QuestHud() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            autoPinRequested = false;
            hiddenThisSession = false;
            return;
        }

        if (HIDE_QUESTS.consumeClick()) {
            boolean showTracker = hiddenThisSession;
            hiddenThisSession = !hiddenThisSession;
            if (FTBQuestsClient.isClientDataLoaded()) {
                boolean pinned = ClientQuestFile.isQuestPinned(1L);
                if ((showTracker && !pinned) || (!showTracker && pinned)) {
                    new TogglePinnedMessage(1L).sendToServer();
                }
            }
            autoPinRequested = true;
            minecraft.player.displayClientMessage(Component.literal(showTracker
                ? "Quest tracker shown"
                : "Quest tracker hidden. Press I to show it again").withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        if (!hiddenThisSession && !autoPinRequested && FTBQuestsClient.isClientDataLoaded()) {
            if (!ClientQuestFile.isQuestPinned(1L)) {
                new TogglePinnedMessage(1L).sendToServer();
            }
            autoPinRequested = true;
        }
    }

    @SubscribeEvent
    public static void render(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || hiddenThisSession) return;
        if (!FTBQuestsClient.isClientDataLoaded()) return;

        MutableComponent hideText = Component.literal("Press ")
            .append(HIDE_QUESTS.getTranslatedKeyMessage())
            .append(" to hide, Press ")
            .append(FTBQuestsClient.KEY_QUESTS.getTranslatedKeyMessage())
            .append(" to expand")
            .withStyle(ChatFormatting.GRAY);

        List<FormattedCharSequence> questLines = collectVisibleQuestText(minecraft);
        int panelWidth = minecraft.font.width(hideText);
        for (FormattedCharSequence line : questLines) {
            panelWidth = Math.max(panelWidth, minecraft.font.width(line));
        }

        int questY = 4;
        int hintY = questY + (questLines.size() * 9);
        int panelRight = panelWidth + 8;
        int panelBottom = hintY + 12;
        float nativeScale = ThemeProperties.PINNED_QUEST_SIZE.get(ClientQuestFile.INSTANCE).floatValue();

        MutableComponent rewardText = Component.literal("Press ")
            .append(FTBQuestsClient.KEY_QUESTS.getTranslatedKeyMessage())
            .append(" to get your reward")
            .withStyle(ChatFormatting.GREEN);
        boolean hasReward = FTBQuestsClient.getClientPlayerData() != null
            && FTBQuestsClient.getClientPlayerData().hasUnclaimedRewards(
                minecraft.player.getUUID(), ClientQuestFile.INSTANCE);
        if (hasReward) {
            panelWidth = Math.max(panelWidth, minecraft.font.width(rewardText));
            panelRight = panelWidth + 8;
            panelBottom += 9;
        }

        // Keep the quest tracker in its own stable corner. Status effects use the
        // opposite side, while ToroHealth is positioned farther down the left edge.
        int topMargin = 8;
        int anchorX = 8;

        var pose = event.getGuiGraphics().pose();
        pose.pushPose();
        pose.translate(anchorX, topMargin, 200.0F);
        pose.scale(nativeScale, nativeScale, 1.0F);

        event.getGuiGraphics().fill(0, 0, panelRight, panelBottom, 0xA0000000);
        for (int i = 0; i < questLines.size(); i++) {
            event.getGuiGraphics().drawString(minecraft.font, questLines.get(i), 4, questY + (i * 9), 0xFFFFFF, false);
        }
        event.getGuiGraphics().drawString(minecraft.font, hideText, 4, hintY, 0xA0A0A0, false);
        if (hasReward) {
            event.getGuiGraphics().drawString(minecraft.font, rewardText, 4, hintY + 9, 0x55FF55, false);
        }
        pose.popPose();
    }

    private static List<FormattedCharSequence> collectVisibleQuestText(Minecraft minecraft) {
        if (ClientQuestFile.INSTANCE == null || ClientQuestFile.INSTANCE.selfTeamData == null) {
            return Collections.emptyList();
        }
        TeamData data = ClientQuestFile.INSTANCE.selfTeamData;
        List<FormattedCharSequence> lines = new ArrayList<>();
        ClientQuestFile.INSTANCE.forAllQuests(quest -> {
            if (!quest.isVisible(data) || data.isCompleted(quest) || !data.canStartTasks(quest)) return;
            int progress = quest.getRelativeProgressFromChildren(data);
            MutableComponent title = quest.getTitle().copy()
                .append(Component.literal(" " + progress + "%").withStyle(ChatFormatting.AQUA));
            lines.addAll(minecraft.font.split(title, 500));
            for (Task task : quest.getTasks()) {
                MutableComponent line = task.getMutableTitle().withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(" ").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(task.formatProgress(data, data.getProgress(task)) + "/" + task.formatMaxProgress())
                        .withStyle(ChatFormatting.GREEN));
                lines.addAll(minecraft.font.split(line, 500));
            }
        });
        return lines;
    }
    @Mod.EventBusSubscriber(modid = SkillBridge.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        private ModEvents() {
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(HIDE_QUESTS);
        }
    }
}
