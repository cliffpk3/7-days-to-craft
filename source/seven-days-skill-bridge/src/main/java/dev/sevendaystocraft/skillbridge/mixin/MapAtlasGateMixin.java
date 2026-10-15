package dev.sevendaystocraft.skillbridge.mixin;

import net.mehvahdjukaar.moonlight.api.platform.network.ChannelHandler;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkDir;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the exploration atlas tied to the Cartography milestone, including
 * opening it through Map Atlases' M key and from a lectern.
 */
@Mixin(targets = "pepjebs.mapatlases.networking.C2S2COpenAtlasScreenPacket", remap = false)
public abstract class MapAtlasGateMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void sevenDays$requireCartography(ChannelHandler.Context context, CallbackInfo callback) {
        if (context.getDirection() != NetworkDir.PLAY_TO_SERVER) return;
        if (!(context.getSender() instanceof ServerPlayer player)) return;
        if (player.getTags().contains("7dtc_cartography")) return;

        player.displayClientMessage(
            Component.literal("Cartography, Minimap and Waypoints is required to use an Atlas.")
                .withStyle(ChatFormatting.RED),
            true
        );
        callback.cancel();
    }
}
