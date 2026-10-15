package dev.sevendaystocraft.locks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DeathRecovery {
    private static final String LOST_HEALTH = "7dtc_lost_health";
    private static final int HEALTH_PER_DEATH = 2;
    private static final int MAX_LOST_HEALTH = 6;
    private static final UUID PENALTY_ID = UUID.fromString("775dc85d-e445-4c63-9c0e-5881bcb13e91");

    private DeathRecovery() {
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int lost = Math.min(MAX_LOST_HEALTH, getLostHealth(player) + HEALTH_PER_DEATH);
        setLostHealth(player, lost);
        award(player, new ResourceLocation("seven_days_to_craft", "medical/first_death"));
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        setLostHealth(event.getEntity(), getLostHealth(event.getOriginal()));
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        moveOffTreeCanopy(player);
        applyPenalty(player);
        int graveRunnerBonus = player.getTags().contains("7dtc_grave_runner") ? 60 * 20 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 20 + graveRunnerBonus, 2, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 10 * 20 + graveRunnerBonus, 1, false, true));
        BuiltInRegistries.MOB_EFFECT.getOptional(new ResourceLocation("hordes", "immunity"))
            .ifPresent(effect -> player.addEffect(new MobEffectInstance(effect, 30 * 20 + graveRunnerBonus, 0, false, true)));
        player.displayClientMessage(Component.translatable("message.seven_days_locks.death_penalty", getLostHealth(player) / 2, MAX_LOST_HEALTH / 2)
            .withStyle(ChatFormatting.RED), false);
    }

    /**
     * Heightmaps can choose leaf canopies as a world's shared spawn surface. We only
     * intervene when the actual respawn is touching leaves; beds, anchors and ordinary
     * ground keep their exact positions. The replacement uses the nearest solid surface
     * while explicitly ignoring leaves and other unsafe landing blocks.
     */
    private static void moveOffTreeCanopy(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos origin = player.blockPosition();
        if (!level.getBlockState(origin).is(BlockTags.LEAVES)
            && !level.getBlockState(origin.above()).is(BlockTags.LEAVES)
            && !level.getBlockState(origin.below()).is(BlockTags.LEAVES)) return;

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int radius = 0; radius <= 16; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        origin.getX() + dx, origin.getZ() + dz);
                    BlockPos candidate = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
                    if (!isSafeRespawn(level, candidate)) continue;
                    double distance = dx * dx + dz * dz;
                    if (distance < bestDistance) {
                        best = candidate;
                        bestDistance = distance;
                    }
                }
            }
            if (best != null) break;
        }
        if (best != null) player.teleportTo(level, best.getX() + 0.5D, best.getY(), best.getZ() + 0.5D,
            player.getYRot(), player.getXRot());
    }

    private static boolean isSafeRespawn(ServerLevel level, BlockPos feet) {
        var support = level.getBlockState(feet.below());
        if (support.isAir() || support.is(BlockTags.LEAVES) || support.is(BlockTags.LOGS)
            || support.is(Blocks.CACTUS) || support.is(Blocks.MAGMA_BLOCK)
            || support.is(Blocks.LAVA) || support.is(Blocks.POWDER_SNOW)
            || !support.isCollisionShapeFullBlock(level, feet.below())) return false;
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
            && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
            && level.getWorldBorder().isWithinBounds(feet);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) applyPenalty(player);
    }

    @SubscribeEvent
    public static void onGoldenApple(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!event.getItem().is(Items.GOLDEN_APPLE) && !event.getItem().is(Items.ENCHANTED_GOLDEN_APPLE)) return;
        int lost = getLostHealth(player);
        if (lost <= 0) return;

        int recoveryAmount = event.getItem().is(Items.GOLDEN_APPLE) && player.getTags().contains("7dtc_field_recovery") ? 4 : 2;
        int restored = Math.min(recoveryAmount, lost);
        setLostHealth(player, lost - restored);
        applyPenalty(player);
        player.heal(restored);
        award(player, new ResourceLocation("seven_days_to_craft", "medical/recover_heart"));
        player.displayClientMessage(Component.translatable("message.seven_days_locks.heart_restored", restored / 2, (lost - restored) / 2, MAX_LOST_HEALTH / 2)
            .withStyle(ChatFormatting.GOLD), false);
    }

    private static int getLostHealth(net.minecraft.world.entity.player.Player player) {
        return Math.max(0, Math.min(MAX_LOST_HEALTH, player.getPersistentData().getInt(LOST_HEALTH)));
    }

    private static void setLostHealth(net.minecraft.world.entity.player.Player player, int value) {
        player.getPersistentData().putInt(LOST_HEALTH, Math.max(0, Math.min(MAX_LOST_HEALTH, value)));
    }

    private static void applyPenalty(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) return;
        health.removeModifier(PENALTY_ID);
        int lost = getLostHealth(player);
        if (lost > 0) {
            health.addPermanentModifier(new AttributeModifier(PENALTY_ID, "7 Days death penalty", -lost, AttributeModifier.Operation.ADDITION));
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }

    private static void award(ServerPlayer player, ResourceLocation id) {
        var advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement == null) return;
        var progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) player.getAdvancements().award(advancement, criterion);
    }

    @Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void tooltip(ItemTooltipEvent event) {
            if (event.getItemStack().is(Items.GOLDEN_APPLE) || event.getItemStack().is(Items.ENCHANTED_GOLDEN_APPLE)) {
                boolean enhanced = event.getItemStack().is(Items.GOLDEN_APPLE)
                    && event.getEntity() != null
                    && event.getEntity().getTags().contains("7dtc_field_recovery");
                String tooltip = enhanced ? "tooltip.seven_days_locks.restores_two_hearts" : "tooltip.seven_days_locks.restores_heart";
                event.getToolTip().add(Component.translatable(tooltip).withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
