package dev.sevendaystocraft.skillbridge.mixin;

import dev.sevendaystocraft.skillbridge.SkillBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "com.mrcrayfish.guns.common.network.ServerPlayHandler", remap = false)
public abstract class WorkbenchEfficiencyMixin {
    private static final ThreadLocal<List<ItemStack>> SEVEN_DAYS_BEFORE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SEVEN_DAYS_ROLL = new ThreadLocal<>();

    @Inject(method = "handleCraft", at = @At("HEAD"), cancellable = true)
    private static void sevenDays$beforeCraft(ServerPlayer player, ResourceLocation recipeId, BlockPos pos, CallbackInfo ci) {
        String requirement = sevenDays$requirement(player, recipeId);
        if (requirement != null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(requirement).withStyle(net.minecraft.ChatFormatting.RED), true);
            ci.cancel();
            return;
        }
        int rank = SkillBridge.getBasicSkillRank(player, "workshopefficiency", 8);
        boolean success = rank > 0 && player.getRandom().nextDouble() < rank * 0.03D;
        SEVEN_DAYS_ROLL.set(success);
        if (!success) return;
        List<ItemStack> snapshot = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) snapshot.add(stack.copy());
        SEVEN_DAYS_BEFORE.set(snapshot);
    }

    @Inject(method = "handleCraft", at = @At("RETURN"))
    private static void sevenDays$afterCraft(ServerPlayer player, ResourceLocation recipeId, BlockPos pos, CallbackInfo ci) {
        boolean success = Boolean.TRUE.equals(SEVEN_DAYS_ROLL.get());
        SEVEN_DAYS_ROLL.remove();
        List<ItemStack> before = SEVEN_DAYS_BEFORE.get();
        SEVEN_DAYS_BEFORE.remove();
        if (!success || before == null) return;

        List<ItemStack> consumed = new ArrayList<>();
        for (int i = 0; i < before.size(); i++) {
            ItemStack oldStack = before.get(i);
            if (oldStack.isEmpty() || !sevenDays$isCommon(oldStack)) continue;
            int afterCount = 0;
            for (ItemStack now : player.getInventory().items) if (ItemStack.isSameItemSameTags(oldStack, now)) afterCount += now.getCount();
            int beforeCount = 0;
            for (ItemStack old : before) if (ItemStack.isSameItemSameTags(oldStack, old)) beforeCount += old.getCount();
            if (afterCount < beforeCount && consumed.stream().noneMatch(s -> ItemStack.isSameItemSameTags(s, oldStack))) consumed.add(oldStack.copyWithCount(1));
        }
        if (consumed.isEmpty()) return;
        ItemStack refund = consumed.get(player.getRandom().nextInt(consumed.size()));
        if (!player.getInventory().add(refund)) player.drop(refund, false);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("Workshop Efficiency preserved " + refund.getHoverName().getString() + "."), true);
    }

    private static String sevenDays$requirement(ServerPlayer player, ResourceLocation recipeId) {
        String namespace = recipeId.getNamespace();
        String path = recipeId.getPath();
        if (namespace.equals("cgm")) {
            if (path.equals("basic_bullet") && !player.getTags().contains("7dtc_ammunition_1")) return "Ammunition I is required to craft Basic Bullets.";
            if (path.equals("advanced_bullet") || path.equals("shell") || path.equals("grenade") || path.equals("stun_grenade") || path.equals("missile")) {
                if (!player.getTags().contains("7dtc_ammunition_2")) return "Ammunition II is required to craft this ammunition.";
            }
            if (path.equals("assault_rifle") || path.equals("bazooka") || path.equals("grenade_launcher") || path.equals("heavy_rifle") || path.equals("machine_pistol") || path.equals("mini_gun") || path.equals("pistol") || path.equals("rifle") || path.equals("shotgun")) {
                if (!player.getTags().contains("7dtc_gunsmithing")) return "Gunsmithing is required to craft firearms.";
            }
        }
        if (!namespace.equals("seven_days_to_craft") || !path.startsWith("cgm/")) return null;
        if (path.equals("cgm/utilities/quark_backpack") && !player.getTags().contains("7dtc_backpack")) return "Backpack License is required to craft the Quark Backpack.";
        if (path.equals("cgm/utilities/grappling_hook") && !player.getTags().contains("7dtc_grappler")) return "Grappler License is required to craft a Grappling Hook.";
        if (path.equals("cgm/utilities/recycler") && !player.getTags().contains("7dtc_salvager")) return "Salvager is required to craft the Recycler.";
        if (path.startsWith("cgm/engineering/")) {
            boolean processing = path.endsWith("depot") || path.endsWith("encased_fan") || path.endsWith("mechanical_press") || path.endsWith("mechanical_mixer");
            if (processing && !player.getTags().contains("7dtc_mechanics_2")) return "Mechanical Processing is required to craft this component.";
            if (!processing && !player.getTags().contains("7dtc_mechanics_1")) return "Mechanical Foundations is required to craft this component.";
        }
        if (path.startsWith("cgm/defense/basic_") && !player.getTags().contains("7dtc_sentry_engineering")) return "Sentry Engineering is required to craft this defense item.";
        if ((path.startsWith("cgm/defense/advanced_") || path.startsWith("cgm/defense/shotgun_") || path.equals("cgm/defense/turret_shell_box")) && !player.getTags().contains("7dtc_turret_engineering")) return "Turret Engineering is required to craft this defense item.";
        if ((path.startsWith("cgm/defense/explosive_") || path.equals("cgm/defense/grenade_canister")) && !player.getTags().contains("7dtc_automated_defense")) return "Automated Defense is required to craft this defense item.";
        return null;
    }
    private static boolean sevenDays$isCommon(ItemStack stack) {
        return stack.getMaxStackSize() > 1 && !stack.is(Items.DIAMOND) && !stack.is(Items.EMERALD)
            && !stack.is(Items.NETHERITE_INGOT) && !stack.is(Items.NETHERITE_SCRAP)
            && !stack.is(Items.ENDER_PEARL) && !stack.is(Items.BLAZE_ROD)
            && !stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }
}
