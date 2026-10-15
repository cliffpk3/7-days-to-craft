package dev.sevendaystocraft.skillbridge.mixin;

import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Mixin(EffectRenderingInventoryScreen.class)
public abstract class XaeroRestrictionEffectsMixin {
    private static final Set<ResourceLocation> SEVEN_DAYS_HIDDEN_XAERO_EFFECTS = Set.of(
        new ResourceLocation("xaerominimap", "no_minimap"),
        new ResourceLocation("xaerominimap", "no_waypoints")
    );

    @Redirect(
        method = "m_280113_",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;m_21220_()Ljava/util/Collection;",
            remap = false
        ),
        require = 1,
        remap = false
    )
    private Collection<MobEffectInstance> sevenDays$hideXaeroRestrictionEffects(LocalPlayer player) {
        List<MobEffectInstance> visibleEffects = player.getActiveEffects().stream()
            .filter(instance -> {
                ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect());
                return id == null || !SEVEN_DAYS_HIDDEN_XAERO_EFFECTS.contains(id);
            })
            .toList();
        return visibleEffects;
    }
}
