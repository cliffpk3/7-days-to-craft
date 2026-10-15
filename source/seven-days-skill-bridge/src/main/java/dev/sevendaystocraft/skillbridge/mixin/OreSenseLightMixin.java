package dev.sevendaystocraft.skillbridge.mixin;

import dev.sevendaystocraft.skillbridge.OreSenseOverlay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockBehaviour.BlockStateBase.class, priority = 1500)
public abstract class OreSenseLightMixin {
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, new ResourceLocation("forge", "ores"));

    @Shadow(remap = false)
    public abstract Block m_60734_();

    @Inject(method = "m_60791_", at = @At("HEAD"), cancellable = true, remap = false)
    private void sevenDays$gateOreLight(CallbackInfoReturnable<Integer> cir) {
        if (m_60734_().builtInRegistryHolder().is(ORES)) {
            cir.setReturnValue(OreSenseOverlay.isUnlocked() ? OreSenseOverlay.LIGHT_LEVEL : 0);
        }
    }
}
