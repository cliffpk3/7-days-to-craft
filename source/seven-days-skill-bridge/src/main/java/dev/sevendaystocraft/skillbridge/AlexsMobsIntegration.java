package dev.sevendaystocraft.skillbridge;

import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod.EventBusSubscriber(modid = SkillBridge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AlexsMobsIntegration {
    private record Gate(String tag, String milestone) {}
    private record Discovery(String key, long questId) {}

    private static final Map<String, Gate> GATES = Map.ofEntries(
        Map.entry("alexsmobs:blood_sprayer", new Gate("7dtc_gunsmithing", "Gunsmithing")),
        Map.entry("alexsmobs:hemolymph_blaster", new Gate("7dtc_gunsmithing", "Gunsmithing")),
        Map.entry("alexsmobs:stink_ray", new Gate("7dtc_gunsmithing", "Gunsmithing")),
        Map.entry("alexsmobs:centipede_leggings", new Gate("7dtc_parcool_wall_grip", "Wall Grip")),
        Map.entry("alexsmobs:rocky_chestplate", new Gate("7dtc_parcool_wall_grip", "Wall Grip")),
        Map.entry("alexsmobs:squid_grapple", new Gate("7dtc_grappler", "Grappler License")),
        Map.entry("alexsmobs:vine_lasso", new Gate("7dtc_grappler", "Grappler License")),
        Map.entry("alexsmobs:flying_fish_boots", new Gate("7dtc_ender_traversal", "Ender Traversal")),
        Map.entry("alexsmobs:tarantula_hawk_elytra", new Gate("7dtc_ender_traversal", "Ender Traversal")),
        Map.entry("alexsmobs:straddleboard", new Gate("7dtc_ender_traversal", "Ender Traversal")),
        Map.entry("alexsmobs:dimensional_carver", new Gate("7dtc_ender_traversal", "Ender Traversal")),
        Map.entry("alexsmobs:echolocator", new Gate("7dtc_cartography", "Cartography")),
        Map.entry("alexsmobs:endolocator", new Gate("7dtc_cartography", "Cartography")),
        Map.entry("alexsmobs:pupfish_locator", new Gate("7dtc_cartography", "Cartography")),
        Map.entry("alexsmobs:transmutation_table", new Gate("7dtc_master_salvager", "Master Salvager"))
    );
    private static final Set<String> DISABLED_ITEMS = Set.of("alexsmobs:unsettling_kimono", "alexsmobs:sculk_boomer");
    private static final Map<String, Discovery> DISCOVERIES = Map.ofEntries(
        Map.entry("field_guide", new Discovery("field_guide", 0x7D7CA10000000001L)),
        Map.entry("hummingbirds", new Discovery("hummingbirds", 0x7D7CA10000000002L)),
        Map.entry("leafcutters", new Discovery("leafcutters", 0x7D7CA10000000003L)),
        Map.entry("blood_technology", new Discovery("blood_technology", 0x7D7CA10000000004L)),
        Map.entry("climbing_gear", new Discovery("climbing_gear", 0x7D7CA10000000005L)),
        Map.entry("living_grapple", new Discovery("living_grapple", 0x7D7CA10000000006L)),
        Map.entry("cave_locator", new Discovery("cave_locator", 0x7D7CA10000000007L)),
        Map.entry("end_locator", new Discovery("end_locator", 0x7D7CA10000000008L)),
        Map.entry("falconry", new Discovery("falconry", 0x7D7CA10000000009L)),
        Map.entry("transmutation", new Discovery("transmutation", 0x7D7CA1000000000AL)),
        Map.entry("void_worm", new Discovery("void_worm", 0x7D7CA1000000000BL)),
        Map.entry("hazards", new Discovery("hazards", 0x7D7CA1000000000CL)),
        Map.entry("elephants", new Discovery("elephants", 0x7D7CA1000000000DL)),
        Map.entry("tigers", new Discovery("tigers", 0x7D7CA1000000000EL))
    );

    private AlexsMobsIntegration() {}

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 20 != 0) return;
        trackInventory(player);
        trackNearbyCreatures(player);
        trackEffects(player);
        enforceEquipment(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String id = itemId(event.getItemStack());
        if (!allow(player, id, event)) {
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        recordItemAction(player, id);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String held = itemId(event.getItemStack());
        if (!allow(player, held, event)) {
            event.setUseItem(Event.Result.DENY);
            event.setUseBlock(Event.Result.DENY);
            return;
        }
        String block = BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(event.getPos()).getBlock()).toString();
        if (block.equals("alexsmobs:transmutation_table")) {
            discover(player, "transmutation");
            if (!player.getTags().contains("7dtc_master_salvager")) {
                event.setCanceled(true);
                event.setUseBlock(Event.Result.DENY);
                deny(player, "Master Salvager");
                return;
            }
            award(player, "alexs_mobs/actions/transmutation_opened");
        }
        if (block.equals("alexsmobs:hummingbird_feeder")) {
            discover(player, "hummingbirds");
            award(player, "alexs_mobs/actions/hummingbird_feeder_placed");
        }
        if (block.equals("alexsmobs:leafcutter_anthill") || block.equals("alexsmobs:leafcutter_ant_chamber")) discover(player, "leafcutters");
        recordItemAction(player, held);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String id = itemId(player.getMainHandItem());
        if (!allow(player, id, event)) return;
        recordItemAction(player, id);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String held = itemId(event.getItemStack());
        if (!allow(player, held, event)) return;
        String target = BuiltInRegistries.ENTITY_TYPE.getKey(event.getTarget().getType()).toString();
        discoverEntity(player, target);
        if (target.equals("alexsmobs:elephant") && held.equals("alexsmobs:acacia_blossom")) award(player, "alexs_mobs/actions/elephant_fed");
        if (target.equals("alexsmobs:tiger") && isTigerFood(event.getItemStack())) award(player, "alexs_mobs/actions/tiger_fed");
        if (target.equals("alexsmobs:bald_eagle") && (held.equals("alexsmobs:falconry_glove") || held.equals("alexsmobs:falconry_hood"))) award(player, "alexs_mobs/actions/falconry_interaction");
        recordItemAction(player, held);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String block = BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock()).toString();
        if (block.equals("alexsmobs:sculk_boomer")) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal("The Sculk Boomer is disabled in 7 Days to Craft."), true);
            return;
        }
        if (block.equals("alexsmobs:hummingbird_feeder")) {
            discover(player, "hummingbirds");
            award(player, "alexs_mobs/actions/hummingbird_feeder_placed");
        }
    }

    private static boolean allow(ServerPlayer player, String id, Event event) {
        if (DISABLED_ITEMS.contains(id)) {
            event.setCanceled(true);
            player.displayClientMessage(Component.literal("This item is disabled in 7 Days to Craft."), true);
            return false;
        }
        Gate gate = GATES.get(id);
        if (gate == null || player.getTags().contains(gate.tag())) return true;
        event.setCanceled(true);
        deny(player, gate.milestone());
        return false;
    }

    private static void deny(ServerPlayer player, String milestone) {
        player.displayClientMessage(Component.literal(milestone + " is required to use this item."), true);
    }

    private static void trackInventory(ServerPlayer player) {
        if (hasAny(player, "alexsmobs:animal_dictionary")) discover(player, "field_guide");
        if (hasAny(player, "alexsmobs:hummingbird_feeder")) {
            discover(player, "hummingbirds");
            award(player, "alexs_mobs/actions/hummingbird_feeder_placed");
        }
        if (hasAny(player, "alexsmobs:leafcutter_ant_pupa")) {
            discover(player, "leafcutters");
            award(player, "alexs_mobs/actions/leafcutter_pupa_obtained");
        }
        if (hasAny(player, "alexsmobs:blood_sac", "alexsmobs:blood_sprayer", "alexsmobs:hemolymph_sac", "alexsmobs:hemolymph_blaster", "alexsmobs:stink_ray")) discover(player, "blood_technology");
        if (hasAny(player, "alexsmobs:centipede_leg", "alexsmobs:centipede_leggings", "alexsmobs:rocky_chestplate")) discover(player, "climbing_gear");
        if (hasAny(player, "alexsmobs:lost_tentacle", "alexsmobs:squid_grapple", "alexsmobs:vine_lasso")) discover(player, "living_grapple");
        if (hasAny(player, "alexsmobs:ambergris", "alexsmobs:cachalot_whale_tooth", "alexsmobs:echolocator")) discover(player, "cave_locator");
        if (hasAny(player, "alexsmobs:endolocator")) discover(player, "end_locator");
        if (hasAny(player, "alexsmobs:falconry_glove", "alexsmobs:falconry_hood")) discover(player, "falconry");
        if (hasAny(player, "alexsmobs:farseer_arm", "alexsmobs:transmutation_table")) discover(player, "transmutation");
        if (hasAny(player, "alexsmobs:mysterious_worm", "alexsmobs:void_worm_mandible", "alexsmobs:void_worm_eye")) discover(player, "void_worm");
    }

    private static void trackNearbyCreatures(ServerPlayer player) {
        for (Entity entity : player.serverLevel().getEntities(player, player.getBoundingBox().inflate(8.0D), e -> e.isAlive())) {
            discoverEntity(player, BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
        }
    }

    private static void discoverEntity(ServerPlayer player, String id) {
        switch (id) {
            case "alexsmobs:hummingbird" -> discover(player, "hummingbirds");
            case "alexsmobs:leafcutter_ant" -> discover(player, "leafcutters");
            case "alexsmobs:cachalot_whale" -> discover(player, "cave_locator");
            case "alexsmobs:bald_eagle" -> discover(player, "falconry");
            case "alexsmobs:void_worm" -> {
                discover(player, "void_worm");
                award(player, "alexs_mobs/actions/void_worm_encountered");
            }
            case "alexsmobs:elephant" -> discover(player, "elephants");
            case "alexsmobs:tiger" -> discover(player, "tigers");
        }
    }

    private static void trackEffects(ServerPlayer player) {
        for (MobEffectInstance effect : player.getActiveEffects()) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect());
            if (id != null && id.getNamespace().equals("alexsmobs")) {
                discover(player, "hazards");
                award(player, "alexs_mobs/actions/unfamiliar_condition");
                return;
            }
        }
    }

    private static void enforceEquipment(ServerPlayer player) {
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            String id = itemId(stack);
            Gate gate = GATES.get(id);
            String reason = null;
            if (DISABLED_ITEMS.contains(id)) reason = "This equipment is disabled in 7 Days to Craft.";
            else if (gate != null && !player.getTags().contains(gate.tag())) reason = gate.milestone() + " is required to equip this item.";
            if (reason != null) {
                ItemStack removed = stack.copy();
                player.setItemSlot(slot, ItemStack.EMPTY);
                if (!player.getInventory().add(removed)) player.drop(removed, false);
                player.displayClientMessage(Component.literal(reason), true);
                continue;
            }
            if (id.equals("alexsmobs:centipede_leggings") || id.equals("alexsmobs:rocky_chestplate")) award(player, "alexs_mobs/actions/climbing_gear_equipped");
        }
    }

    private static void recordItemAction(ServerPlayer player, String id) {
        switch (id) {
            case "alexsmobs:animal_dictionary" -> award(player, "alexs_mobs/actions/field_guide_opened");
            case "alexsmobs:blood_sprayer", "alexsmobs:hemolymph_blaster", "alexsmobs:stink_ray" -> award(player, "alexs_mobs/actions/blood_weapon_used");
            case "alexsmobs:squid_grapple", "alexsmobs:vine_lasso" -> award(player, "alexs_mobs/actions/living_grapple_used");
            case "alexsmobs:echolocator", "alexsmobs:pupfish_locator" -> award(player, "alexs_mobs/actions/cave_locator_used");
            case "alexsmobs:endolocator" -> award(player, "alexs_mobs/actions/end_locator_used");
        }
    }

    private static boolean isTigerFood(ItemStack stack) {
        return stack.is(Items.PORKCHOP) || stack.is(Items.COOKED_PORKCHOP) || stack.is(Items.CHICKEN) || stack.is(Items.COOKED_CHICKEN);
    }

    private static boolean hasAny(ServerPlayer player, String... ids) {
        Set<String> wanted = Set.of(ids);
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && wanted.contains(itemId(stack))) return true;
        }
        return false;
    }

    private static String itemId(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    private static void discover(ServerPlayer player, String key) {
        Discovery discovery = DISCOVERIES.get(key);
        if (discovery == null) return;
        String marker = "7dtc_alexsmobs_discovered_" + discovery.key();
        if (player.getPersistentData().getBoolean(marker)) return;
        player.getPersistentData().putBoolean(marker, true);
        award(player, "alexs_mobs/discovery/" + discovery.key());
        if (ServerQuestFile.INSTANCE != null) {
            Quest quest = ServerQuestFile.INSTANCE.getQuest(discovery.questId());
            if (quest != null) TeamData.get(player).setQuestPinned(player, discovery.questId(), true);
        }
    }

    private static void award(ServerPlayer player, String path) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(new ResourceLocation("seven_days_to_craft", path));
        if (advancement == null) return;
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) player.getAdvancements().award(advancement, criterion);
    }
}
