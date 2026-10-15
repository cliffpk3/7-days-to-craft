package dev.sevendaystocraft.locks;

import java.util.List;
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

/** Portable reward chest: the Mimic's artifact is rolled only after a key opens it. */
public final class MimicCacheItem extends Item {
    private static final String[] CHESTS = {
        "", "", "quark:acacia_chest", "quark:prismarine_chest", "quark:nether_brick_chest"
    };
    private final int tier;

    public MimicCacheItem(int tier, Properties properties) {
        super(properties);
        if (tier < 2 || tier > 4) throw new IllegalArgumentException("Mimic cache tier must be 2-4");
        this.tier = tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos position = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(position).canBeReplaced()) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockState state = BuiltInRegistries.BLOCK.get(new ResourceLocation(CHESTS[tier])).defaultBlockState();
        if (!(state.getBlock() instanceof ChestBlock)) return InteractionResult.FAIL;
        if (player != null && state.hasProperty(ChestBlock.FACING)) {
            state = state.setValue(ChestBlock.FACING, player.getDirection().getOpposite());
        }
        if (!level.setBlock(position, state, 11)) return InteractionResult.FAIL;
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (!(blockEntity instanceof RandomizableContainerBlockEntity container)) {
            level.removeBlock(position, false);
            return InteractionResult.FAIL;
        }
        container.getPersistentData().putBoolean("7dtc_lock_rolled", true);
        container.getPersistentData().putInt("7dtc_lock_tier", tier);
        container.getPersistentData().putString("7dtc_source_loot_table", "seven_days_to_craft:mimic_cache");
        container.getPersistentData().putBoolean("7dtc_mimic_cache", true);
        container.setChanged();

        level.playSound(null, position, state.getSoundType(level, position, player).getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.9F);
        if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        if (player != null) player.displayClientMessage(Component.literal("Level " + tier + " Mimic cache placed. Use the matching key to unlock it.").withStyle(ChatFormatting.GOLD), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Places a locked Level " + tier + " chest").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Contains one random Rank " + tier + " artifact").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Requires the matching Level " + tier + " Key").withStyle(ChatFormatting.GRAY));
    }
}
