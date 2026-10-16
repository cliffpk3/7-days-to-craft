package dev.sevendaystocraft.locks;

import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps Quark's sort button off the expanded backpack inventory. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InventorySortPlacement {
    private static final String BUTTON_CLASS =
        "org.violetmoon.quark.content.management.client.screen.widgets.MiniInventoryButton";
    private static Field startY;

    private InventorySortPlacement() {}

    @SubscribeEvent
    public static void beforeRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)
            || !TrashSlotIntegration.isPlayerInventory(screen)) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        // The backpack is worn in a Curios slot. Its extra storage covers the
        // position Quark normally chooses for the inventory sort button.
        boolean backpackEquipped = screen.getMenu().slots.stream()
            .anyMatch(slot -> slot.container != player.getInventory() && slot.hasItem()
                && BuiltInRegistries.ITEM.getKey(slot.getItem().getItem()).getPath().contains("backpack"));
        if (!backpackEquipped) return;

        try {
            for (GuiEventListener child : screen.children()) {
                if (!BUTTON_CLASS.equals(child.getClass().getName())) continue;
                if (startY == null) {
                    startY = child.getClass().getDeclaredField("startY");
                    startY.setAccessible(true);
                }
                // Keep the button in the lower-right empty area above the
                // backpack slots, close to its position without a backpack.
                // Quark recalculates widget Y from startY every frame.
                startY.setInt(child, 65);
            }
        } catch (ReflectiveOperationException ignored) {
            // Leave Quark's own button intact if a future version changes it.
        }
    }
}
