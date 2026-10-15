package dev.sevendaystocraft.skillbridge.mixin;

import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestPanel;
import dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.Quest;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;

@Mixin(value = QuestScreen.class, remap = false)
public abstract class MilestoneTrainingFocusMixin {
    @Shadow @Final private ClientQuestFile file;
    @Shadow @Final public QuestPanel questPanel;

    @Inject(method = "selectChapter", at = @At("RETURN"))
    private void sevenDays$focusFirstAvailableTraining(Chapter chapter, CallbackInfo ci) {
        if (chapter == null || !"skill_training".equals(chapter.getFilename()) || file.selfTeamData == null) {
            return;
        }

        Comparator<Quest> readingOrder = Comparator
            .comparingDouble(Quest::getY)
            .thenComparingDouble(Quest::getX);

        Quest firstVisible = chapter.getQuests().stream()
            .filter(quest -> quest.isVisible(file.selfTeamData))
            .min(readingOrder)
            .orElse(null);

        Quest focus = chapter.getQuests().stream()
            .filter(quest -> quest.isVisible(file.selfTeamData))
            .filter(quest -> !quest.isCompletedRaw(file.selfTeamData))
            .min(readingOrder)
            .orElse(firstVisible);

        if (focus != null) {
            questPanel.scrollTo(focus.getX(), focus.getY());
        }
    }
}
