package dev.sevendaystocraft.cgmworkbench;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(targets = "com.mrcrayfish.guns.client.screen.WorkbenchScreen", remap = false)
public abstract class WorkbenchScreenMixin {
    private static final int GRID_COLUMNS = 1;
    private static final int GRID_ROWS = 6;
    private static final int GRID_PAGE_SIZE = GRID_COLUMNS * GRID_ROWS;
    private static final int GRID_CELL = 22;
    private static final int GRID_WIDTH = 169;
    private static final int GRID_HEIGHT = 182;
    private static final int WORKBENCH_WIDTH = 275;
    private static final int WORKBENCH_HEIGHT = 184;

    @Shadow(remap = false)
    private List<Object> tabs;

    @Shadow(remap = false)
    private ItemStack displayStack;

    @Shadow(remap = false)
    private Button btnCraft;

    private int sevenDaysToCraft$gridPage;
    private Object sevenDaysToCraft$lastGridTab;

    @Invoker("loadItem")
    protected abstract void sevenDaysToCraft$loadItem(int index);
    @Inject(method = "m_7286_", at = @At("HEAD"), remap = false)
    private void sevenDaysToCraft$ensureDisplayStack(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (displayStack != null) return;

        Object tab = getCurrentTab();
        List<Object> recipes = getRecipes(tab);
        if (recipes != null && !recipes.isEmpty()) {
            int index = getCurrentIndex(tab);
            sevenDaysToCraft$loadItem(Math.max(0, Math.min(index, recipes.size() - 1)));
        }

        // CGM 1.4.20 assumes this field is never null while rendering. Keep the
        // screen usable even if another mod removes or rejects a recipe.
        if (displayStack == null) displayStack = ItemStack.f_41583_;
    }

    @Inject(method = "m_7856_", at = @At("TAIL"), remap = false)
    private void sevenDaysToCraft$configureCraftButton(CallbackInfo ci) {
        int left = getScreenInt("f_97735_");
        btnCraft.m_252865_(left + 172);
        btnCraft.m_93674_(97);
        btnCraft.m_93666_(Component.m_237113_("Craft"));
        sevenDaysToCraft$hideContainerSlots();
    }

    @Inject(method = "createTabs", at = @At("TAIL"), remap = false)
    private void sevenDaysToCraft$addCustomTabs(CallbackInfo ci) {
        addTab("equipment", "seven_days_to_craft:cgm/equipment/", "insurgency_commander_helmet");
        addTab("utilities", "seven_days_to_craft:cgm/utilities/", "quark_backpack");
        addTab("engineering", "seven_days_to_craft:cgm/engineering/", "cogwheel");
        addTab("defense", "seven_days_to_craft:cgm/defense/", "basic_turret");
        replaceTabIcon("misc", "iron_longsword_craft");
        removeEmptyTabs();
        orderTabs();
        sortRecipesAlphabetically();
    }

    @Inject(method = "m_88315_", at = @At("RETURN"), remap = false)
    private void sevenDaysToCraft$renderRecipeGrid(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Object tab = getCurrentTab();
        if (tab == null) return;
        if (tab != sevenDaysToCraft$lastGridTab) {
            sevenDaysToCraft$lastGridTab = tab;
            sevenDaysToCraft$gridPage = 0;
        }

        List<Object> recipes = getRecipes(tab);
        if (recipes == null || recipes.isEmpty()) return;
        int pageCount = Math.max(1, (recipes.size() + GRID_PAGE_SIZE - 1) / GRID_PAGE_SIZE);
        sevenDaysToCraft$gridPage = Math.min(sevenDaysToCraft$gridPage, pageCount - 1);

        int left = getScreenInt("f_97735_");
        int top = getScreenInt("f_97736_");
        int panelX = getPanelX(left);
        int panelY = top + 1;
        Font font = Minecraft.m_91087_().f_91062_;

        graphics.m_280168_().m_85836_();
        // Stay below Minecraft's tooltip layer (z=400), including the panel border.
        graphics.m_280168_().m_252880_(0.0F, 0.0F, 200.0F);
        graphics.m_280509_(panelX, panelY, panelX + GRID_WIDTH, panelY + GRID_HEIGHT, 0xFFC6C6C6);
        graphics.m_280509_(panelX + 1, panelY + 1, panelX + GRID_WIDTH - 1, panelY + 17, 0xFFC6C6C6);
        graphics.m_280430_(font, Component.m_237115_("gui.cgm.workbench.catalog"), panelX + 6, panelY + 5, 0xFFFFFFFF);

        int selectedIndex = getCurrentIndex(tab);
        int start = sevenDaysToCraft$gridPage * GRID_PAGE_SIZE;
        int end = Math.min(recipes.size(), start + GRID_PAGE_SIZE);
        ItemStack hoveredStack = ItemStack.f_41583_;

        for (int index = start; index < end; index++) {
            int local = index - start;
            int x = panelX + 4;
            int y = panelY + 21 + local * GRID_CELL;
            boolean hovered = mouseX >= x && mouseX < x + GRID_WIDTH - 8 && mouseY >= y && mouseY < y + 20;
            sevenDaysToCraft$renderMenuButton(graphics, x, y, GRID_WIDTH - 8, 20, hovered, index == selectedIndex);
            ItemStack stack = (ItemStack) findResultStackQuiet(recipes.get(index));
            if (!stack.m_41619_()) {
                graphics.m_280480_(stack, x + 2, y + 2);
                graphics.m_280370_(font, stack, x + 2, y + 2);
                String itemName = stack.m_41786_().getString();
                if (itemName.length() > 20) itemName = itemName.substring(0, 19) + "…";
                graphics.m_280488_(font, itemName, x + 25, y + 6, 0xFFFFFF);
                if (hovered) hoveredStack = stack;
            }
        }

        if (pageCount > 1) {
            int controlsY = panelY + GRID_HEIGHT - 17;
            boolean prev = mouseX >= panelX + 5 && mouseX < panelX + 25 && mouseY >= controlsY && mouseY < controlsY + 13;
            boolean next = mouseX >= panelX + GRID_WIDTH - 25 && mouseX < panelX + GRID_WIDTH - 5 && mouseY >= controlsY && mouseY < controlsY + 13;
            sevenDaysToCraft$renderMenuButton(graphics, panelX + 5, controlsY, 20, 13, prev, false);
            sevenDaysToCraft$renderMenuButton(graphics, panelX + GRID_WIDTH - 25, controlsY, 20, 13, next, false);
            graphics.m_280137_(font, "<", panelX + 12, controlsY + 2, 0xFFFFFF);
            graphics.m_280137_(font, ">", panelX + GRID_WIDTH - 18, controlsY + 2, 0xFFFFFF);
            String pageText = (sevenDaysToCraft$gridPage + 1) + "/" + pageCount;
            graphics.m_280137_(font, pageText, panelX + 72, controlsY + 2, 0xFFFFFF);
        }

        sevenDaysToCraft$renderWorkbenchBorder(graphics, left, top);
        if (!hoveredStack.m_41619_()) sevenDaysToCraft$renderTopTooltip(graphics, font, hoveredStack, mouseX, mouseY);
        graphics.m_280168_().m_85849_();
    }

    private static void sevenDaysToCraft$renderTopTooltip(GuiGraphics graphics, Font font, ItemStack stack, int mouseX, int mouseY) {
        graphics.m_280168_().m_85836_();
        graphics.m_280168_().m_252880_(0.0F, 0.0F, 1000.0F);
        graphics.m_280153_(font, stack, mouseX, mouseY);
        graphics.m_280168_().m_85849_();
    }

    private void sevenDaysToCraft$renderWorkbenchBorder(GuiGraphics graphics, int left, int top) {
        // The selected top tab joins the panel, so the bright upper edge stops
        // beneath that tab. All other panel edges keep the vanilla bevel.
        int selectedTab = tabs.indexOf(getCurrentTab());
        int start = selectedTab < 0 ? -1 : left + selectedTab * 28;
        int end = selectedTab < 0 ? -1 : start + 28;
        int upper = 0xFFF0F0F0;
        int lower = 0xFF555555;
        int topY = top + 1;
        if (selectedTab < 0) {
            graphics.m_280509_(left + 1, topY, left + WORKBENCH_WIDTH - 1, topY + 1, upper);
        } else {
            if (start > left + 1) graphics.m_280509_(left + 1, topY, start, topY + 1, upper);
            if (end < left + WORKBENCH_WIDTH - 1) graphics.m_280509_(end, topY, left + WORKBENCH_WIDTH - 1, topY + 1, upper);
        }
        graphics.m_280509_(left + 1, topY, left + 2, top + WORKBENCH_HEIGHT - 1, upper);
        graphics.m_280509_(left + 1, top + WORKBENCH_HEIGHT - 2, left + WORKBENCH_WIDTH - 1, top + WORKBENCH_HEIGHT - 1, lower);
        graphics.m_280509_(left + WORKBENCH_WIDTH - 2, topY, left + WORKBENCH_WIDTH - 1, top + WORKBENCH_HEIGHT - 1, lower);
    }

    private void sevenDaysToCraft$renderMenuButton(GuiGraphics graphics, int x, int y, int width, int height, boolean hovered, boolean selected) {
        // Match Minecraft's beveled menu buttons: light top/left, dark bottom/right,
        // gray face, and a soft blue face on hover or for the selected recipe.
        int border = 0xFF171717;
        int face = hovered || selected ? 0xFF9AA8D4 : 0xFF858585;
        graphics.m_280509_(x, y, x + width, y + height, border);
        graphics.m_280509_(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFEEEEEE);
        graphics.m_280509_(x + 2, y + 2, x + width - 1, y + height - 1, face);
        graphics.m_280509_(x + 2, y + height - 2, x + width - 1, y + height - 1, 0xFF4A4A4A);
        graphics.m_280509_(x + width - 2, y + 2, x + width - 1, y + height - 1, 0xFF4A4A4A);
    }

    @Inject(method = "m_6375_", at = @At("HEAD"), cancellable = true, remap = false)
    private void sevenDaysToCraft$clickRecipeGrid(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;
        Object tab = getCurrentTab();
        List<Object> recipes = getRecipes(tab);
        if (tab == null || recipes == null || recipes.isEmpty()) return;

        int left = getScreenInt("f_97735_");
        int top = getScreenInt("f_97736_");
        int panelX = getPanelX(left);
        int panelY = top + 1;
        if (mouseX < panelX || mouseX >= panelX + GRID_WIDTH || mouseY < panelY || mouseY >= panelY + GRID_HEIGHT) return;

        int pageCount = Math.max(1, (recipes.size() + GRID_PAGE_SIZE - 1) / GRID_PAGE_SIZE);
        int controlsY = panelY + GRID_HEIGHT - 17;
        if (pageCount > 1 && mouseY >= controlsY && mouseY < controlsY + 13) {
            if (mouseX >= panelX + 5 && mouseX < panelX + 25) {
                sevenDaysToCraft$gridPage = Math.max(0, sevenDaysToCraft$gridPage - 1);
                sevenDaysToCraft$playClick();
            }
            if (mouseX >= panelX + GRID_WIDTH - 25 && mouseX < panelX + GRID_WIDTH - 5) {
                sevenDaysToCraft$gridPage = Math.min(pageCount - 1, sevenDaysToCraft$gridPage + 1);
                sevenDaysToCraft$playClick();
            }
            cir.setReturnValue(true);
            return;
        }

        if (mouseY < panelY + 21 || mouseY >= panelY + 21 + GRID_ROWS * GRID_CELL) {
            cir.setReturnValue(true);
            return;
        }

        int row = ((int) mouseY - (panelY + 21)) / GRID_CELL;
        int withinY = ((int) mouseY - (panelY + 21)) % GRID_CELL;
        if (row < 0 || row >= GRID_ROWS || withinY >= 20) {
            cir.setReturnValue(true);
            return;
        }
        int index = sevenDaysToCraft$gridPage * GRID_PAGE_SIZE + row;
        if (index >= recipes.size()) {
            cir.setReturnValue(true);
            return;
        }
        setCurrentIndex(tab, index);
        sevenDaysToCraft$loadItem(index);
        sevenDaysToCraft$playClick();
        cir.setReturnValue(true);
    }

    private static void sevenDaysToCraft$playClick() {
        Minecraft.m_91087_().m_91106_().m_120367_(SimpleSoundInstance.m_263171_(SoundEvents.f_12490_, 1.0F));
    }

    private int getPanelX(int left) {
        return left + 1;
    }

    @SuppressWarnings("unchecked")
    private void sevenDaysToCraft$hideContainerSlots() {
        try {
            Object menu = findField("f_97732_").get(this);
            Field slotsField = findFieldOn(menu.getClass(), "f_38839_");
            List<Object> slots = (List<Object>) slotsField.get(menu);
            for (Object slot : slots) {
                findFieldOn(slot.getClass(), "f_40220_").setInt(slot, -10000);
                findFieldOn(slot.getClass(), "f_40221_").setInt(slot, -10000);
            }
        } catch (ReflectiveOperationException exception) {
            System.err.println("[7 Days to Craft Workbench] Failed to hide container slots: " + exception);
        }
    }

    private Field findFieldOn(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private Object getCurrentTab() {
        try {
            Field field = findField("currentTab");
            return field.get(this);
        } catch (ReflectiveOperationException exception) {
            System.err.println("[7 Days to Craft Workbench] Failed to read current tab: " + exception);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> getRecipes(Object tab) {
        if (tab == null) return null;
        try {
            Method method = tab.getClass().getDeclaredMethod("getRecipes");
            method.setAccessible(true);
            return (List<Object>) method.invoke(tab);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private void setCurrentIndex(Object tab, int index) {
        try {
            Method method = tab.getClass().getDeclaredMethod("setCurrentIndex", int.class);
            method.setAccessible(true);
            method.invoke(tab, index);
        } catch (ReflectiveOperationException exception) {
            System.err.println("[7 Days to Craft Workbench] Failed to select recipe " + index + ": " + exception);
        }
    }

    private int getCurrentIndex(Object tab) {
        try {
            Method method = tab.getClass().getDeclaredMethod("getCurrentIndex");
            method.setAccessible(true);
            return (Integer) method.invoke(tab);
        } catch (ReflectiveOperationException exception) {
            return -1;
        }
    }

    private int getScreenInt(String name) {
        try {
            return findField(name).getInt(this);
        } catch (ReflectiveOperationException exception) {
            return 0;
        }
    }

    private Field findField(String name) throws NoSuchFieldException {
        Class<?> type = this.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private Method findMethod(String name, Class<?>... types) throws NoSuchMethodException {
        Class<?> type = this.getClass();
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(name, types);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchMethodException(name);
    }

    private void orderTabs() {
        List<Object> ordered = new ArrayList<>();
        appendTab(ordered, "equipment");
        appendTab(ordered, "utilities");
        appendTab(ordered, "engineering");
        appendTab(ordered, "defense");
        appendTab(ordered, "weapons");
        appendTab(ordered, "attachments");
        appendTab(ordered, "ammo");
        for (Object tab : tabs) if (!ordered.contains(tab)) ordered.add(tab);
        tabs.clear();
        tabs.addAll(ordered);
    }

    private void appendTab(List<Object> ordered, String id) {
        for (Object tab : tabs) {
            try {
                Method getTabKey = tab.getClass().getDeclaredMethod("getTabKey");
                getTabKey.setAccessible(true);
                if (String.valueOf(getTabKey.invoke(tab)).endsWith("." + id)) {
                    ordered.add(tab);
                    return;
                }
            } catch (ReflectiveOperationException exception) {
                System.err.println("[7 Days to Craft Workbench] Failed to identify tab: " + exception);
            }
        }
    }

    private void sortRecipesAlphabetically() {
        for (Object tab : tabs) {
            List<Object> recipes = getRecipes(tab);
            if (recipes == null) continue;
            recipes.sort((left, right) -> {
                ItemStack leftStack = (ItemStack) findResultStackQuiet(left);
                ItemStack rightStack = (ItemStack) findResultStackQuiet(right);
                String leftName = leftStack.m_41619_() ? "" : leftStack.m_41786_().getString();
                String rightName = rightStack.m_41619_() ? "" : rightStack.m_41786_().getString();
                return String.CASE_INSENSITIVE_ORDER.compare(leftName, rightName);
            });
        }
    }
    private void removeEmptyTabs() {
        tabs.removeIf(tab -> {
            List<Object> recipes = getRecipes(tab);
            return recipes != null && recipes.isEmpty();
        });
    }

    private void addTab(String tabId, String recipePrefix, String preferredIcon) {
        try {
            List<Object> matchingRecipes = new ArrayList<>();
            Object iconStack = null;
            for (Object tab : new ArrayList<>(tabs)) {
                List<Object> recipes = getRecipes(tab);
                if (recipes == null) continue;
                Iterator<Object> iterator = recipes.iterator();
                while (iterator.hasNext()) {
                    Object recipe = iterator.next();
                    String recipeId = findRecipeId(recipe);
                    if (recipeId.startsWith(recipePrefix)) {
                        matchingRecipes.add(recipe);
                        iterator.remove();
                        if (iconStack == null || recipeId.endsWith(preferredIcon)) iconStack = findResultStack(recipe);
                    }
                }
            }
            if (matchingRecipes.isEmpty() || iconStack == null) return;
            Class<?> tabClass = Class.forName("com.mrcrayfish.guns.client.screen.WorkbenchScreen$Tab");
            Constructor<?> constructor = tabClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            tabs.add(constructor.newInstance(iconStack, tabId, matchingRecipes));
        } catch (ReflectiveOperationException exception) {
            System.err.println("[7 Days to Craft Workbench] Failed to create tab '" + tabId + "': " + exception);
        }
    }

    private void replaceTabIcon(String tabId, String preferredRecipeSuffix) {
        try {
            for (int tabIndex = 0; tabIndex < tabs.size(); tabIndex++) {
                Object tab = tabs.get(tabIndex);
                Method getTabKey = tab.getClass().getDeclaredMethod("getTabKey");
                getTabKey.setAccessible(true);
                if (!String.valueOf(getTabKey.invoke(tab)).endsWith("." + tabId)) continue;

                List<Object> recipes = getRecipes(tab);
                if (recipes == null || recipes.isEmpty()) return;
                Object iconStack = null;
                for (Object recipe : recipes) {
                    if (findRecipeId(recipe).endsWith(preferredRecipeSuffix)) {
                        iconStack = findResultStack(recipe);
                        break;
                    }
                }
                if (iconStack == null) iconStack = findResultStack(recipes.get(0));
                if (iconStack == null) return;

                Class<?> tabClass = Class.forName("com.mrcrayfish.guns.client.screen.WorkbenchScreen$Tab");
                Constructor<?> constructor = tabClass.getDeclaredConstructors()[0];
                constructor.setAccessible(true);
                Object replacement = constructor.newInstance(iconStack, tabId, recipes);
                tabs.set(tabIndex, replacement);
                return;
            }
        } catch (ReflectiveOperationException exception) {
            System.err.println("[7 Days to Craft Workbench] Failed to replace tab icon '" + tabId + "': " + exception);
        }
    }

    private String findRecipeId(Object recipe) throws ReflectiveOperationException {
        for (Method method : recipe.getClass().getMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType().getName().equals("net.minecraft.resources.ResourceLocation")) return String.valueOf(method.invoke(recipe));
        }
        return "";
    }

    private Object findResultStack(Object recipe) throws ReflectiveOperationException {
        for (Method method : recipe.getClass().getMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType().getName().equals("net.minecraft.world.item.ItemStack")) return method.invoke(recipe);
        }
        return null;
    }

    private Object findResultStackQuiet(Object recipe) {
        try {
            Object stack = findResultStack(recipe);
            return stack == null ? ItemStack.f_41583_ : stack;
        } catch (ReflectiveOperationException exception) {
            return ItemStack.f_41583_;
        }
    }
}


