package dev.sevendaystocraft.locks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class TrainingCacheItem extends Item {
    private static final ResourceLocation LEVEL_ONE_CHEST = new ResourceLocation("quark:ancient_chest");

    public TrainingCacheItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos position = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(position).canBeReplaced()) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockState state = BuiltInRegistries.BLOCK.get(LEVEL_ONE_CHEST).defaultBlockState();
        if (player != null && state.hasProperty(ChestBlock.FACING)) {
            state = state.setValue(ChestBlock.FACING, player.getDirection().getOpposite());
        }
        if (!level.setBlock(position, state, 11)) return InteractionResult.FAIL;

        BlockEntity blockEntity = level.getBlockEntity(position);
        if (blockEntity instanceof RandomizableContainerBlockEntity container) {
            container.getPersistentData().putBoolean("7dtc_lock_rolled", true);
            container.getPersistentData().putInt("7dtc_lock_tier", 1);
            container.getPersistentData().putString("7dtc_source_loot_table", "seven_days_to_craft:onboarding_cache");
            container.getPersistentData().putBoolean("7dtc_onboarding_cache", true);
            container.setChanged();
        }
        BlockState placed = level.getBlockState(position);
        level.playSound(null, position, placed.getSoundType(level, position, player).getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.9F);
        if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        if (player != null) player.displayClientMessage(Component.literal("Level 1 cache placed. Use an Iron Level 1 Key to unlock it.").withStyle(ChatFormatting.GOLD), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Places a locked Level 1 chest").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Guarantees one random Rank 1 amulet").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Requires an Iron Level 1 Key to open").withStyle(ChatFormatting.GRAY));
    }
}
