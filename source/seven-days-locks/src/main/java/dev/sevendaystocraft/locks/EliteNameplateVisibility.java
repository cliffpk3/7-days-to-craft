package dev.sevendaystocraft.locks;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/** Keeps elite and horde-role labels visible only to nearby players with line of sight. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EliteNameplateVisibility {
    private static final double MAX_DISTANCE_SQUARED = 24.0D * 24.0D;

    private EliteNameplateVisibility() {}

    @SubscribeEvent
    public static void onRenderName(RenderNameTagEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !mob.isCustomNameVisible()
                || mob.getCustomName() == null) return;

        boolean hordeRole = mob.getTags().stream().anyMatch(tag -> tag.startsWith("7dtc_role_"));

        // Infernal Mobs synchronizes its modifier cache to clients. The name
        // fallback also covers the interval before that packet arrives.
        String name = mob.getCustomName().getString();
        boolean infernalName = name.startsWith("Elite ") || name.startsWith("Rare ")
                || name.startsWith("Ultra ") || name.startsWith("Infernal ");
        if (!hordeRole && (!ModList.get().isLoaded("infernalmobs")
                || InfernalMobsCore.getMobModifiers(mob) == null && !infernalName)) return;

        var viewer = Minecraft.getInstance().player;
        if (viewer == null || viewer.distanceToSqr(mob) > MAX_DISTANCE_SQUARED
                || !viewer.hasLineOfSight(mob)) {
            event.setResult(Event.Result.DENY);
        }
    }
}
