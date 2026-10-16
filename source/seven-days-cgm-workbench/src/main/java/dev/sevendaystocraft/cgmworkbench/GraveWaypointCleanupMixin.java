package dev.sevendaystocraft.cgmworkbench;

import de.maxhenkel.gravestone.corelib.death.Death;
import de.maxhenkel.gravestone.tileentity.GraveStoneTileEntity;
import dlovin.advancedcompass.AdvancedCompass;
import dlovin.advancedcompass.utils.StringUtils;
import dlovin.advancedcompass.utils.waypoints.Waypoint;
import dlovin.advancedcompass.utils.waypoints.WaypointIcon;
import dlovin.advancedcompass.utils.waypoints.WaypointJsonUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = MultiPlayerGameMode.class, remap = false)
public abstract class GraveWaypointCleanupMixin {
    @Unique
    private boolean sevenDaysToCraft$breakingGrave;
    @Unique
    private Vec3 sevenDaysToCraft$deathPosition;
    @Unique
    private String sevenDaysToCraft$deathDimension;
    @Unique
    private String sevenDaysToCraft$deathWaypointName;

    @Inject(method = "m_105267_", at = @At("HEAD"), remap = false)
    private void sevenDaysToCraft$captureGrave(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        sevenDaysToCraft$clearPendingGrave();

        Minecraft minecraft = Minecraft.m_91087_();
        if (minecraft.f_91073_ == null) {
            return;
        }

        BlockEntity blockEntity = minecraft.f_91073_.m_7702_(pos);
        if (!(blockEntity instanceof GraveStoneTileEntity grave)) {
            return;
        }

        sevenDaysToCraft$breakingGrave = true;
        Death death = grave.getDeath();
        if (death != null) {
            sevenDaysToCraft$deathPosition = new Vec3(death.getPosX(), death.getPosY(), death.getPosZ());
            sevenDaysToCraft$deathDimension = death.getDimension();
            sevenDaysToCraft$deathWaypointName = "DP(" + (int) death.getPosX() + "_" + (int) death.getPosY() + "_" + (int) death.getPosZ() + ")";
        } else {
            sevenDaysToCraft$deathPosition = Vec3.m_82512_(pos);
            sevenDaysToCraft$deathDimension = minecraft.f_91073_.m_46472_().m_135782_().toString();
        }
    }

    @Inject(method = "m_105267_", at = @At("RETURN"), remap = false)
    private void sevenDaysToCraft$removeDeathWaypoint(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!sevenDaysToCraft$breakingGrave || !Boolean.TRUE.equals(cir.getReturnValue())) {
            sevenDaysToCraft$clearPendingGrave();
            return;
        }

        try {
            String serverName = sevenDaysToCraft$getServerName();
            if (serverName == null) {
                return;
            }

            List<Waypoint> waypoints = WaypointJsonUtils.getWaypoints(serverName);
            Waypoint matching = sevenDaysToCraft$findMatchingDeathWaypoint(waypoints);
            if (matching != null) {
                WaypointJsonUtils.deleteWaypoint(serverName, matching);
                AdvancedCompass.getInstance().getCompassGui().removeWaypoint(matching);
            }
        } catch (RuntimeException exception) {
            AdvancedCompass.logger.warn("Could not remove the Advanced Compass death waypoint for a broken gravestone", exception);
        } finally {
            sevenDaysToCraft$clearPendingGrave();
        }
    }

    @Unique
    private Waypoint sevenDaysToCraft$findMatchingDeathWaypoint(List<Waypoint> waypoints) {
        Waypoint nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Waypoint waypoint : waypoints) {
            if (waypoint.getIcon() != WaypointIcon.SKULL
                || waypoint.getName() == null
                || !waypoint.getName().startsWith("DP(")
                || !sevenDaysToCraft$sameDimension(waypoint.getDimension())) {
                continue;
            }

            if (sevenDaysToCraft$deathWaypointName != null && sevenDaysToCraft$deathWaypointName.equals(waypoint.getName())) {
                return waypoint;
            }

            if (sevenDaysToCraft$deathPosition != null) {
                double distance = waypoint.getPosition().m_82557_(sevenDaysToCraft$deathPosition);
                if (distance <= 144.0D && distance < nearestDistance) {
                    nearest = waypoint;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
    }

    @Unique
    private boolean sevenDaysToCraft$sameDimension(String waypointDimension) {
        return sevenDaysToCraft$deathDimension == null || sevenDaysToCraft$deathDimension.equals(waypointDimension);
    }

    @Unique
    private static String sevenDaysToCraft$getServerName() {
        Minecraft minecraft = Minecraft.m_91087_();
        ServerData serverData = minecraft.m_91089_();
        if (serverData != null) {
            return StringUtils.trimPort(serverData.f_105363_);
        }
        if (minecraft.m_91092_() == null) {
            return null;
        }
        try {
            Object serverLevel = minecraft.m_91092_().getClass().getMethod("m_129783_").invoke(minecraft.m_91092_());
            Object levelDataObject = serverLevel.getClass().getMethod("m_6106_").invoke(serverLevel);
            ServerLevelData levelData = (ServerLevelData) levelDataObject;
            return StringUtils.removeIllegal(levelData.m_5462_());
        } catch (ReflectiveOperationException exception) {
            AdvancedCompass.logger.warn("Could not identify the singleplayer waypoint folder", exception);
            return null;
        }
    }

    @Unique
    private void sevenDaysToCraft$clearPendingGrave() {
        sevenDaysToCraft$breakingGrave = false;
        sevenDaysToCraft$deathPosition = null;
        sevenDaysToCraft$deathDimension = null;
        sevenDaysToCraft$deathWaypointName = null;
    }
}
