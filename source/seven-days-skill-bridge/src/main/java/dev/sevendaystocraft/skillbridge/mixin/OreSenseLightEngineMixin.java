package dev.sevendaystocraft.skillbridge.mixin;

import dev.sevendaystocraft.skillbridge.OreSenseOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockLightEngine.class)
public abstract class OreSenseLightEngineMixin {
    @Redirect(method = "m_284436_", remap = false, at = @At(value = "INVOKE", remap = false,
        target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I"))
    private int sevenDays$limitOreEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return OreSenseOverlay.lightEmission(state, level, pos);
    }

    @Redirect(method = "m_284140_", remap = false,
        at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I"))
    private int sevenDays$limitInitialOreEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return OreSenseOverlay.lightEmission(state, level, pos);
    }
}
