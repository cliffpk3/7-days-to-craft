package dev.sevendaystocraft.cgmworkbench;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Minecraft.class, remap = false)
public abstract class OffhandTorchRestrictionMixin {
    @Redirect(
        method = "m_91277_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;m_233732_(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
        ),
        remap = false
    )
    private InteractionResult sevenDaysToCraft$blockRightClickOffhandTorch(
        MultiPlayerGameMode gameMode,
        LocalPlayer player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (hand == InteractionHand.OFF_HAND && sevenDaysToCraft$isTorch(player.m_21120_(hand).m_41720_())) {
            return InteractionResult.PASS;
        }
        return gameMode.m_233732_(player, hand, hit);
    }

    private static boolean sevenDaysToCraft$isTorch(Item item) {
        return item instanceof BlockItem blockItem && blockItem.m_40614_() instanceof TorchBlock;
    }
}
