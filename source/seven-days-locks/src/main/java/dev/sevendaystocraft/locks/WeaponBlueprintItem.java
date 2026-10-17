package dev.sevendaystocraft.locks;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A single blueprint carries its remaining number of Workbench crafts. */
public final class WeaponBlueprintItem extends Item {
    public static final String USES_TAG = "7dtc_blueprint_uses";
    private final String group;
    private final String weapon;

    WeaponBlueprintItem(String group, String weapon) {
        super(new Item.Properties().stacksTo(1));
        this.group = group;
        this.weapon = weapon;
    }

    public static int remaining(ItemStack stack) {
        // Existing blueprints from older worlds are valid for one craft.
        return Math.max(1, stack.hasTag() ? stack.getTag().getInt(USES_TAG) : 1);
    }

    public static ItemStack rolled(Item item, RandomSource random) {
        ItemStack stack = new ItemStack(item);
        if (!(item instanceof WeaponBlueprintItem blueprint)) return stack;
        double roll = random.nextDouble();
        int uses = "gun".equals(blueprint.group) || "tacz".equals(blueprint.group)
            ? roll < 0.10D ? 3 : roll < 0.30D ? 2 : 1
            : roll < 0.15D ? 6 : roll < 0.45D ? 3 : 1;
        stack.getOrCreateTag().putInt(USES_TAG, uses);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        int uses = remaining(stack);
        if ("tacz".equals(group)) {
            tooltip.add(Component.translatable(uses == 1
                    ? "tooltip.seven_days_locks.blueprint_use"
                    : "tooltip.seven_days_locks.blueprint_uses",
                Component.translatable("tacz.gun." + weapon + ".name"), uses).withStyle(ChatFormatting.GRAY));
            return;
        }
        String namespace = "gun".equals(group) && !weapon.equals("blood_sprayer")
            && !weapon.equals("stink_ray") && !weapon.equals("hemolymph_blaster") ? "cgm"
            : "gun".equals(group) ? "alexsmobs" : "divine_weaponry";
        Item target = BuiltInRegistries.ITEM.get(new ResourceLocation(namespace, weapon));
        tooltip.add(Component.translatable(uses == 1
                ? "tooltip.seven_days_locks.blueprint_use"
                : "tooltip.seven_days_locks.blueprint_uses",
            target.getDescription(), uses).withStyle(ChatFormatting.GRAY));
    }
}
