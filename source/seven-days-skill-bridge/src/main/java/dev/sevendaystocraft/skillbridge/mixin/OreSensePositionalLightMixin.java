package dev.sevendaystocraft.skillbridge.mixin;

import dev.sevendaystocraft.skillbridge.OreSenseOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/** Overrides Forge's positional default method for client-side ore lighting. */
@Mixin(BlockState.class)
public abstract class OreSensePositionalLightMixin {
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK,
        new ResourceLocation("forge", "ores"));

    public int getLightEmission(BlockGetter level, BlockPos pos) {
        BlockState state = (BlockState) (Object) this;
        if (state.is(ORES)) return OreSenseOverlay.lightEmission(state, level, pos);
        return state.getBlock().getLightEmission(state, level, pos);
    }
}
