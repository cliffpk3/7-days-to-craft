package dev.sevendaystocraft.locks;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import insane96mcp.enhancedai.modules.mobs.fisher.FisherMobs;
import insane96mcp.enhancedai.modules.mobs.fisher.FishingTargetGoal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.smileycorp.hordes.common.event.HordeEndEvent;
import net.smileycorp.hordes.common.event.HordeSpawnEntityEvent;
import net.smileycorp.hordes.common.event.HordeStartEvent;
import net.smileycorp.hordes.common.event.HordeStartWaveEvent;
import net.smileycorp.hordes.hordeevent.capability.HordeEvent;
import net.smileycorp.hordes.hordeevent.capability.HordeSavedData;

import java.util.List;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class HordeRoles {
    private static final String ROLE_PREFIX = "7dtc_role_";
    private static final String SUMMONED = "7dtc_summoned";
    private static final String HORDE_BOSS = "7dtc_horde_boss";
    private static final String HORDE_SPAWN = "7dtc_horde_spawn";
    private static final String THREAT = "7dtc_horde_threat";
    private static final String DEATHS = "7dtc_horde_deaths";
    private static final String BOSS_SPAWNED = "7dtc_horde_boss_spawned";
    private static final String ONLINE_PLAYERS = "7dtc_horde_online_players";
    private static final String REWARD_DAY = "7dtc_horde_reward_day";
    private static final Set<Mob> FISHER_GOALS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final String[] MUTANT_BOSSES = {
        "mutantmonsters:mutant_zombie", "mutantmonsters:mutant_skeleton",
        "mutantmonsters:mutant_creeper", "mutantmonsters:mutant_enderman"
    };
    private static final String[][] TANK_ARMOR = {
        {"fracturepoint:insurgency_commander_boots", "fracturepoint:insurgency_commander_leggings", "fracturepoint:insurgency_commander_chestplate", "fracturepoint:insurgency_commander_helmet"},
        {"fracturepoint:fsb_boots", "fracturepoint:fsb_leggings", "fracturepoint:fsb_squad_leader_chestplate", "fracturepoint:fsb_squad_leader_helmet"},
        {"fracturepoint:beta7_boots", "fracturepoint:beta7_leggings", "fracturepoint:beta7_chestplate", "fracturepoint:beta7_nvg_helmet"}
    };

    private HordeRoles() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void removeCommonArmor(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) return;
        if (mob.getTags().stream().anyMatch(tag -> tag.startsWith(ROLE_PREFIX))
            || mob.getTags().contains(HORDE_BOSS)
            || mob.getTags().contains(SUMMONED)) return;

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (id == null || !id.getNamespace().equals("minecraft")) return;
        String path = id.getPath();
        if (!(path.equals("zombie") || path.equals("zombie_villager") || path.equals("husk")
            || path.equals("drowned") || path.equals("skeleton") || path.equals("stray"))) return;

        // Keep signature weapons (bows, tridents, shovels), which are visible
        // parts of the vanilla variant, but prevent silent armor upgrades.
        mob.setCanPickUpLoot(false);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            mob.setItemSlot(slot, ItemStack.EMPTY);
            mob.setDropChance(slot, 0.0F);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void restoreFisherRod(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) return;
        if (mob.getTags().contains("7dtc_horde_bruiser")) {
            var armor = mob.getAttribute(Attributes.ARMOR);
            if (armor != null) armor.setBaseValue(Math.max(8.0D, armor.getBaseValue()));
        }
        if (mob.getTags().stream().anyMatch(tag -> tag.startsWith(ROLE_PREFIX))) {
            mob.setCustomNameVisible(true);
            if (hasRole(mob, "breaker") && mob.getCustomName() != null
                && "Breaker".equals(mob.getCustomName().getString()))
                mob.setCustomName(Component.literal("Miner").withStyle(ChatFormatting.GOLD));
            if (hasRole(mob, "tank") && mob.getCustomName() != null
                && "Tank".equals(mob.getCustomName().getString()))
                mob.setCustomName(Component.literal("Tanker").withStyle(ChatFormatting.DARK_RED));
        }
        if (!hasRole(mob, "fisher")) return;
        // The Hordes fires its role event before finalizeSpawn, which can replace equipment.
        mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.FISHING_ROD));
        mob.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        FisherMobs.hookInventoryChance = 0.0D;
        if (FISHER_GOALS.add(mob)) mob.goalSelector.addGoal(2, new FishingTargetGoal(mob));
    }

    @SubscribeEvent
    public static void onHordeMob(HordeSpawnEntityEvent event) {
        Mob mob = event.getEntity();
        mob.addTag(HORDE_SPAWN);
        if (!(mob instanceof Zombie) || mob.getTags().contains(SUMMONED)) return;
        int horde = hordeNumber(event.getDay());
        int roll = event.getRandom().nextInt(100);
        int fisherStart = horde == 1 ? 10 : 38;
        int fisherSlots = Mth.clamp((int) Math.round(HordeRoleConfig.fisherChance() * 100.0), 0, 30);
        if (horde >= 4 && roll < 4) {
            Mob witch = replacement(event, "minecraft:witch");
            configureSummoner(witch == null ? mob : witch, horde);
        }
        else if (horde >= 3 && roll < 14) configureDisruptor(mob, horde);
        else if (horde >= 2 && roll < 24) {
            Mob bruiser = replacement(event, "born_in_chaos_v1:zombie_bruiser");
            if (bruiser == null) configureTank(mob, horde, event.getRandom().nextInt(TANK_ARMOR.length));
            else bruiser.addTag("7dtc_horde_bruiser");
        }
        else if (roll < (horde == 1 ? 10 : 38)) configureBreaker(mob, horde);
        else if (roll < fisherStart + fisherSlots) configureFisher(mob, horde);
        else configureAssault(mob, horde);
    }

    private static Mob replacement(HordeSpawnEntityEvent event, String id) {
        ResourceLocation key = new ResourceLocation(id);
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(key)) return null;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(key);
        if (!(type.create(event.getPlayer().serverLevel()) instanceof Mob replacement)) return null;
        replacement.addTag(HORDE_SPAWN);
        event.setEntity(replacement);
        return replacement;
    }

    @SubscribeEvent
    public static void onHordeStart(HordeStartEvent event) {
        ServerPlayer player = event.getPlayer();
        player.getPersistentData().putInt(DEATHS, 0);
        player.getPersistentData().putBoolean(BOSS_SPAWNED, false);
        int onlinePlayers = (int) player.server.getPlayerList().getPlayers().stream()
            .filter(online -> !online.isSpectator())
            .count();
        onlinePlayers = Math.max(1, onlinePlayers);
        player.getPersistentData().putInt(ONLINE_PLAYERS, onlinePlayers);
        int horde = hordeNumber(event.getDay());
        int bosses = 0;
        if (horde == 4 && event.getRandom().nextDouble() < 0.20) bosses = 1;
        else if (horde == 5 && event.getRandom().nextDouble() < 0.50) bosses = 1;
        else if (horde >= 6) bosses = 1;
        if (horde >= 10 && event.getRandom().nextDouble() < 0.20) bosses++;
        for (int i = 0; i < bosses; i++) spawnBoss(event, i);
        if (bosses > 0) player.getPersistentData().putBoolean(BOSS_SPAWNED, true);
    }

    @SubscribeEvent
    public static void onWave(HordeStartWaveEvent event) {
        int threat = Mth.clamp(event.getPlayer().getPersistentData().getInt(THREAT), 0, 5);
        int onlinePlayers = Math.max(1, event.getPlayer().getPersistentData().getInt(ONLINE_PLAYERS));
        float multiplier = cooperativeMultiplier(onlinePlayers) * (1.0F + threat * 0.10F);
        event.setCount(Math.max(1, Math.round(event.getCount() * multiplier)));
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        HordeEvent horde = HordeSavedData.getData(level).getEvent(player);
        if (horde != null && horde.isActive()) player.getPersistentData().putInt(DEATHS, player.getPersistentData().getInt(DEATHS) + 1);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath() || !(event.getEntity() instanceof ServerPlayer player)) return;
        var previous = event.getOriginal().getPersistentData();
        var current = player.getPersistentData();
        if (previous.contains(DEATHS)) current.putInt(DEATHS, previous.getInt(DEATHS));
        if (previous.contains(REWARD_DAY)) current.putInt(REWARD_DAY, previous.getInt(REWARD_DAY));
    }

    @SubscribeEvent
    public static void onHordeEnd(HordeEndEvent event) {
        ServerPlayer player = event.getPlayer();
        int threat = Mth.clamp(player.getPersistentData().getInt(THREAT), 0, 5);
        int deaths = player.getPersistentData().getInt(DEATHS);
        if (deaths == 0) threat = Math.min(5, threat + 1);
        else if (deaths >= 2) threat = Math.max(0, threat - 1);
        player.getPersistentData().putInt(THREAT, threat);
        player.displayClientMessage(Component.literal("Horde threat level: " + threat + "/5").withStyle(ChatFormatting.GOLD), false);
        if (event.wasCommand() || deaths != 0) return;
        int day = event.getDay();
        if (player.getPersistentData().contains(REWARD_DAY)
            && player.getPersistentData().getInt(REWARD_DAY) == day) return;
        player.getPersistentData().putInt(REWARD_DAY, day);
        giveHordeReward(player, new ItemStack(BuiltInRegistries.ITEM.get(
            new ResourceLocation("wrd", "keystrongholdchest_gold"))));
        giveHordeReward(player, new ItemStack(SevenDaysLocks.MIMIC_CACHE_2.get()));
        player.displayClientMessage(Component.literal("Horde survived: Level 2 key and cache awarded.")
            .withStyle(ChatFormatting.GOLD), false);
    }

    private static void giveHordeReward(ServerPlayer player, ItemStack reward) {
        if (!player.getInventory().add(reward)) player.drop(reward, false);
        player.containerMenu.broadcastChanges();
    }

    @SubscribeEvent
    public static void onRoleTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide() || mob.tickCount % 20 != 0) return;
        if (hasRole(mob, "summoner")) tickSummoner(mob);
        if (!(mob instanceof Zombie zombie)) return;
        if (hasRole(zombie, "breaker") && zombie.tickCount % 100 == 0) breakObstacle(zombie);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void protectSummonerTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Witch witch && hasRole(witch, "summoner")
            && event.getEntity() instanceof Enemy) event.setNewTarget(null);
        if (event.getEntity() instanceof Witch witch && hasRole(witch, "summoner")
            && event.getNewTarget() instanceof Enemy) event.setNewTarget(null);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void protectSummonerFromMonsters(LivingAttackEvent event) {
        if (event.getEntity() instanceof Witch witch && hasRole(witch, "summoner")
            && event.getSource().getEntity() instanceof Enemy) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void protectHordeMobsFromSun(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide() || mob.tickCount % 10 != 0) return;
        if (!mob.getTags().contains(HORDE_SPAWN) && !mob.getTags().contains(HORDE_BOSS)) return;
        if (!(mob.level() instanceof ServerLevel level) || !hasActiveHorde(level)) return;

        // The normal sunlight rules remain enabled in the Hordes config. Only
        // creatures that actually belong to a currently active horde receive
        // this short, continuously refreshed protection.
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 30, 0, true, false, false));
    }

    @SubscribeEvent
    public static void onRoleAttack(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (hasRole(attacker, "disruptor")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 6, 1));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 6, 0));
        }
    }

    @SubscribeEvent
    public static void suppressSummonedLoot(LivingDropsEvent event) {
        if (event.getEntity().getTags().contains(SUMMONED)) event.getDrops().clear();
    }

    @SubscribeEvent
    public static void suppressSummonedXp(LivingExperienceDropEvent event) {
        if (event.getEntity().getTags().contains(SUMMONED)) event.setDroppedExperience(0);
    }

    private static void configureAssault(Mob mob, int horde) {
        mob.addTag(ROLE_PREFIX + "assault");
        scaleBase(mob, horde, 1.0, 1.0);
    }

    private static void configureFisher(Mob mob, int horde) {
        mob.addTag(ROLE_PREFIX + "fisher");
        mob.setCustomName(Component.literal("Grappler").withStyle(ChatFormatting.AQUA));
        mob.setCustomNameVisible(true);
        scaleBase(mob, horde, 1.0, 1.0);
        mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.FISHING_ROD));
        mob.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    static void maybeConfigureCityGrappler(Mob mob) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (id == null) return;
        boolean zombie = mob instanceof Zombie
            || id.getNamespace().equals("zombie_extreme") && Set.of("infected", "crawler", "runner",
                "clicker", "revived", "infected_police").contains(id.getPath())
            || id.toString().equals("born_in_chaos_v1:decaying_zombie");
        if (zombie && mob.getRandom().nextDouble() < HordeRoleConfig.fisherChance()) {
            configureFisher(mob, 0);
        }
    }

    private static void configureBreaker(Mob mob, int horde) {
        mob.addTag(ROLE_PREFIX + "breaker");
        mob.setCustomName(Component.literal("Miner").withStyle(ChatFormatting.GOLD));
        mob.setCustomNameVisible(true);
        scaleBase(mob, horde, 1.20, 1.10);
        equip(mob, EquipmentSlot.MAINHAND, "minecraft:iron_pickaxe", 0.0F, false);
        if (mob instanceof Zombie zombie) zombie.setCanBreakDoors(true);
    }

    private static void configureTank(Mob mob, int horde, int set) {
        mob.addTag(ROLE_PREFIX + "tank");
        mob.setCustomName(Component.literal("Tanker").withStyle(ChatFormatting.DARK_RED));
        mob.setCustomNameVisible(true);
        setBase(mob, Attributes.MAX_HEALTH, 46.0 + horde * 2.0);
        setBase(mob, Attributes.ARMOR, 14.0);
        setBase(mob, Attributes.ARMOR_TOUGHNESS, 4.0);
        setBase(mob, Attributes.KNOCKBACK_RESISTANCE, 0.65);
        setBase(mob, Attributes.MOVEMENT_SPEED, 0.18);
        mob.setHealth(mob.getMaxHealth());
        EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
        for (int i = 0; i < slots.length; i++) equip(mob, slots[i], TANK_ARMOR[set][i], 0.01F, true);
        mob.refreshDimensions();
    }

    private static void configureDisruptor(Mob mob, int horde) {
        mob.addTag(ROLE_PREFIX + "disruptor");
        mob.setCustomName(Component.literal("Disruptor").withStyle(ChatFormatting.DARK_GREEN));
        mob.setCustomNameVisible(true);
        scaleBase(mob, horde, 1.25, 1.10);
        equip(mob, EquipmentSlot.OFFHAND, "minecraft:fermented_spider_eye", 0.0F, false);
    }

    private static void configureSummoner(Mob mob, int horde) {
        mob.addTag(ROLE_PREFIX + "summoner");
        mob.setCustomName(Component.literal("Summoner").withStyle(ChatFormatting.DARK_PURPLE));
        mob.setCustomNameVisible(true);
        setBase(mob, Attributes.MAX_HEALTH, 38.0 + horde * 2.0);
        setBase(mob, Attributes.ARMOR, 8.0);
        setBase(mob, Attributes.MOVEMENT_SPEED, 0.17);
        mob.setHealth(mob.getMaxHealth());
        equip(mob, EquipmentSlot.MAINHAND, "minecraft:totem_of_undying", 0.0F, false);
        mob.getPersistentData().putInt("7dtc_summon_cooldown", 20 + mob.getRandom().nextInt(10));
    }

    private static void scaleBase(Mob mob, int horde, double health, double damage) {
        var hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(hp.getBaseValue() * health * (1.0 + Math.min(horde, 10) * 0.03));
        var attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(attack.getBaseValue() * damage * (1.0 + Math.min(horde, 10) * 0.02));
        mob.setHealth(mob.getMaxHealth());
    }

    private static void tickSummoner(Mob summoner) {
        int cooldown = summoner.getPersistentData().getInt("7dtc_summon_cooldown") - 1;
        if (cooldown > 0) { summoner.getPersistentData().putInt("7dtc_summon_cooldown", cooldown); return; }
        summoner.getPersistentData().putInt("7dtc_summon_cooldown", 25 + summoner.getRandom().nextInt(11));
        ServerLevel level = (ServerLevel) summoner.level();
        List<Zombie> nearby = level.getEntitiesOfClass(Zombie.class, summoner.getBoundingBox().inflate(24), z -> z.getTags().contains(SUMMONED));
        if (nearby.size() >= 4) return;
        for (int i = 0; i < 2 && nearby.size() + i < 4; i++) {
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie == null) continue;
            double x = summoner.getX() + summoner.getRandom().nextInt(7) - 3;
            double z = summoner.getZ() + summoner.getRandom().nextInt(7) - 3;
            zombie.moveTo(x, summoner.getY(), z, summoner.getYRot(), 0);
            zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(zombie.blockPosition()), MobSpawnType.EVENT, null, null);
            zombie.addTag(SUMMONED);
            zombie.addTag(HORDE_SPAWN);
            zombie.setCustomName(Component.literal("Raised Zombie").withStyle(ChatFormatting.DARK_PURPLE));
            zombie.setCustomNameVisible(false);
            zombie.setTarget(summoner.getTarget());
            level.addFreshEntity(zombie);
        }
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, summoner.getX(), summoner.getY() + 1, summoner.getZ(), 24, 1.0, 0.7, 1.0, 0.05);
    }

    private static void breakObstacle(Zombie zombie) {
        LivingEntity target = zombie.getTarget();
        if (target == null || zombie.distanceToSqr(target) > 36 || zombie.hasLineOfSight(target)) return;
        int dx = Integer.signum(Mth.floor(target.getX()) - zombie.blockPosition().getX());
        int dz = Integer.signum(Mth.floor(target.getZ()) - zombie.blockPosition().getZ());
        if (dx == 0 && dz == 0) return;
        ServerLevel level = (ServerLevel) zombie.level();
        for (int dy = 1; dy >= 0; dy--) {
            BlockPos pos = zombie.blockPosition().offset(dx, dy, dz);
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (!state.isAir() && level.getBlockEntity(pos) == null && hardness >= 0 && hardness <= 3.0F
                && !id.getPath().contains("bedrock") && !id.getPath().contains("obsidian")) {
                level.destroyBlock(pos, false, zombie);
                break;
            }
        }
    }

    private static void spawnBoss(HordeStartEvent event, int index) {
        ServerPlayer player = event.getPlayer();
        ServerLevel level = (ServerLevel) player.level();
        String id = MUTANT_BOSSES[(hordeNumber(event.getDay()) + index) % MUTANT_BOSSES.length];
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(id));
        Entity entity = type.create(level);
        if (!(entity instanceof Mob boss)) return;
        double angle = event.getRandom().nextDouble() * Math.PI * 2.0;
        int distance = 42 + event.getRandom().nextInt(14);
        int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        boss.moveTo(x + 0.5, y, z + 0.5, event.getRandom().nextFloat() * 360.0F, 0);
        boss.addTag(HORDE_BOSS);
        boss.addTag(HORDE_SPAWN);
        boss.setTarget(player);
        level.addFreshEntity(boss);
        event.getHorde().registerEntity(boss, player);
        player.displayClientMessage(Component.literal("A MUTANT BOSS HAS JOINED THE HORDE").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
    }

    private static void equip(Mob mob, EquipmentSlot slot, String id, float dropChance, boolean damage) {
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
        ItemStack stack = new ItemStack(item);
        if (damage && stack.isDamageableItem()) {
            double remaining = 0.40 + mob.getRandom().nextDouble() * 0.40;
            stack.setDamageValue((int) Math.round(stack.getMaxDamage() * (1.0 - remaining)));
        }
        mob.setItemSlot(slot, stack);
        mob.setDropChance(slot, dropChance);
    }

    private static void setBase(Mob mob, net.minecraft.world.entity.ai.attributes.Attribute attribute, double value) {
        var instance = mob.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }

    private static boolean hasRole(Entity entity, String role) {
        return entity.getTags().contains(ROLE_PREFIX + role)
            || (entity.getCustomName() != null && entity.getCustomName().getString().equalsIgnoreCase(role));
    }

    private static float cooperativeMultiplier(int onlinePlayers) {
        return 1.0F + 0.15F * Math.min(5, Math.max(0, onlinePlayers - 1));
    }

    private static boolean hasActiveHorde(ServerLevel level) {
        HordeSavedData data = HordeSavedData.getData(level);
        for (ServerPlayer player : level.players()) {
            HordeEvent event = data.getEvent(player);
            if (event != null && event.isActive()) return true;
        }
        return false;
    }

    private static int hordeNumber(int day) { return Math.max(1, day / 7); }

    @SubscribeEvent
    public static void resizeTank(EntityEvent.Size event) {
        if (hasRole(event.getEntity(), "tank")) {
            event.setNewSize(event.getNewSize().scale(1.12F));
            event.setNewEyeHeight(event.getNewEyeHeight() * 1.12F);
        }
    }
    @Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ClientScale {
        private ClientScale() {}

        @SubscribeEvent
        public static void renderPre(RenderLivingEvent.Pre<?, ?> event) {
            if (hasRole(event.getEntity(), "tank")) {
                event.getPoseStack().pushPose();
                event.getPoseStack().scale(1.22F, 1.22F, 1.22F);
            }
        }

        @SubscribeEvent
        public static void renderPost(RenderLivingEvent.Post<?, ?> event) {
            if (hasRole(event.getEntity(), "tank")) event.getPoseStack().popPose();
        }
    }
}
