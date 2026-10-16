package dev.sevendaystocraft.locks;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import com.mojang.serialization.Codec;
import net.smileycorp.hordes.common.event.HordeEndEvent;
import java.util.Locale;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod(SevenDaysLocks.MOD_ID)
public final class SevenDaysLocks {
    public static final String MOD_ID = "seven_days_locks";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, MOD_ID);
    public static final RegistryObject<Feature<?>> DISTANCE_SCALED_ORE = FEATURES.register(
        "distance_scaled_ore", () -> new DistanceScaledOreFeature(OreConfiguration.CODEC));
    public static final Map<String, RegistryObject<Item>> GUN_BLUEPRINTS = registerGunBlueprints();
    public static final Map<String, RegistryObject<Item>> DIVINE_BLUEPRINTS = registerDivineBlueprints();
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);
    public static final RegistryObject<Codec<ArtifactLootFilterModifier>> ARTIFACT_LOOT_FILTER =
        LOOT_MODIFIERS.register("artifact_loot_filter", () -> ArtifactLootFilterModifier.CODEC);
    public static final RegistryObject<Item> TRAINING_CACHE = ITEMS.register("training_cache", () -> new TrainingCacheItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MIMIC_CACHE_2 = ITEMS.register("mimic_cache_2", () -> new MimicCacheItem(2, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MIMIC_CACHE_3 = ITEMS.register("mimic_cache_3", () -> new MimicCacheItem(3, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MIMIC_CACHE_4 = ITEMS.register("mimic_cache_4", () -> new MimicCacheItem(4, new Item.Properties().stacksTo(1)));
    private static Map<String, RegistryObject<Item>> registerGunBlueprints() {
        Map<String, RegistryObject<Item>> result = new LinkedHashMap<>();
        for (String weapon : new String[] {
            "pistol", "machine_pistol", "rifle", "shotgun", "assault_rifle", "heavy_rifle",
            "grenade_launcher", "mini_gun", "bazooka", "blood_sprayer", "stink_ray", "hemolymph_blaster"
        }) {
            result.put(weapon, ITEMS.register("blueprint_" + weapon, () -> new WeaponBlueprintItem("gun", weapon)));
        }
        return Map.copyOf(result);
    }
    private static Map<String, RegistryObject<Item>> registerDivineBlueprints() {
        Map<String, RegistryObject<Item>> result = new LinkedHashMap<>();
        for (String material : new String[] {"iron", "diamond", "netherite"}) {
            for (String type : new String[] {"battleaxe", "cutlass", "dagger", "glaive", "hammer", "katana",
                    "longsword", "rapier", "scythe", "shuriken", "sickle", "spear"}) {
                String weapon = material + "_" + type;
                result.put(weapon, ITEMS.register("blueprint_" + weapon,
                    () -> new WeaponBlueprintItem("divine", weapon)));
            }
        }
        return Map.copyOf(result);
    }
    private static final String LOCK_TIER = "7dtc_lock_tier";
    private static final String LOCK_ROLLED = "7dtc_lock_rolled";
    private static final String LOCK_OPENED = "7dtc_lock_opened";
    private static final String SOURCE_LOOT_TABLE = "7dtc_source_loot_table";
    private static final String SETTLEMENT_FREE = "7dtc_settlement_free";
    private static final String DEEP_COMMON_CANDIDATE = "7dtc_deep_common_candidate";
    private static final TagKey<Structure> SETTLEMENTS = TagKey.create(
        Registries.STRUCTURE, new ResourceLocation(MOD_ID, "settlements")
    );
    private static final String[] KEYS = {"", "wrd:keystrongholdchest_iron", "wrd:keystrongholdchest_gold", "wrd:keystrongholdchest_diamond", "wrd:keystrongholdchest_treasure"};
    private static final String[] TIER_CHESTS = {"minecraft:chest", "quark:ancient_chest", "quark:acacia_chest", "quark:prismarine_chest", "quark:nether_brick_chest"};
    static final String[][] RANK_ITEMS = {
        {},
        {"artifacts:plastic_drinking_hat", "artifacts:novely_drinking_hat", "artifacts:snorkel", "artifacts:lucky_scarf", "artifacts:panic_necklace", "artifacts:golden_hook", "artifacts:cross_necklace", "artifacts:flippers", "celestial_artifacts:gold_ring", "celestial_artifacts:netherite_ring", "celestial_artifacts:nether_fire", "celestial_artifacts:gluttony_badge", "celestial_artifacts:precious_bracelet", "celestial_artifacts:red_ruby_bracelet", "celestial_artifacts:spirit_bracelet", "celestial_artifacts:cross_necklace", "celestial_artifacts:precious_necklace", "celestial_artifacts:holy_necklace", "celestial_artifacts:red_heart_necklace", "celestial_artifacts:spirit_necklace", "celestial_artifacts:evil_eye", "nameless_trinkets:experience_battery", "nameless_trinkets:experience_magnet", "nameless_trinkets:what_magnet", "nameless_trinkets:explosion_proof_jacket", "nameless_trinkets:creeper_sense", "nameless_trinkets:fractured_nullstone", "nameless_trinkets:miners_soul"},
        {"artifacts:umbrella", "artifacts:thorn_pendant", "artifacts:charm_of_sinking", "artifacts:obsidian_skull", "artifacts:antidote_vessel", "artifacts:digging_claws", "artifacts:feral_claws", "artifacts:pocket_piston", "artifacts:aqua_dashers", "artifacts:steadfast_spikes", "artifacts:rooted_boots", "celestial_artifacts:gold_ring", "celestial_artifacts:holy_sword", "celestial_artifacts:sands_talisman", "celestial_artifacts:skywalker_scroll", "celestial_artifacts:charming_bracelet", "celestial_artifacts:star_necklace", "celestial_artifacts:gallop_necklace", "celestial_artifacts:fang_necklace", "celestial_artifacts:sea_god_crown", "celestial_artifacts:guardian_eye", "celestial_artifacts:magic_arrow_bag", "celestial_artifacts:spirit_crown", "celestial_artifacts:flame_arrow_bag", "celestial_artifacts:iron_scabbard", "celestial_artifacts:leech_scabbard", "celestial_artifacts:repent_mirror", "celestial_artifacts:backtrack_mirror", "nameless_trinkets:callus", "nameless_trinkets:puffer_fish_liver", "nameless_trinkets:blaze_nucleus", "nameless_trinkets:ice_cube", "nameless_trinkets:amphibious_hands", "nameless_trinkets:spider_legs", "nameless_trinkets:scarab_amulet", "nameless_trinkets:dragons_eye", "nameless_trinkets:four_leaf_clover", "nameless_trinkets:experience_battery"},
        {"artifacts:everlasting_beef", "artifacts:eternal_steak", "artifacts:shock_pendant", "artifacts:flame_pendant", "artifacts:cloud_in_a_bottle", "artifacts:crystal_heart", "artifacts:helium_flamingo", "artifacts:chorus_totem", "artifacts:power_glove", "artifacts:fire_gauntlet", "artifacts:vampiric_glove", "artifacts:pickaxe_heater", "artifacts:running_shoes", "celestial_artifacts:magic_horseshoe", "celestial_artifacts:heart_of_revenge", "celestial_artifacts:life_bracelet", "celestial_artifacts:prayer_crown", "celestial_artifacts:titan_scabbard", "nameless_trinkets:reverse_card", "nameless_trinkets:broken_magnet", "nameless_trinkets:super_magnet", "nameless_trinkets:lucky_rock", "nameless_trinkets:rage_mind", "nameless_trinkets:cracked_crown", "nameless_trinkets:sigil_of_baphomet", "nameless_trinkets:tear_of_the_sea", "nameless_trinkets:true_heart_of_the_sea", "nameless_trinkets:moon_stone", "nameless_trinkets:sleeping_pills", "nameless_trinkets:ethereal_wings", "nameless_trinkets:reforger", "nameless_trinkets:light_gloves", "nameless_trinkets:nelumbo", "nameless_trinkets:dark_nelumbo"},
        {"artifacts:night_vision_goggles", "artifacts:scarf_of_invisibility", "artifacts:bunny_hoppers", "celestial_artifacts:amethyst_ring", "celestial_artifacts:flight_ring", "celestial_artifacts:holy_talisman", "celestial_artifacts:angel_heart", "celestial_artifacts:angel_pearl", "celestial_artifacts:knight_shelter", "celestial_artifacts:solar_magnet", "celestial_artifacts:bearing_stamen", "celestial_artifacts:earth_axe", "celestial_artifacts:earth_pickaxe", "celestial_artifacts:earth_shovel", "nameless_trinkets:missing_page", "nameless_trinkets:broken_ankh", "nameless_trinkets:speed_force", "nameless_trinkets:vampire_blood", "nameless_trinkets:blindfold", "nameless_trinkets:ghast_eye", "nameless_trinkets:wooden_stick", "nameless_trinkets:gods_crown", "nameless_trinkets:fragile_cloud"},
    };
    private static final String[] ONBOARDING_AMULETS = {
        "artifacts:panic_necklace",
        "artifacts:cross_necklace",
        "celestial_artifacts:cross_necklace",
        "celestial_artifacts:precious_necklace",
        "celestial_artifacts:red_heart_necklace",
        "celestial_artifacts:spirit_necklace"
    };

    public SevenDaysLocks() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        FEATURES.register(FMLJavaModLoadingContext.get().getModEventBus());
        LOOT_MODIFIERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ThreatSense.init();
        CompassIntegration.init();
        HordeCountdown.init();
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        private record PendingChest(ServerLevel level, BlockPos pos) {}
        private static final ArrayDeque<PendingChest> PENDING_CHESTS = new ArrayDeque<>();

        @SubscribeEvent
        public static void onOpen(PlayerInteractEvent.RightClickBlock event) {
            if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) return;
            handleNativeWrdChest(event, player);
            BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
            if (!(be instanceof RandomizableContainerBlockEntity)) return;
            RandomizableContainerBlockEntity container = (RandomizableContainerBlockEntity) be;
            int visualTier = tierFromBlock(container);
            RandomizableContainerBlockEntity paired = paired(container);
            if (visualTier <= 0) {
                // Also repairs containers generated before the global loot filter
                // was installed. Ordinary barrels/chests may keep all normal loot,
                // but never an artifact reserved for the keyed reward system.
                container.unpackLootTable(player);
                sanitize(container);
                if (paired != null) {
                    paired.unpackLootTable(player);
                    sanitize(paired);
                }
                return;
            }
            CompoundTag persistent = be.getPersistentData();
            String sourceLootTable = persistent.getString(SOURCE_LOOT_TABLE);
            if (wasOpened(container) || (paired != null && wasOpened(paired))) {
                // Also repairs chests opened by older builds: they already have
                // their one-time rank reward, but used to be locked on every click.
                unlock(container);
                if (paired != null) unlock(paired);
            } else {
                markLock(container, visualTier);
                if (paired != null) markLock(paired, visualTier);
            }
            container.unpackLootTable(player);
            sanitize(container);
            if (paired != null) { paired.unpackLootTable(player); sanitize(paired); }
            int tier = persistent.getInt(LOCK_TIER);
            if (tier <= 0 || tier >= KEYS.length) return;
            Item required = BuiltInRegistries.ITEM.get(new ResourceLocation(KEYS[tier]));
            InteractionHand hand = findKey(player, required);
            if (hand == null) {
                player.displayClientMessage(Component.translatable("message.seven_days_locks.locked." + tier).withStyle(ChatFormatting.GOLD), true);
                event.setCanceled(true);
                event.setUseBlock(Event.Result.DENY);
                return;
            }
            boolean preservedKey = !player.getAbilities().instabuild
                && player.getTags().contains("7dtc_key_conservation")
                && player.getRandom().nextFloat() < 0.30F;
            if (!player.getAbilities().instabuild && !preservedKey) player.getItemInHand(hand).shrink(1);
            if (preservedKey) {
                player.displayClientMessage(Component.literal("Key Conservation preserved your key!").withStyle(ChatFormatting.AQUA), true);
            }
            addRankReward(container, paired, tier, sourceLootTable);
            addTierSupplies(container, paired, tier);
            unlock(container);
            if (paired != null) unlock(paired);
            if (tier == 1) award(player, new ResourceLocation("seven_days_to_craft", "onboarding/open_tier_1_cache"));
            if (sourceLootTable.startsWith("wrd:")) {
                award(player, new ResourceLocation("seven_days_to_craft", "wrd/open_locked_chest"));
                if (isWrdCompletionLoot(sourceLootTable)) award(player, new ResourceLocation("seven_days_to_craft", "wrd/claim_treasure"));
            }
            if (player.getTags().contains("7dtc_key_conservation")) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/key_conservation_open"));
            }
            player.displayClientMessage(Component.translatable("message.seven_days_locks.opened", tier).withStyle(ChatFormatting.GREEN), true);
        }

        private static void handleNativeWrdChest(PlayerInteractEvent.RightClickBlock event, ServerPlayer player) {
            BlockPos pos = event.getPos().immutable();
            ResourceLocation before = BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(pos).getBlock());
            if (before == null || !before.getNamespace().equals("wrd") || !before.getPath().contains("chest") || before.getPath().contains("open")) return;
            player.server.execute(() -> {
                ResourceLocation after = BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(pos).getBlock());
                if (after == null || !after.getNamespace().equals("wrd") || (!after.getPath().contains("open") && after.equals(before))) return;
                award(player, new ResourceLocation("seven_days_to_craft", "wrd/open_locked_chest"));
                if (player.getTags().contains("7dtc_key_conservation")) {
                    award(player, new ResourceLocation("seven_days_to_craft", "training/key_conservation_open"));
                }
                int rank = nativeWrdCompletionRank(before.getPath());
                if (rank <= 0) return;
                award(player, new ResourceLocation("seven_days_to_craft", "wrd/claim_treasure"));
                String[] choices = RANK_ITEMS[rank];
                if (choices.length == 0) return;
                ItemStack reward = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(choices[player.getRandom().nextInt(choices.length)])));
                if (!player.getInventory().add(reward)) player.drop(reward, false);
                player.displayClientMessage(Component.literal("Dungeon completion reward: Rank " + rank + " artifact").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            });
        }

        private static int nativeWrdCompletionRank(String path) {
            if (path.contains("closed_lost_treasure") || path.contains("stronghold_treasure")) return 4;
            if (path.contains("bone_chest")) return 3;
            if (path.contains("treasure_chest")) return 2;
            return 0;
        }

        @SubscribeEvent
        public static void onChunkLoad(ChunkEvent.Load event) {
            if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
            // Chest blocks query their neighbours when their state changes. Doing
            // that from ChunkEvent.Load can request the very chunk whose load future
            // is still completing. Queue the work for normal server ticks instead.
            for (BlockEntity blockEntity : new ArrayList<>(chunk.getBlockEntities().values())) {
                if (blockEntity instanceof RandomizableContainerBlockEntity container
                    && container.getBlockState().getBlock() instanceof ChestBlock
                    && !container.saveWithFullMetadata().getString("LootTable").isBlank()) {
                    PENDING_CHESTS.addLast(new PendingChest(level, container.getBlockPos().immutable()));
                }
            }
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            int remaining = Math.min(32, PENDING_CHESTS.size());
            while (remaining-- > 0) {
                PendingChest pending = PENDING_CHESTS.removeFirst();
                if (!pending.level().hasChunkAt(pending.pos())) continue;
                BlockEntity blockEntity = pending.level().getBlockEntity(pending.pos());
                if (blockEntity instanceof RandomizableContainerBlockEntity container) {
                    visualizeLootChest(pending.level(), container);
                }
            }
        }

        @SubscribeEvent
        public static void onReservedChestPlaced(BlockEvent.EntityPlaceEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) return;
            int tier = tierFromBlock(event.getPlacedBlock());
            if (tier <= 0) return;
            // Forge dispatches EntityPlaceEvent after Item#useOn returns. The
            // onboarding item has already marked its newly placed chest by then.
            // Only that marked Level 1 chest may bypass the placement guard.
            if (tier == 1 && event.getLevel().getBlockEntity(event.getPos()) instanceof RandomizableContainerBlockEntity chest
                && chest.getPersistentData().getBoolean("7dtc_onboarding_cache")
                && chest.getPersistentData().getInt(LOCK_TIER) == 1) return;
            if (tier >= 2 && event.getLevel().getBlockEntity(event.getPos()) instanceof RandomizableContainerBlockEntity chest
                && chest.getPersistentData().getBoolean("7dtc_mimic_cache")
                && chest.getPersistentData().getInt(LOCK_TIER) == tier) return;
            event.setCanceled(true);
            player.displayClientMessage(Component.literal("Tiered loot chests can only be generated by structures.").withStyle(ChatFormatting.RED), true);
        }

        @SubscribeEvent
        public static void onReservedChestBroken(BlockEvent.BreakEvent event) {
            if (event.getLevel().isClientSide() || tierFromBlock(event.getState()) <= 0) return;
            event.setCanceled(true);
            event.getPlayer().displayClientMessage(Component.literal("Tiered loot chests cannot be collected.").withStyle(ChatFormatting.RED), true);
        }

        @SubscribeEvent
        public static void onExplosion(ExplosionEvent.Detonate event) {
            if (event.getLevel().isClientSide()) return;
            event.getAffectedBlocks().removeIf(pos -> tierFromBlock(event.getLevel().getBlockState(pos)) > 0);
        }

        @SubscribeEvent
        public static void onPiston(PistonEvent.Pre event) {
            if (event.getLevel().isClientSide()) return;
            BlockPos cursor = event.getPos().relative(event.getDirection());
            for (int i = 0; i < 13; i++, cursor = cursor.relative(event.getDirection())) {
                var state = event.getLevel().getBlockState(cursor);
                if (state.isAir()) break;
                if (tierFromBlock(state) > 0) {
                    event.setCanceled(true);
                    return;
                }
            }
        }

        @SubscribeEvent
        public static void onDrops(LivingDropsEvent event) {
            LivingEntity mob = event.getEntity();
            if (mob.level().isClientSide()) return;
            if (mob instanceof IronGolem golem) {
                // Village golems become a deliberate combat reward. Requiring a
                // player kill and excluding player-built golems prevents passive
                // farms from becoming unlimited Divine Weaponry generators.
                if (!golem.isPlayerCreated() && event.getSource().getEntity() instanceof ServerPlayer) {
                    DivineWeaponDrops.dropGuaranteedIron(event);
                }
                return;
            }
            // Curated cave encounters use the same threat-tier loot even if a
            // mod's hostile mob does not implement vanilla's Enemy marker.
            if (!(mob instanceof Enemy) && !mob.getTags().contains("7dtc_ecosystem_spawn")) return;
            ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            if (isArtifactsMimic(type)) {
                removeArtifactDrops(event);
                dropMimicCache(event);
                if (mob.getTags().contains("7dtc_horde_boss")) mob.spawnAtLocation(key(4));
                return;
            }
            DivineWeaponDrops.apply(event);
            EquipmentDrops.apply(event);
            GunBlueprintDrops.apply(event);
            DivineBlueprintDrops.apply(event);
            removeArtifactDrops(event);
            if (mob.getTags().contains("7dtc_horde_boss")) {
                mob.spawnAtLocation(key(4));
                return;
            }
            int tier;
            double chance;
            if (type != null && (type.getNamespace().equals("mutantmonsters") || type.getNamespace().equals("mutantsbuff"))) {
                tier = 4; chance = 0.25;
            } else if (ModList.get().isLoaded("infernalmobs") && InfernalMobsCore.getMobModifiers(mob) != null) {
                tier = 3; chance = 0.10;
            } else if (type != null && type.getNamespace().equals("minecraft") && type.getPath().equals("zombie")) {
                tier = 1; chance = 0.03;
            } else if (ThreatSense.threatTier(mob) >= 4) {
                tier = 4; chance = 0.08;
            } else if (ThreatSense.threatTier(mob) >= 3) {
                tier = 3; chance = 0.05;
            } else if (ThreatSense.threatTier(mob) >= 2) {
                tier = 2; chance = 0.03;
            } else if (isStrongCommon(type)) {
                tier = 2; chance = 0.03;
            } else {
                tier = 1; chance = 0.015;
            }
            if (event.getSource().getEntity() instanceof ServerPlayer killer && killer.getTags().contains("7dtc_key_hunter")) {
                chance = switch (tier) {
                    case 1 -> 0.06;
                    case 2 -> 0.08;
                    case 3 -> 0.18;
                    case 4 -> 0.32;
                    default -> chance;
                };
            }
            if (mob.getRandom().nextDouble() < chance) mob.spawnAtLocation(key(tier));
        }

        private static boolean isArtifactsMimic(ResourceLocation id) {
            return id != null && id.getNamespace().equals("artifacts") && id.getPath().equals("mimic");
        }

        private static void removeArtifactDrops(LivingDropsEvent event) {
            event.getDrops().removeIf(drop -> {
                ResourceLocation dropId = BuiltInRegistries.ITEM.getKey(drop.getItem().getItem());
                return dropId != null && isArtifactNamespace(dropId.getNamespace());
            });
        }

        private static void dropMimicCache(LivingDropsEvent event) {
            LivingEntity mimic = event.getEntity();
            // Preserve the previous 70/29/1 tier roll, then place the curated
            // artifact inside a keyed chest when the player opens it.
            double roll = mimic.getRandom().nextDouble();
            int rank = roll < 0.70 ? 2 : roll < 0.99 ? 3 : 4;
            Item cache = switch (rank) {
                case 2 -> MIMIC_CACHE_2.get();
                case 3 -> MIMIC_CACHE_3.get();
                default -> MIMIC_CACHE_4.get();
            };
            event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(
                mimic.level(), mimic.getX(), mimic.getY() + 0.5D, mimic.getZ(), new ItemStack(cache)
            ));
        }

        @SubscribeEvent
        public static void onJoin(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide()) return;
            Entity entity = event.getEntity();
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (entity instanceof Mob mob && id != null && (id.getNamespace().equals("mutantmonsters") || id.getNamespace().equals("mutantsbuff"))) {
                var followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
                if (followRange != null && followRange.getBaseValue() > 18.0) followRange.setBaseValue(18.0);
            }
        }

        @SubscribeEvent
        public static void markDeepCommonSpawn(MobSpawnEvent.FinalizeSpawn event) {
            Mob mob = event.getEntity();
            if (mob.level().isClientSide() || event.getSpawnType() != MobSpawnType.NATURAL || mob.getY() > -33.0D) return;
            if (!mob.level().dimension().equals(ServerLevel.OVERWORLD)) return;
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            if (id == null || !id.getNamespace().equals("minecraft")) return;
            boolean ordinary = switch (id.getPath()) {
                case "zombie", "zombie_villager", "husk", "skeleton", "stray" -> true;
                default -> false;
            };
            if (ordinary) {
                mob.addTag(DEEP_COMMON_CANDIDATE);
            }
        }

        @SubscribeEvent
        public static void removeOrdinaryDeepSpawn(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide() || entity.tickCount < 10 || !entity.getTags().contains(DEEP_COMMON_CANDIDATE)) return;
            entity.removeTag(DEEP_COMMON_CANDIDATE);
            boolean infernal = ModList.get().isLoaded("infernalmobs") && InfernalMobsCore.getMobModifiers(entity) != null;
            if (infernal || !hasDeepReplacementNearby(entity)) return;

            // The upper-deep band keeps part of its familiar population. In the
            // abyssal band the replacement is complete, but only after at least
            // two configured cave threats are actually present near this spawn.
            // This prevents the progression rule from turning a cave empty when
            // a modded encounter fails its placement checks or local cap.
            double removalChance = entity.getY() <= -49.0D ? 1.0D : 0.65D;
            if (entity.getRandom().nextDouble() < removalChance) entity.discard();
        }

        private static boolean hasDeepReplacementNearby(LivingEntity common) {
            if (!(common.level() instanceof ServerLevel level)) return false;
            int nearby = level.getEntitiesOfClass(
                Mob.class,
                common.getBoundingBox().inflate(48.0D, 24.0D, 48.0D),
                Events::isDeepReplacement
            ).size();
            return nearby >= 2;
        }

        private static boolean isDeepReplacement(Mob mob) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            if (id == null || !(mob instanceof Enemy)) return false;
            if (id.getNamespace().equals("realmrpg_demons")) return true;
            if (id.getNamespace().equals("born_in_chaos_v1")) return ThreatSense.threatTier(mob) >= 2;
            if (id.getNamespace().equals("alexscaves")) return id.getPath().equals("watcher");
            if (id.getNamespace().equals("quark")) return id.getPath().equals("forgotten");
            if (!id.getNamespace().equals("alexsmobs")) return false;
            return switch (id.getPath()) {
                case "rocky_roller", "centipede_head", "murmur", "murmur_head" -> true;
                default -> false;
            };
        }

        private static void award(ServerPlayer player, ResourceLocation id) {
            var advancement = player.server.getAdvancements().getAdvancement(id);
            if (advancement != null) {
                var progress = player.getAdvancements().getOrStartProgress(advancement);
                for (String criterion : progress.getRemainingCriteria()) player.getAdvancements().award(advancement, criterion);
            }
        }

        private static RandomizableContainerBlockEntity paired(RandomizableContainerBlockEntity container) {
            if (container.getLevel() == null || !(container.getBlockState().getBlock() instanceof ChestBlock) || !container.getBlockState().hasProperty(ChestBlock.TYPE) || container.getBlockState().getValue(ChestBlock.TYPE) == ChestType.SINGLE) return null;
            BlockEntity other = container.getLevel().getBlockEntity(container.getBlockPos().relative(ChestBlock.getConnectedDirection(container.getBlockState())));
            return other instanceof RandomizableContainerBlockEntity paired ? paired : null;
        }
        private static boolean isLocked(RandomizableContainerBlockEntity container) {
            if (container.getPersistentData().getInt(LOCK_TIER) > 0) return true;
            RandomizableContainerBlockEntity paired = paired(container);
            return paired != null && paired.getPersistentData().getInt(LOCK_TIER) > 0;
        }
        private static void visualizeLootChest(ServerLevel level, RandomizableContainerBlockEntity container) {
            if (!(container.getBlockState().getBlock() instanceof ChestBlock)) return;
            CompoundTag saved = container.saveWithFullMetadata();
            String lootTable = saved.getString("LootTable");
            if (lootTable.isBlank()) return;
            StructureStart settlement = level.structureManager().getStructureWithPieceAt(container.getBlockPos(), SETTLEMENTS);
            if (settlement != null && settlement.isValid()) {
                String settlementKey = settlementKey(level, settlement);
                SettlementChestData data = SettlementChestData.get(level);
                RandomizableContainerBlockEntity pair = paired(container);
                long chestKey = container.getBlockPos().asLong();
                if (pair != null) chestKey = Math.min(chestKey, pair.getBlockPos().asLong());
                int specialTier = settlementSpecialTier(level, settlement);
                if (specialTier > 0 && data.claimSpecial(settlementKey, chestKey)) {
                    convertToTierChest(level, container, saved, lootTable, specialTier);
                    return;
                }
                int freeChestLimit = 1 + (int) (mix(level.getSeed(), new BlockPos(
                    settlement.getChunkPos().getMinBlockX(), 0, settlement.getChunkPos().getMinBlockZ()
                )) & 1L);
                if (data.claim(settlementKey, chestKey, freeChestLimit)) {
                    makeSettlementChestOpen(level, container);
                    return;
                }
            }
            int tier = classify(lootTable);
            convertToTierChest(level, container, saved, lootTable, tier);
        }
        private static int settlementSpecialTier(ServerLevel level, StructureStart settlement) {
            BlockPos origin = new BlockPos(
                settlement.getChunkPos().getMinBlockX(), 0, settlement.getChunkPos().getMinBlockZ()
            );
            long villageRoll = mix(level.getSeed() ^ 0x56494C4C414745L, origin);
            if (Math.floorMod(villageRoll, 100L) >= 20L) return 0;
            long tierRoll = mix(level.getSeed() ^ 0x54494552524F4C4CL, origin);
            return Math.floorMod(tierRoll, 100L) < 80L ? 3 : 4;
        }
        private static void convertToTierChest(ServerLevel level, RandomizableContainerBlockEntity container,
                                               CompoundTag saved, String lootTable, int tier) {
            container.getPersistentData().putString(SOURCE_LOOT_TABLE, lootTable);
            Block target = BuiltInRegistries.BLOCK.get(new ResourceLocation(TIER_CHESTS[tier]));
            if (!(target instanceof ChestBlock) || container.getBlockState().is(target)) {
                markLock(container, tier);
                return;
            }
            var oldState = container.getBlockState();
            var newState = target.defaultBlockState();
            if (oldState.hasProperty(ChestBlock.FACING) && newState.hasProperty(ChestBlock.FACING)) newState = newState.setValue(ChestBlock.FACING, oldState.getValue(ChestBlock.FACING));
            if (oldState.hasProperty(ChestBlock.TYPE) && newState.hasProperty(ChestBlock.TYPE)) newState = newState.setValue(ChestBlock.TYPE, oldState.getValue(ChestBlock.TYPE));
            if (oldState.hasProperty(ChestBlock.WATERLOGGED) && newState.hasProperty(ChestBlock.WATERLOGGED)) newState = newState.setValue(ChestBlock.WATERLOGGED, oldState.getValue(ChestBlock.WATERLOGGED));
            BlockPos pos = container.getBlockPos();
            // Replacing a container block calls ChestBlock#onRemove. If the old
            // block entity is still present, vanilla materializes its loot table
            // and ejects every item into the world before the tier chest is made.
            // Keep the complete NBT snapshot above, then detach an empty, lootless
            // block entity so conversion can never leak unopened loot.
            container.clearContent();
            container.setLootTable(null, 0L);
            container.setChanged();
            level.removeBlockEntity(pos);
            level.setBlock(pos, newState, 2 | 16);
            BlockEntity replacement = level.getBlockEntity(pos);
            if (replacement instanceof RandomizableContainerBlockEntity converted) {
                ResourceLocation typeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(converted.getType());
                if (typeId != null) saved.putString("id", typeId.toString());
                converted.load(saved);
                converted.getPersistentData().putString(SOURCE_LOOT_TABLE, lootTable);
                markLock(converted, tier);
                converted.setChanged();
            }
        }
        private static String settlementKey(ServerLevel level, StructureStart start) {
            ResourceLocation structureId = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(start.getStructure());
            var chunk = start.getChunkPos();
            return level.dimension().location() + "|" + (structureId == null ? "unknown" : structureId) + "|" + chunk.x + "," + chunk.z;
        }
        private static void makeSettlementChestOpen(ServerLevel level, RandomizableContainerBlockEntity container) {
            ArrayList<BlockPos> positions = new ArrayList<>();
            positions.add(container.getBlockPos().immutable());
            RandomizableContainerBlockEntity pair = paired(container);
            if (pair != null && !pair.getBlockPos().equals(container.getBlockPos())) positions.add(pair.getBlockPos().immutable());
            Block ordinary = BuiltInRegistries.BLOCK.get(new ResourceLocation("minecraft", "chest"));
            for (BlockPos pos : positions) {
                BlockEntity current = level.getBlockEntity(pos);
                if (!(current instanceof RandomizableContainerBlockEntity chest)) continue;
                CompoundTag saved = chest.saveWithFullMetadata();
                var oldState = chest.getBlockState();
                var newState = ordinary.defaultBlockState();
                if (oldState.hasProperty(ChestBlock.FACING) && newState.hasProperty(ChestBlock.FACING)) newState = newState.setValue(ChestBlock.FACING, oldState.getValue(ChestBlock.FACING));
                if (oldState.hasProperty(ChestBlock.TYPE) && newState.hasProperty(ChestBlock.TYPE)) newState = newState.setValue(ChestBlock.TYPE, oldState.getValue(ChestBlock.TYPE));
                if (oldState.hasProperty(ChestBlock.WATERLOGGED) && newState.hasProperty(ChestBlock.WATERLOGGED)) newState = newState.setValue(ChestBlock.WATERLOGGED, oldState.getValue(ChestBlock.WATERLOGGED));
                if (!oldState.is(ordinary)) {
                    chest.clearContent();
                    chest.setLootTable(null, 0L);
                    chest.setChanged();
                    level.removeBlockEntity(pos);
                    level.setBlock(pos, newState, 2 | 16);
                    BlockEntity replacement = level.getBlockEntity(pos);
                    if (!(replacement instanceof RandomizableContainerBlockEntity converted)) continue;
                    ResourceLocation typeId = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(converted.getType());
                    if (typeId != null) saved.putString("id", typeId.toString());
                    converted.load(saved);
                    chest = converted;
                }
                chest.getPersistentData().remove(LOCK_TIER);
                chest.getPersistentData().remove(LOCK_ROLLED);
                chest.getPersistentData().putBoolean(SETTLEMENT_FREE, true);
                chest.setChanged();
            }
        }
        private static void markLock(RandomizableContainerBlockEntity container, int tier) {
            CompoundTag persistent = container.getPersistentData();
            if (persistent.getBoolean(LOCK_ROLLED) || wasOpened(container)) return;
            persistent.putBoolean(LOCK_ROLLED, true);
            persistent.putInt(LOCK_TIER, tier);
            container.setChanged();
        }
        private static boolean wasOpened(RandomizableContainerBlockEntity container) {
            CompoundTag persistent = container.getPersistentData();
            return persistent.getBoolean(LOCK_OPENED) || persistent.getBoolean("7dtc_rank_loot_added");
        }
        private static void unlock(RandomizableContainerBlockEntity container) {
            CompoundTag persistent = container.getPersistentData();
            persistent.putBoolean(LOCK_ROLLED, true);
            persistent.putBoolean(LOCK_OPENED, true);
            persistent.putInt(LOCK_TIER, 0);
            container.setChanged();
        }
        private static void sanitize(RandomizableContainerBlockEntity container) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (!stack.isEmpty() && id != null && isArtifactNamespace(id.getNamespace())) container.setItem(slot, ItemStack.EMPTY);
            }
        }
        private static void addRankReward(RandomizableContainerBlockEntity container, RandomizableContainerBlockEntity paired, int tier, String sourceLootTable) {
            if (container.getPersistentData().getBoolean("7dtc_rank_loot_added") || (paired != null && paired.getPersistentData().getBoolean("7dtc_rank_loot_added"))) return;
            container.getPersistentData().putBoolean("7dtc_rank_loot_added", true);
            if (paired != null) paired.getPersistentData().putBoolean("7dtc_rank_loot_added", true);
            if (container.getPersistentData().getBoolean("7dtc_onboarding_cache")
                || (paired != null && paired.getPersistentData().getBoolean("7dtc_onboarding_cache"))) {
                place(container, paired, stack(ONBOARDING_AMULETS[container.getLevel().random.nextInt(ONBOARDING_AMULETS.length)], 1));
                return;
            }
            if (container.getPersistentData().getBoolean("7dtc_mimic_cache")
                || (paired != null && paired.getPersistentData().getBoolean("7dtc_mimic_cache"))) {
                String[] choices = RANK_ITEMS[tier];
                place(container, paired, stack(choices[container.getLevel().random.nextInt(choices.length)], 1));
                return;
            }
            int rewardRank = tier;
            double chance;
            if (sourceLootTable.startsWith("wrd:")) {
                if (sourceLootTable.endsWith("/final_chest")) { rewardRank = 4; chance = 1.0; }
                else if (sourceLootTable.endsWith("/bone_chest_loot")) { rewardRank = 3; chance = 1.0; }
                else if (sourceLootTable.endsWith("/treasure_chest_loot")) { rewardRank = 2; chance = 1.0; }
                else return;
            } else {
                chance = switch (tier) { case 1 -> 0.15; case 2 -> 0.25; case 3 -> 0.40; case 4 -> 1.0; default -> 0.0; };
            }
            if (container.getLevel() == null || container.getLevel().random.nextDouble() >= chance) return;
            String[] choices = RANK_ITEMS[rewardRank];
            if (choices.length == 0) return;
            ItemStack reward = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(choices[container.getLevel().random.nextInt(choices.length)])));
            for (int slot = 0; slot < container.getContainerSize(); slot++) if (container.getItem(slot).isEmpty()) { container.setItem(slot, reward); return; }
            if (paired != null) for (int slot = 0; slot < paired.getContainerSize(); slot++) if (paired.getItem(slot).isEmpty()) { paired.setItem(slot, reward); return; }
            BlockPos pos = container.getBlockPos();
            container.getLevel().addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(container.getLevel(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, reward));
        }
        private static void addTierSupplies(RandomizableContainerBlockEntity container, RandomizableContainerBlockEntity paired, int tier) {
            if (container.getPersistentData().getBoolean("7dtc_supplies_added")) return;
            container.getPersistentData().putBoolean("7dtc_supplies_added", true);
            if (paired != null) paired.getPersistentData().putBoolean("7dtc_supplies_added", true);
            var random = container.getLevel().random;
            String[][] resources = {
                {},
                {"minecraft:bread", "minecraft:baked_potato", "minecraft:cooked_beef", "minecraft:coal", "minecraft:coal", "minecraft:raw_iron", "minecraft:raw_copper", "minecraft:gunpowder"},
                {"minecraft:bread", "minecraft:cooked_beef", "minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:copper_ingot", "minecraft:gold_ingot", "minecraft:redstone", "minecraft:lapis_lazuli", "minecraft:leather", "minecraft:slime_ball", "minecraft:quartz"},
                {"minecraft:cooked_beef", "minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:gold_ingot", "minecraft:redstone", "minecraft:lapis_lazuli", "minecraft:emerald", "minecraft:diamond", "minecraft:blaze_powder", "minecraft:ender_pearl"},
                {"minecraft:gold_ingot", "minecraft:emerald", "minecraft:emerald", "minecraft:diamond", "minecraft:diamond", "minecraft:blaze_powder", "minecraft:ender_pearl", "minecraft:echo_shard"}
            };
            int rolls = switch (tier) { case 1 -> 2 + random.nextInt(3); case 2 -> 3 + random.nextInt(3); case 3 -> 4 + random.nextInt(3); default -> 5 + random.nextInt(3); };
            for (int i = 0; i < rolls; i++) {
                String id = resources[tier][random.nextInt(resources[tier].length)];
                int count = switch (tier) { case 1 -> 2 + random.nextInt(5); case 2 -> 3 + random.nextInt(6); case 3 -> 4 + random.nextInt(7); default -> 4 + random.nextInt(8); };
                place(container, paired, stack(id, count));
            }
            if (tier == 1 && random.nextDouble() < 0.30) place(container, paired, stack("cgm:basic_bullet", 2 + random.nextInt(5)));
            if (tier == 2) place(container, paired, stack(random.nextBoolean() ? "cgm:basic_bullet" : "cgm:shell", random.nextBoolean() ? 8 + random.nextInt(9) : 3 + random.nextInt(6)));
            if (tier == 3) {
                place(container, paired, stack("cgm:basic_bullet", 16 + random.nextInt(13)));
                place(container, paired, stack(random.nextBoolean() ? "cgm:advanced_bullet" : "cgm:shell", 5 + random.nextInt(8)));
            }
            if (tier == 4) {
                place(container, paired, stack("cgm:advanced_bullet", 10 + random.nextInt(11)));
                place(container, paired, stack("cgm:shell", 8 + random.nextInt(11)));
                if (random.nextDouble() < 0.10) place(container, paired, stack("cgm:grenade", 1));
            }
            addMagicBookRoll(container, paired, tier);
            addEquipmentRoll(container, paired, tier);
        }

        private static void addMagicBookRoll(RandomizableContainerBlockEntity container, RandomizableContainerBlockEntity paired, int tier) {
            var random = container.getLevel().random;
            double chance = switch (tier) { case 1 -> 0.05; case 2 -> 0.12; case 3 -> 0.22; default -> 0.35; };
            if (random.nextDouble() >= chance) return;
            int power = switch (tier) { case 1 -> 5; case 2 -> 15; case 3 -> 25; default -> 30; };
            boolean treasure = tier >= 3;
            ItemStack book = EnchantmentHelper.enchantItem(random, new ItemStack(Items.BOOK), power, treasure);
            place(container, paired, book);
        }
        private static void addEquipmentRoll(RandomizableContainerBlockEntity container, RandomizableContainerBlockEntity paired, int tier) {
            var random = container.getLevel().random;
            double chance = switch (tier) { case 1 -> 0.18; case 2 -> 0.25; case 3 -> 0.30; default -> 0.38; };
            if (random.nextDouble() >= chance) return;
            String[][] equipment = {
                {},
                {"minecraft:iron_pickaxe", "minecraft:iron_axe", "minecraft:iron_shovel"},
                {"minecraft:iron_sword", "minecraft:iron_pickaxe", "divine_weaponry:iron_dagger", "divine_weaponry:iron_sickle", "divine_weaponry:iron_spear", "divine_weaponry:iron_cutlass", "divine_weaponry:iron_rapier"},
                {"minecraft:diamond_pickaxe", "minecraft:diamond_sword", "divine_weaponry:diamond_dagger", "divine_weaponry:diamond_sickle", "divine_weaponry:diamond_spear", "divine_weaponry:diamond_katana", "divine_weaponry:diamond_battleaxe"},
                {"cgm:pistol", "cgm:rifle", "cgm:shotgun", "cgm:machine_pistol", "cgm:heavy_rifle", "divine_weaponry:apophis_katana", "divine_weaponry:erebus_scythe", "divine_weaponry:hyperion_longsword", "divine_weaponry:mjollnir_hammer", "divine_weaponry:om_spear", "divine_weaponry:zeus_cutlass"}
            };
            place(container, paired, stack(equipment[tier][random.nextInt(equipment[tier].length)], 1));
        }

        private static ItemStack stack(String id, int count) {
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(id)));
            stack.setCount(Math.min(count, stack.getMaxStackSize()));
            return stack;
        }

        private static void place(RandomizableContainerBlockEntity container, RandomizableContainerBlockEntity paired, ItemStack stack) {
            if (stack.isEmpty()) return;
            for (int slot = 0; slot < container.getContainerSize(); slot++) if (container.getItem(slot).isEmpty()) { container.setItem(slot, stack); return; }
            if (paired != null) for (int slot = 0; slot < paired.getContainerSize(); slot++) if (paired.getItem(slot).isEmpty()) { paired.setItem(slot, stack); return; }
        }
        private static boolean isArtifactNamespace(String namespace) { return namespace.equals("artifacts") || namespace.equals("nameless_trinkets") || namespace.equals("celestial_artifacts"); }
        private static boolean isCurated(String id) { for (int tier = 1; tier < RANK_ITEMS.length; tier++) for (String item : RANK_ITEMS[tier]) if (item.equals(id)) return true; return false; }
        private static InteractionHand findKey(ServerPlayer player, Item required) {
            if (player.getMainHandItem().is(required)) return InteractionHand.MAIN_HAND;
            if (player.getOffhandItem().is(required)) return InteractionHand.OFF_HAND;
            return null;
        }
        private static ItemStack key(int tier) { return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(KEYS[tier]))); }
        private static boolean isStrongCommon(ResourceLocation id) {
            if (id == null) return false;
            String s = id.toString();
            return s.contains("husk") || s.contains("drowned") || s.contains("witch") || s.contains("pillager") || s.contains("vindicator") || s.contains("evoker") || s.contains("wither_skeleton") || s.contains("ravager") || s.contains("brute");
        }
        private static int classify(String raw) {
            String id = raw.toLowerCase(Locale.ROOT);
            // Born in Chaos declares an explicit reward ladder. Preserve it so
            // every structure chest uses the matching 7DTC key and visual tier.
            if (id.equals("minecraft:chests/chest_level_3")) return 3;
            if (id.equals("minecraft:chests/chest_level_2") || id.equals("minecraft:chests/firewell_d") || id.equals("minecraft:chests/shater")) return 2;
            if (id.equals("minecraft:chests/chest_level_1") || id.equals("minecraft:chests/basic_chest") || id.equals("minecraft:chests/farm_drop")) return 1;
            if (id.startsWith("wrd:")) {
                if (containsAny(id, "final_chest")) return 4;
                if (containsAny(id, "bone_chest_loot", "treasure_chest_loot", "stronghold_loot_diamond")) return 3;
                if (containsAny(id, "stronghold_loot_gold", "stronghold_loot_redstone", "purpur_chest", "endstone_chest")) return 2;
                return 1;
            }
            if (containsAny(id, "ancient_city", "bastion_treasure", "end_city", "boss", "treasure_room")) return 4;
            if (containsAny(id, "dungeon", "stronghold", "woodland_mansion", "trial_chambers", "buried_treasure", "mineshaft")) return 3;
            if (containsAny(id, "shipwreck", "ruined_portal", "pillager_outpost", "desert_pyramid", "jungle_temple", "igloo")) return 2;
            return 1;
        }
        private static boolean isWrdCompletionLoot(String id) {
            return containsAny(id, "/treasure_chest_loot", "/bone_chest_loot", "/final_chest");
        }
        private static boolean containsAny(String value, String... needles) { for (String needle : needles) if (value.contains(needle)) return true; return false; }
        private static int tierFromBlock(RandomizableContainerBlockEntity container) { return tierFromBlock(container.getBlockState()); }
        private static int tierFromBlock(net.minecraft.world.level.block.state.BlockState state) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (id == null) return 0;
            for (int tier = 1; tier < TIER_CHESTS.length; tier++) if (id.toString().equals(TIER_CHESTS[tier])) return tier;
            return 0;
        }
        private static long mix(long seed, BlockPos pos) {
            long x = seed ^ pos.asLong() * 0x9E3779B97F4A7C15L;
            x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L;
            x ^= x >>> 27; x *= 0x94D049BB133111EBL;
            return x ^ (x >>> 31);
        }
    }
}
