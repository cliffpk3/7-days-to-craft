package dev.sevendaystocraft.skillbridge;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Concrete effects for the three Scavenging skills that previously existed only
 * as Puffish definitions. Every bonus is a separate roll and deliberately avoids
 * feeding Fortune, Silk Touch, player farms, or reopenable containers.
 */
@Mod.EventBusSubscriber(modid = SkillBridge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScavengingSkills {
    private static final String FORAGER_DATA = "seven_days_forager_placed";
    private static final String SCAVENGER_PROCESSED = "7dtc_scavenger_processed";
    private static final String PENDING_POS = "7dtc_scavenger_pending_pos";
    private static final String PENDING_TICK = "7dtc_scavenger_pending_tick";
    private static final String PENDING_DIMENSION = "7dtc_scavenger_pending_dimension";

    private ScavengingSkills() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onOreBroken(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)
            || !(event.getPlayer() instanceof ServerPlayer player) || player.getAbilities().instabuild) return;

        int rank = SkillBridge.getBasicSkillRank(player, "prospector", 5);
        if (rank <= 0 || player.getRandom().nextDouble() >= rank * 0.02D) return;

        ItemStack tool = player.getMainHandItem();
        BlockState state = event.getState();
        if (!tool.isCorrectToolForDrops(state)
            || EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) return;

        Item bonus = oreBonus(state);
        if (bonus == null) return;

        Block.popResource(level, event.getPos(), new ItemStack(bonus));
        player.displayClientMessage(Component.literal("Prospector: +1 ")
            .append(bonus.getDescription()).append(".").withStyle(ChatFormatting.GREEN), true);
    }

    private static Item oreBonus(BlockState state) {
        if (state.is(BlockTags.IRON_ORES)) return Items.RAW_IRON;
        if (state.is(BlockTags.COPPER_ORES)) return Items.RAW_COPPER;
        if (state.is(BlockTags.GOLD_ORES)) return state.is(Blocks.NETHER_GOLD_ORE) ? Items.GOLD_NUGGET : Items.RAW_GOLD;
        if (state.is(BlockTags.COAL_ORES)) return Items.COAL;
        if (state.is(BlockTags.DIAMOND_ORES)) return Items.DIAMOND;
        if (state.is(BlockTags.EMERALD_ORES)) return Items.EMERALD;
        if (state.is(BlockTags.LAPIS_ORES)) return Items.LAPIS_LAZULI;
        if (state.is(BlockTags.REDSTONE_ORES)) return Items.REDSTONE;
        if (state.is(Blocks.NETHER_QUARTZ_ORE)) return Items.QUARTZ;
        return null;
    }

    @SubscribeEvent
    public static void rememberPlacedFood(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer)) return;
        if (event.getPlacedBlock().is(Blocks.SWEET_BERRY_BUSH)) PlacedFoodData.get(level).add(event.getPos());
    }

    @SubscribeEvent
    public static void forgetBrokenFood(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.getState().is(Blocks.SWEET_BERRY_BUSH)) return;
        PlacedFoodData.get(level).remove(event.getPos());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onWildFoodGathered(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)
            || event.getHand() != InteractionHand.MAIN_HAND) return;

        BlockState state = level.getBlockState(event.getPos());
        if (!state.is(Blocks.SWEET_BERRY_BUSH)
            || state.getValue(SweetBerryBushBlock.AGE) < SweetBerryBushBlock.MAX_AGE
            || PlacedFoodData.get(level).contains(event.getPos())) return;

        var data = player.getPersistentData();
        long now = level.getGameTime();
        if (data.getLong("7dtc_forager_tick") == now
            && data.getLong("7dtc_forager_pos") == event.getPos().asLong()) return;
        data.putLong("7dtc_forager_tick", now);
        data.putLong("7dtc_forager_pos", event.getPos().asLong());

        int rank = SkillBridge.getBasicSkillRank(player, "forager", 9);
        if (rank <= 0 || player.getRandom().nextDouble() >= rank * 0.025D) return;

        Block.popResource(level, event.getPos(), new ItemStack(Items.SWEET_BERRIES));
        player.displayClientMessage(Component.literal("Forager: +1 Sweet Berries from a wild bush.")
            .withStyle(ChatFormatting.GREEN), true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void rememberGeneratedContainer(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        int rank = SkillBridge.getBasicSkillRank(player, "scavenger", 5);
        if (rank <= 0) return;

        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (!(blockEntity instanceof RandomizableContainerBlockEntity container)
            || container.getPersistentData().getBoolean(SCAVENGER_PROCESSED)
            || excludedContainer(container)) return;

        // Only generated loot containers are eligible. Player storage never gains
        // a free roll, even if another player is the first person to open it.
        String lootTable = container.saveWithFullMetadata().getString("LootTable");
        if (lootTable.isBlank()) return;

        var data = player.getPersistentData();
        data.putLong(PENDING_POS, event.getPos().asLong());
        data.putLong(PENDING_TICK, level.getGameTime());
        data.putString(PENDING_DIMENSION, level.dimension().location().toString());
    }

    @SubscribeEvent
    public static void onGeneratedContainerOpened(PlayerContainerEvent.Open event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var data = player.getPersistentData();
        long pendingTick = data.getLong(PENDING_TICK);
        if (pendingTick <= 0L || player.level().getGameTime() - pendingTick > 10L
            || !data.getString(PENDING_DIMENSION).equals(player.level().dimension().location().toString())) {
            clearPending(data);
            return;
        }

        BlockPos pos = BlockPos.of(data.getLong(PENDING_POS));
        clearPending(data);
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof RandomizableContainerBlockEntity container)
            || container.getPersistentData().getBoolean(SCAVENGER_PROCESSED)
            || excludedContainer(container)) return;

        markProcessed(container);
        RandomizableContainerBlockEntity pair = paired(container);
        if (pair != null) markProcessed(pair);

        int rank = SkillBridge.getBasicSkillRank(player, "scavenger", 5);
        if (rank <= 0 || player.getRandom().nextDouble() >= rank * 0.03D) return;

        List<Integer> eligible = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (eligibleContainerStack(container.getItem(slot))) eligible.add(slot);
        }
        if (pair != null) {
            int offset = container.getContainerSize();
            for (int slot = 0; slot < pair.getContainerSize(); slot++) {
                if (eligibleContainerStack(pair.getItem(slot))) eligible.add(offset + slot);
            }
        }
        if (eligible.isEmpty()) return;

        int encoded = eligible.get(player.getRandom().nextInt(eligible.size()));
        RandomizableContainerBlockEntity target = encoded < container.getContainerSize() ? container : pair;
        int slot = encoded < container.getContainerSize() ? encoded : encoded - container.getContainerSize();
        ItemStack source = target.getItem(slot);
        ItemStack bonus = source.copyWithCount(1);
        if (source.getCount() < source.getMaxStackSize()) {
            source.grow(1);
            target.setChanged();
        } else if (!insertOne(target, bonus)) {
            ItemEntity dropped = player.drop(bonus, false);
            if (dropped != null) dropped.setNoPickUpDelay();
        }
        player.displayClientMessage(Component.literal("Scavenger: +1 ")
            .append(bonus.getHoverName()).append(" from an unexplored container.")
            .withStyle(ChatFormatting.GREEN), true);
    }

    private static boolean insertOne(RandomizableContainerBlockEntity container, ItemStack stack) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                container.setItem(slot, stack);
                container.setChanged();
                return true;
            }
        }
        return false;
    }

    private static boolean eligibleContainerStack(ItemStack stack) {
        if (stack.isEmpty() || stack.getMaxStackSize() <= 1 || stack.isDamageableItem() || stack.hasTag()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return false;
        String namespace = id.getNamespace();
        return !namespace.equals("artifacts") && !namespace.equals("celestial_artifacts")
            && !namespace.equals("nameless_trinkets") && !namespace.equals("wrd")
            && !namespace.equals("divine_weaponry") && !namespace.equals("cgm")
            && !namespace.equals("scguns");
    }

    private static boolean excludedContainer(RandomizableContainerBlockEntity container) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(container.getBlockState().getBlock());
        if (id == null) return true;
        if (id.getNamespace().equals("wrd")) return true;
        String value = id.toString();
        return value.equals("quark:ancient_chest") || value.equals("quark:acacia_chest")
            || value.equals("quark:prismarine_chest") || value.equals("quark:nether_brick_chest");
    }

    private static void markProcessed(RandomizableContainerBlockEntity container) {
        container.getPersistentData().putBoolean(SCAVENGER_PROCESSED, true);
        container.setChanged();
    }

    private static RandomizableContainerBlockEntity paired(RandomizableContainerBlockEntity container) {
        if (container.getLevel() == null || !(container.getBlockState().getBlock() instanceof ChestBlock)
            || !container.getBlockState().hasProperty(ChestBlock.TYPE)
            || container.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE) return null;
        BlockPos otherPos = container.getBlockPos().relative(ChestBlock.getConnectedDirection(container.getBlockState()));
        BlockEntity other = container.getLevel().getBlockEntity(otherPos);
        return other instanceof RandomizableContainerBlockEntity paired ? paired : null;
    }

    private static void clearPending(CompoundTag data) {
        data.remove(PENDING_POS);
        data.remove(PENDING_TICK);
        data.remove(PENDING_DIMENSION);
    }

    private static final class PlacedFoodData extends SavedData {
        private final Set<Long> positions = new HashSet<>();

        static PlacedFoodData get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(PlacedFoodData::load, PlacedFoodData::new, FORAGER_DATA);
        }

        static PlacedFoodData load(CompoundTag tag) {
            PlacedFoodData data = new PlacedFoodData();
            for (long position : tag.getLongArray("Positions")) data.positions.add(position);
            return data;
        }

        boolean contains(BlockPos pos) { return positions.contains(pos.asLong()); }

        void add(BlockPos pos) {
            if (positions.add(pos.asLong())) setDirty();
        }

        void remove(BlockPos pos) {
            if (positions.remove(pos.asLong())) setDirty();
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putLongArray("Positions", positions.stream().mapToLong(Long::longValue).toArray());
            return tag;
        }
    }
}
