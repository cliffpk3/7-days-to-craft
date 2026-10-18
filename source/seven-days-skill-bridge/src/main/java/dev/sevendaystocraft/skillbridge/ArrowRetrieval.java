package dev.sevendaystocraft.skillbridge;

import com.tacz.guns.entity.EntityKineticBullet;
import dev.sevendaystocraft.locks.TaczAmmoTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/** Recovers arrows or the CGM ammunition used by curated TaCZ guns after a creature hit. */
@Mod.EventBusSubscriber(modid = SkillBridge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ArrowRetrieval {
    private static final String PROCESSED = "7dtc_arrow_retrieval_processed";
    private static final String PENDING = "7dtc_arrow_retrieval_pending";
    private static final String OWNER = "owner";
    private static final String EXPIRES = "expires";
    private static final String APPROACH = "approach";
    private static final double[] CHANCES = {0, 0.07, 0.14, 0.21, 0.28};

    private ArrowRetrieval() {}

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || event.getAmount() <= 0) return;
        if (event.getSource().getDirectEntity() instanceof EntityKineticBullet bullet) {
            recoverGunAmmo(victim, bullet);
            return;
        }
        if (!(event.getSource().getDirectEntity() instanceof Arrow arrow)
            || !(arrow.getOwner() instanceof ServerPlayer shooter)
            || victim == shooter || !ordinaryRecoverable(arrow)) return;

        // Piercing and repeated damage callbacks must never yield multiple arrows.
        if (arrow.getPersistentData().getBoolean(PROCESSED)) return;
        arrow.getPersistentData().putBoolean(PROCESSED, true);
        int rank = SkillBridge.getBasicSkillRank(shooter, "arrowretrieval", 4);
        boolean approach = rank > 0 && shooter.getRandom().nextDouble() < CHANCES[rank];
        boolean afterKill = shooter.getTags().contains("7dtc_embedded_arrows")
            && shooter.getRandom().nextDouble() < 0.30D;
        if (!approach && !afterKill) return;

        CompoundTag data = victim.getPersistentData();
        ListTag pending = data.getList(PENDING, Tag.TAG_COMPOUND);
        if (pending.size() >= 16) return;
        CompoundTag entry = new CompoundTag();
        entry.putUUID(OWNER, shooter.getUUID());
        entry.putLong(EXPIRES, victim.level().getGameTime() + 2400);
        entry.putBoolean(APPROACH, approach);
        pending.add(entry);
        data.put(PENDING, pending);
    }

    private static void recoverGunAmmo(LivingEntity victim, EntityKineticBullet bullet) {
        if (!(bullet.getOwner() instanceof ServerPlayer shooter) || shooter.isCreative()
            || victim == shooter || bullet.getPersistentData().getBoolean(PROCESSED)) return;
        bullet.getPersistentData().putBoolean(PROCESSED, true);
        int rank = SkillBridge.getBasicSkillRank(shooter, "arrowretrieval", 4);
        if (rank <= 0) return;
        String ammoId = TaczAmmoTypes.ammoId(bullet.getGunId().toString());
        if (ammoId.isEmpty()) return;

        // One shotgun blast can create several pellet entities. Roll only once
        // for pellets arriving within the same brief hit window.
        if (ammoId.equals("cgm:shell")) {
            CompoundTag data = shooter.getPersistentData();
            long now = shooter.level().getGameTime();
            if (now - data.getLong("7dtc_last_shell_retrieval_roll") < 5L) return;
            data.putLong("7dtc_last_shell_retrieval_roll", now);
        }
        if (shooter.getRandom().nextDouble() >= CHANCES[rank]) return;
        ItemStack recovered = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(ammoId)));
        if (!shooter.getInventory().add(recovered)) shooter.drop(recovered, false);
    }

    private static boolean ordinaryRecoverable(Arrow arrow) {
        return arrow.pickup == AbstractArrow.Pickup.ALLOWED
            && arrow.getPierceLevel() == 0 && arrow.getColor() == -1;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
            || player.tickCount % 10 != 0) return;
        AABB area = player.getBoundingBox().inflate(2.5);
        for (LivingEntity victim : player.serverLevel().getEntitiesOfClass(LivingEntity.class, area,
            entity -> entity != player && entity.getPersistentData().contains(PENDING, Tag.TAG_LIST))) {
            CompoundTag data = victim.getPersistentData();
            ListTag pending = data.getList(PENDING, Tag.TAG_COMPOUND);
            ListTag remaining = new ListTag();
            for (int i = 0; i < pending.size(); i++) {
                CompoundTag entry = pending.getCompound(i);
                if (entry.getLong(EXPIRES) < player.level().getGameTime()) continue;
                if (entry.hasUUID(OWNER) && entry.getUUID(OWNER).equals(player.getUUID())
                    && (!entry.contains(APPROACH) || entry.getBoolean(APPROACH))) {
                    ItemStack arrow = new ItemStack(Items.ARROW);
                    if (!player.getInventory().add(arrow)) {
                        remaining.add(entry); // Retry when inventory space is available.
                    }
                } else remaining.add(entry);
            }
            if (remaining.isEmpty()) data.remove(PENDING);
            else data.put(PENDING, remaining);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        CompoundTag data = victim.getPersistentData();
        ListTag pending = data.getList(PENDING, Tag.TAG_COMPOUND);
        for (int i = 0; i < pending.size(); i++) {
            CompoundTag entry = pending.getCompound(i);
            if (!entry.hasUUID(OWNER) || entry.getLong(EXPIRES) < victim.level().getGameTime()) continue;
            ItemEntity drop = new ItemEntity(victim.level(), victim.getX(), victim.getY() + 0.3,
                victim.getZ(), new ItemStack(Items.ARROW));
            drop.getPersistentData().putUUID(OWNER, entry.getUUID(OWNER));
            drop.getPersistentData().putLong(EXPIRES, victim.level().getGameTime() + 600);
            victim.level().addFreshEntity(drop);
        }
        data.remove(PENDING);
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        ItemEntity item = event.getItem();
        CompoundTag data = item.getPersistentData();
        if (!data.hasUUID(OWNER) || data.getLong(EXPIRES) < item.level().getGameTime()) return;
        UUID owner = data.getUUID(OWNER);
        if (!owner.equals(event.getEntity().getUUID())) event.setCanceled(true);
    }
}
