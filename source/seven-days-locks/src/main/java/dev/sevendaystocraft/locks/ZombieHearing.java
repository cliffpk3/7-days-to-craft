package dev.sevendaystocraft.locks;

import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Server-side, bounded zombie investigation of gunshots and militia screams. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ZombieHearing {
    private static final String PREFIX = "7dtc_heard_noise_";
    private static final String UNTIL = PREFIX + "until";
    private static final String X = PREFIX + "x";
    private static final String Y = PREFIX + "y";
    private static final String Z = PREFIX + "z";
    private static final String NEXT_GUNSHOT = PREFIX + "next_gunshot";
    private static final int MEMORY_TICKS = 160;
    private static final int GUNSHOT_DEBOUNCE_TICKS = 20;
    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.IntValue GUNSHOT_RADIUS;
    private static final ForgeConfigSpec.IntValue SILENCED_RADIUS;
    private static final ForgeConfigSpec.IntValue SCREAM_RADIUS;
    private static final ForgeConfigSpec.IntValue ZOMBIE_CRY_RADIUS;
    private static final ForgeConfigSpec.IntValue MAX_RESPONDERS;
    private static final Map<String, Long> LAST_ZOMBIE_CRY = new HashMap<>();

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("7DTC zombie hearing; affects loaded zombies outside the horde.").push("hearing");
        ENABLED = builder.define("enabled", true);
        GUNSHOT_RADIUS = builder.comment("Maximum distance for an unsuppressed gunshot, in blocks.")
            .defineInRange("gunshotRadius", 48, 0, 96);
        SILENCED_RADIUS = builder.comment("Maximum distance for a gun with a suppressor, in blocks.")
            .defineInRange("silencedRadius", 16, 0, 96);
        SCREAM_RADIUS = builder.comment("Maximum distance for a militia death scream, in blocks.")
            .defineInRange("screamRadius", 24, 0, 96);
        ZOMBIE_CRY_RADIUS = builder.comment("Maximum distance for a dying zombie's cry, in blocks.")
            .defineInRange("zombieCryRadius", 14, 0, 48);
        MAX_RESPONDERS = builder.comment("Maximum zombies responding to one sound.")
            .defineInRange("maxResponders", 16, 1, 40);
        builder.pop();
        SPEC = builder.build();
    }

    private ZombieHearing() {}

    static void registerConfig() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "seven-days-zombie-hearing.toml");
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        LAST_ZOMBIE_CRY.clear();
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof PathfinderMob mob)
            || !CityAmbientEncounter.isCityZombie(mob)) return;
        mob.goalSelector.addGoal(3, new InvestigateSoundGoal(mob));
    }

    @SubscribeEvent
    public static void onTaczGunfire(GunFireEvent event) {
        if (!event.getLogicalSide().isServer()) return;
        hearGunshot(event.getShooter(), suppressed(event.getGunItemStack()));
    }

    @SubscribeEvent
    public static void onCgmGunfire(com.mrcrayfish.guns.event.GunFireEvent.Post event) {
        if (event.isClient()) return;
        hearGunshot(event.getEntity(), false);
    }

    @SubscribeEvent
    public static void onZombieDeath(LivingDeathEvent event) {
        if (!ENABLED.get() || !(event.getEntity() instanceof PathfinderMob zombie)
            || !(zombie.level() instanceof ServerLevel level)
            || !CityAmbientEncounter.isCityZombie(zombie)
            || zombie.getTags().contains("7dtc_horde_spawn")
            || !(event.getSource().getEntity() instanceof LivingEntity attacker)
            || (attacker instanceof Mob mob && CityAmbientEncounter.isCityZombie(mob))) return;
        BlockPos position = zombie.blockPosition();
        String sector = level.dimension().location() + ":" + (position.getX() >> 4) + ":" + (position.getZ() >> 4);
        long now = level.getGameTime();
        if (LAST_ZOMBIE_CRY.getOrDefault(sector, Long.MIN_VALUE / 2) + 60 > now) return;
        LAST_ZOMBIE_CRY.put(sector, now);
        if (LAST_ZOMBIE_CRY.size() > 1024)
            LAST_ZOMBIE_CRY.entrySet().removeIf(entry -> entry.getValue() + 1200 < now);
        alert(level, position, ZOMBIE_CRY_RADIUS.get(), Math.max(2, MAX_RESPONDERS.get() / 4));
    }

    static void onMilitiaScream(ServerLevel level, BlockPos position) {
        if (ENABLED.get()) alert(level, position, SCREAM_RADIUS.get(), Math.max(2, MAX_RESPONDERS.get() / 2));
    }

    private static void hearGunshot(LivingEntity shooter, boolean silenced) {
        if (!ENABLED.get() || !(shooter.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        if (shooter.getPersistentData().getLong(NEXT_GUNSHOT) > now) return;
        shooter.getPersistentData().putLong(NEXT_GUNSHOT, now + GUNSHOT_DEBOUNCE_TICKS);
        alert(level, shooter.blockPosition(), silenced ? SILENCED_RADIUS.get() : GUNSHOT_RADIUS.get(),
            MAX_RESPONDERS.get());
    }

    private static boolean suppressed(ItemStack weapon) {
        IGun gun = IGun.getIGunOrNull(weapon);
        if (gun == null) return false;
        ResourceLocation muzzle = gun.getAttachmentId(weapon, AttachmentType.MUZZLE);
        if (muzzle == null) muzzle = gun.getBuiltInAttachmentId(weapon, AttachmentType.MUZZLE);
        if (muzzle == null) return false;
        String path = muzzle.getPath().toLowerCase(Locale.ROOT);
        return path.contains("silenc") || path.contains("suppress");
    }

    private static void alert(ServerLevel level, BlockPos sound, int radius, int maxResponders) {
        if (radius <= 0) return;
        double maxDistanceSq = (double) radius * radius;
        AABB area = new AABB(sound).inflate(radius, Math.min(radius, 24), radius);
        List<PathfinderMob> nearby = level.getEntitiesOfClass(PathfinderMob.class, area,
            mob -> mob.isAlive() && CityAmbientEncounter.isCityZombie(mob)
                && !mob.getTags().contains("7dtc_horde_spawn")
                && mob.getTarget() == null && mob.distanceToSqr(sound.getX() + 0.5,
                    sound.getY() + 0.5, sound.getZ() + 0.5) <= maxDistanceSq);
        nearby.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(sound.getX() + 0.5,
            sound.getY() + 0.5, sound.getZ() + 0.5)));
        int count = Math.min(maxResponders, nearby.size());
        long until = level.getGameTime() + MEMORY_TICKS;
        for (int i = 0; i < count; i++) {
            PathfinderMob mob = nearby.get(i);
            mob.getPersistentData().putLong(UNTIL, until);
            mob.getPersistentData().putInt(X, sound.getX());
            mob.getPersistentData().putInt(Y, sound.getY());
            mob.getPersistentData().putInt(Z, sound.getZ());
        }
    }

    private static final class InvestigateSoundGoal extends Goal {
        private final PathfinderMob mob;

        private InvestigateSoundGoal(PathfinderMob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return active() && destination().distSqr(mob.blockPosition()) > 9.0D;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            navigate();
        }

        @Override
        public void tick() {
            if (mob.tickCount % 20 == 0) navigate();
        }

        @Override
        public void stop() {
            if (destination().distSqr(mob.blockPosition()) <= 9.0D)
                mob.getPersistentData().remove(UNTIL);
        }

        private boolean active() {
            return ENABLED.get() && mob.isAlive() && mob.getTarget() == null
                && !mob.getTags().contains("7dtc_horde_spawn")
                && mob.getPersistentData().getLong(UNTIL) > mob.level().getGameTime();
        }

        private BlockPos destination() {
            return new BlockPos(mob.getPersistentData().getInt(X),
                mob.getPersistentData().getInt(Y), mob.getPersistentData().getInt(Z));
        }

        private void navigate() {
            BlockPos destination = destination();
            mob.getNavigation().moveTo(destination.getX() + 0.5D,
                destination.getY(), destination.getZ() + 0.5D, 1.05D);
        }
    }
}
