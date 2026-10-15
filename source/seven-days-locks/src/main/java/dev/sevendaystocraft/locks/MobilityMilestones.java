package dev.sevendaystocraft.locks;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Set;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MobilityMilestones {
    private static final String PROTOCOL = "1";
    private static final String DOUBLE_JUMP_TAG = "7dtc_double_jump";
    private static final String LOW_PROFILE_TAG = "7dtc_low_profile";
    private static final String RIDER_TAG = "7dtc_beast_rider";
    private static final String COOLDOWN = "7dtc_double_jump_ready_at";
    private static final Set<String> ALEX_MOUNTS = Set.of(
        "alexsmobs:endergrade", "alexsmobs:komodo_dragon", "alexsmobs:tusklin", "alexsmobs:laviathan"
    );
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(SevenDaysLocks.MOD_ID, "mobility"))
        .networkProtocolVersion(() -> PROTOCOL)
        .clientAcceptedVersions(PROTOCOL::equals)
        .serverAcceptedVersions(PROTOCOL::equals)
        .simpleChannel();

    static {
        CHANNEL.registerMessage(0, DoubleJumpPacket.class, DoubleJumpPacket::encode, DoubleJumpPacket::decode, DoubleJumpPacket::handle);
    }

    private MobilityMilestones() {}

    @SubscribeEvent
    public static void reduceDetection(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(event.getNewTarget() instanceof ServerPlayer player)
            || !player.getTags().contains(LOW_PROFILE_TAG)) return;
        // Attacking a creature still reveals the player and allows retaliation.
        if (mob.getLastHurtByMob() == player) return;
        double range = 16.0;
        var attribute = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (attribute != null) range = attribute.getValue();
        // Low Profile reduces initial detection distance by 35%. It does not break
        // pursuit after the player has already been acquired at close range.
        if (mob.distanceToSqr(player) > range * range * 0.4225) event.setNewTarget(null);
    }

    @SubscribeEvent
    public static void gateAlexMounts(EntityMountEvent event) {
        if (!event.isMounting() || event.getLevel().isClientSide()
            || !(event.getEntityMounting() instanceof ServerPlayer player)
            || event.getEntityBeingMounted() == null) return;
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntityBeingMounted().getType());
        if (id != null && ALEX_MOUNTS.contains(id.toString()) && !player.getTags().contains(RIDER_TAG)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal("Beast Rider milestone required.").withStyle(ChatFormatting.GOLD), true);
        } else if (id != null && ALEX_MOUNTS.contains(id.toString())) {
            award(player, "training/beast_rider_mount");
        }
    }

    private record DoubleJumpPacket() {
        static void encode(DoubleJumpPacket packet, FriendlyByteBuf buffer) {}
        static DoubleJumpPacket decode(FriendlyByteBuf buffer) { return new DoubleJumpPacket(); }
        static void handle(DoubleJumpPacket packet, Supplier<NetworkEvent.Context> context) {
            ServerPlayer player = context.get().getSender();
            context.get().enqueueWork(() -> {
                if (player == null || !player.getTags().contains(DOUBLE_JUMP_TAG) || player.onGround()
                    || player.isPassenger() || player.isFallFlying() || player.isInWaterOrBubble()) return;
                long now = player.level().getGameTime();
                long ready = player.getPersistentData().getLong(COOLDOWN);
                if (now < ready) {
                    long seconds = (ready - now + 19) / 20;
                    player.displayClientMessage(Component.literal("Double Jump ready in " + seconds + "s").withStyle(ChatFormatting.GRAY), true);
                    return;
                }
                player.setDeltaMovement(player.getDeltaMovement().x, 0.62D, player.getDeltaMovement().z);
                player.hurtMarked = true;
                player.fallDistance = 0.0F;
                player.getPersistentData().putLong(COOLDOWN, now + 600L);
                award(player, "training/double_jump_use");
            });
            context.get().setPacketHandled(true);
        }
    }

    @Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ClientInput {
        private static boolean jumpWasDown;
        private static boolean leftGroundSinceJumpRelease;

        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.screen != null) { jumpWasDown = false; return; }
            boolean down = minecraft.options.keyJump.isDown();
            if (!minecraft.player.onGround() && !down) leftGroundSinceJumpRelease = true;
            if (minecraft.player.onGround()) leftGroundSinceJumpRelease = false;
            if (down && !jumpWasDown && leftGroundSinceJumpRelease) {
                CHANNEL.sendToServer(new DoubleJumpPacket());
                leftGroundSinceJumpRelease = false;
            }
            jumpWasDown = down;
        }
    }

    private static void award(ServerPlayer player, String path) {
        var advancement = player.server.getAdvancements().getAdvancement(new ResourceLocation("seven_days_to_craft", path));
        if (advancement == null) return;
        var progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) player.getAdvancements().award(advancement, criterion);
    }
}
