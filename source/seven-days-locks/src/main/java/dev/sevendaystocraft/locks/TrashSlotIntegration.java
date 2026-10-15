package dev.sevendaystocraft.locks;

import net.blay09.mods.trashslot.TrashSlotSaveState;
import net.blay09.mods.trashslot.api.IGuiContainerLayout;
import net.blay09.mods.trashslot.api.SlotRenderStyle;
import net.blay09.mods.trashslot.client.ContainerSettings;
import net.blay09.mods.trashslot.client.LayoutManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps the trash icon attached to hotbar slot 1 in the E inventory. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TrashSlotIntegration {
    private static final int GAP = 2;

    private TrashSlotIntegration() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen rawScreen = event.getScreen();
        if (!(rawScreen instanceof AbstractContainerScreen<?> screen) || !isPlayerInventory(screen)) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        Inventory inventory = minecraft.player.getInventory();
        Slot firstHotbarSlot = null;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == inventory && slot.getContainerSlot() == 0) {
                firstHotbarSlot = slot;
                break;
            }
        }
        if (firstHotbarSlot == null) return;

        IGuiContainerLayout layout = LayoutManager.getLayout(screen);
        ContainerSettings settings = TrashSlotSaveState.getSettings(screen, layout);
        settings.setAnchorX(0.0F);
        settings.setAnchorY(0.0F);
        // TrashSlot renders a 28px frame around its 18px slot. Reserve the full
        // frame so it cannot cover hotbar slot 1, even when Quark expands E.
        settings.setSlotX(firstHotbarSlot.x - SlotRenderStyle.LONE.getWidth() - GAP);
        settings.setSlotY(firstHotbarSlot.y);
        settings.setEnabled(true);
        settings.setLocked(true);
        TrashSlotSaveState.save();
    }

    private static boolean isPlayerInventory(AbstractContainerScreen<?> screen) {
        if (screen instanceof InventoryScreen) return true;
        String name = screen.getClass().getName();
        return name.equals("top.theillusivec4.curios.client.gui.CuriosScreenV2")
            || name.equals("top.theillusivec4.curios.client.gui.CuriosScreen")
            || name.equals("org.violetmoon.quark.addons.oddities.client.screen.BackpackInventoryScreen");
    }
}
