package dev.sevendaystocraft.skillbridge;

import com.alrex.parcool.api.unstable.Limitation;
import com.alrex.parcool.api.Stamina;
import com.mojang.logging.LogUtils;
import com.google.gson.JsonParser;
import com.alrex.parcool.common.action.impl.*;
import com.alrex.parcool.common.capability.Parkourability;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.puffish.skillsmod.api.SkillsAPI;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioEquipEvent;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;

import java.util.Set;
import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.UUID;
import org.slf4j.Logger;

@Mod(SkillBridge.MOD_ID)
public final class SkillBridge {
    public static final String MOD_ID = "seven_days_skill_bridge";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final String PROTOCOL = "5";
    private static final ResourceLocation SURVIVOR = new ResourceLocation("seven_days_to_craft", "survivor");
    private static volatile Map<String, String> SKILL_DEFINITIONS = Map.of();
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(MOD_ID, "main"))
        .networkProtocolVersion(() -> PROTOCOL)
        .clientAcceptedVersions(PROTOCOL::equals)
        .serverAcceptedVersions(PROTOCOL::equals)
        .simpleChannel();

    public SkillBridge() {
        CHANNEL.registerMessage(0, SkillPointsPacket.class, SkillPointsPacket::encode, SkillPointsPacket::decode, SkillPointsPacket::handle);
        CHANNEL.registerMessage(1, SkillUnlockNoticePacket.class, SkillUnlockNoticePacket::encode, SkillUnlockNoticePacket::decode, SkillUnlockNoticePacket::handle);
        SkillsAPI.registerSkillUnlockEvent((player, category, skillId) -> { if (SURVIVOR.equals(category)) Events.onSkillUnlocked(player, skillId); });
        SkillsAPI.registerSkillLockEvent((player, category, skillId) -> { if (SURVIVOR.equals(category)) Events.onSkillLocked(player, skillId); });
    }

    /**
     * Reads the live graph instead of duplicating ability ranks in scoreboard tags.
     * Positional node ids stay unique (combat_1_slot01, ...), while every repeated
     * effect points to one canonical definition (combat_1_meleetraining, ...).
     */
    public static synchronized void reloadSkillDefinitionIndex() {
        Path skillsFile = FMLPaths.GAMEDIR.get().resolve("kubejs/data/seven_days_to_craft/puffish_skills/categories/survivor/skills.json");
        Map<String, String> definitions = new HashMap<>();
        try {
            var root = JsonParser.parseString(Files.readString(skillsFile)).getAsJsonObject();
            for (var entry : root.entrySet()) {
                if (!entry.getValue().isJsonObject()) continue;
                var node = entry.getValue().getAsJsonObject();
                if (node.has("definition")) definitions.put(entry.getKey(), node.get("definition").getAsString());
            }
            SKILL_DEFINITIONS = Map.copyOf(definitions);
            LOGGER.info("Indexed {} canonical skill assignments", definitions.size());
        } catch (Exception error) {
            SKILL_DEFINITIONS = Map.of();
            LOGGER.error("Could not index canonical skill assignments from {}", skillsFile, error);
        }
    }

    public static int getBasicSkillRank(ServerPlayer player, String abilityName, int max) {
        if (SKILL_DEFINITIONS.isEmpty()) reloadSkillDefinitionIndex();
        String suffix = "_1_" + abilityName;
        int rank = SkillsAPI.getCategory(SURVIVOR).map(category ->
            (int) category.streamUnlockedSkills(player)
                .filter(skill -> {
                    String definition = SKILL_DEFINITIONS.get(skill.getId());
                    return definition != null && definition.endsWith(suffix);
                })
                .count()
        ).orElse(0);
        return Math.min(max, rank);
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        private static final UUID SILENT_STEP_SPEED_ID = UUID.fromString("db60556c-41ba-4b50-a217-b2621dcafcc5");
        private static final UUID EVASIVE_RUSH_SPEED_ID = UUID.fromString("cbad898a-141c-46ee-921f-c3847436f3b7");
        private record Milestone(String tag, String advancement, long questId) {}
        private record UnlockNotice(String title, String message) {}
        private static final Map<String, UnlockNotice> UNLOCK_NOTICES = Map.ofEntries(
            Map.entry("survivor_root", new UnlockNotice("Survivor", "You can now see the current day and time.")),
            Map.entry("mobility_1_efficientmetabolism", new UnlockNotice("Efficient Metabolism", "Activities now consume less hunger.")),
            Map.entry("mobility_2_grapplerlicense", new UnlockNotice("Grappler License", "You can now use standard and motorized Grappling Hooks.")),
            Map.entry("mobility_2_advancedparkour", new UnlockNotice("Advanced Parkour", "You can now cling to ledges, vault, slide, wall-run and use the other advanced Parkour moves.")),
            Map.entry("mobility_2_lowprofile", new UnlockNotice("Low Profile", "Hostile creatures now have more difficulty detecting you at a distance.")),
            Map.entry("mobility_2_reservejump", new UnlockNotice("Reserve Jump", "You can now jump a second time while airborne. Cooldown: 30 seconds.")),
            Map.entry("mobility_2_beastrider", new UnlockNotice("Beast Rider", "You can now tame and ride supported Alex's Mobs mounts.")),
            Map.entry("mobility_2_evasiverush", new UnlockNotice("Evasive Rush", "Taking damage can now grant +25% movement speed for 5 seconds.")),
            Map.entry("engineering_1_workshopefficiency", new UnlockNotice("Workshop Efficiency", "Workbench crafting can now preserve a common component.")),
            Map.entry("engineering_1_maintenance", new UnlockNotice("Maintenance", "Equipment can now avoid durability loss when used.")),
            Map.entry("engineering_1_repairefficiency", new UnlockNotice("Repair Efficiency", "Repair Kits now restore more durability.")),
            Map.entry("engineering_2_mechanicalfoundations", new UnlockNotice("Mechanical Foundations", "You can now craft and use basic Create kinetic components.")),
            Map.entry("engineering_2_sentryengineering", new UnlockNotice("Sentry Engineering", "You can now build one Basic Turret.")),
            Map.entry("engineering_2_structuralengineering", new UnlockNotice("Structural Engineering", "Advanced fortification paths are now available.")),
            Map.entry("engineering_2_advancedmanufacturing", new UnlockNotice("Advanced Manufacturing", "Advanced Create components are now available.")),
            Map.entry("engineering_2_turretengineering", new UnlockNotice("Turret Engineering", "Auto and Shotgun Turrets are now available. Turret capacity: 2.")),
            Map.entry("scavenging_1_animalsense", new UnlockNotice("Animal Sense", "Passive food animals now appear nearby on Advanced Compass.")),
            Map.entry("scavenging_1_prospector", new UnlockNotice("Prospector", "Eligible ore blocks can now yield one extra raw ore.")),
            Map.entry("scavenging_1_forager", new UnlockNotice("Forager", "Wild Sweet Berry Bushes can now yield an extra berry.")),
            Map.entry("scavenging_1_scavenger", new UnlockNotice("Scavenger", "Newly opened generated loot can now contain one extra eligible stack.")),
            Map.entry("scavenging_2_oresense", new UnlockNotice("Ore Sense", "You can now see nearby ore blocks glowing in dark places (20 block range).")),
            Map.entry("scavenging_2_oresense2", new UnlockNotice("Ore Sense II", "Loaded ore blocks now glow without a distance limit.")),
            Map.entry("scavenging_2_salvager", new UnlockNotice("Salvager", "You can now place and use the Recycler.")),
            Map.entry("scavenging_2_lumberjack", new UnlockNotice("Lumberjack", "Breaking the lowest log with an axe can now fell the whole tree.")),
            Map.entry("scavenging_2_backpacktraining", new UnlockNotice("Backpack Training", "You can now equip and open Quark Backpacks in the Curios Back slot.")),
            Map.entry("scavenging_2_keyhunter", new UnlockNotice("Key Hunter", "Eligible enemies now have improved chances to drop dungeon keys.")),
            Map.entry("combat_1_graverunner", new UnlockNotice("Grave Runner", "Your post-death protection effects now last 60 seconds longer.")),
            Map.entry("combat_1_arrowretrieval", new UnlockNotice("Arrow Retrieval", "Recover fired arrows or CGM ammunition after hitting creatures.")),
            Map.entry("combat_2_embeddedarrows", new UnlockNotice("Embedded Arrows", "Monsters you shot can return one ordinary arrow after death (30% chance).")),
            Map.entry("combat_2_extendedreach", new UnlockNotice("Extended Reach", "Your attacks and entity interactions now reach 2 blocks farther.")),
            Map.entry("combat_2_fieldrecovery", new UnlockNotice("Field Recovery", "Golden Apples now restore 2 permanently lost hearts.")),
            Map.entry("combat_2_threatsense", new UnlockNotice("Threat Sense I", "Detect nearby hostile monsters within 10 blocks and receive Elite or Boss aggro warnings.")),
            Map.entry("combat_2_threatsense2", new UnlockNotice("Threat Sense II", "Detect hostile monsters within 30 blocks.")),
            Map.entry("combat_2_ranger", new UnlockNotice("Ranger", "Bows, crossbows and compatible firearms now deal 25% more damage.")),
            Map.entry("mobility_3_emergencymobility", new UnlockNotice("Emergency Mobility", "At critical health, you can gain speed and unlimited Parkour stamina.")),
            Map.entry("mobility_3_breakfallreflex", new UnlockNotice("Breakfall Reflex", "You can now completely prevent one fall every 30 seconds.")),
            Map.entry("mobility_3_endertraversal", new UnlockNotice("Ender Traversal", "You can now use the Ender Staff and Ender Grappling Hook.")),
            Map.entry("combat_3_enduringvitality", new UnlockNotice("Enduring Vitality", "After avoiding damage, you can now regenerate health over time.")),
            Map.entry("combat_3_paintolerance", new UnlockNotice("Pain Tolerance", "At critical health, you can now gain Resistance I.")),
            Map.entry("combat_3_secondwind", new UnlockNotice("Second Wind", "Killing a hostile creature can now restore health.")),
            Map.entry("scavenging_3_keyconservation", new UnlockNotice("Key Conservation", "Opening a locked chest now has a 30% chance to preserve its key.")),
            Map.entry("scavenging_3_darkadaptation", new UnlockNotice("Dark Adaptation", "You can now see more clearly in very dark areas.")),
            Map.entry("scavenging_3_mastersalvager", new UnlockNotice("Master Salvager", "Workbench dismantling now recovers more eligible materials.")),
            Map.entry("engineering_3_createautomation", new UnlockNotice("Create Automation", "Automatic assembly, logistics, trains and advanced Create systems are now available.")),
            Map.entry("engineering_3_fortressconstruction", new UnlockNotice("Fortress Construction", "You can now place reinforced Smooth Stone fortifications.")),
            Map.entry("engineering_3_automateddefense", new UnlockNotice("Automated Defense", "Explosive Turrets and advanced modules are now available. Turret capacity: 3."))
        );
        private static final List<Milestone> MILESTONES = List.of(
            new Milestone("7dtc_parcool_wall_grip","wall_grip",0x7D7C310000000001L),new Milestone("7dtc_grappler","grappler_license",0x7D7C310000000002L),new Milestone("7dtc_backpack","backpack_license",0x7D7C310000000003L),new Milestone("7dtc_ore_sense_2","ore_sense_2",0x7D7C310000000004L),new Milestone("7dtc_ore_sense","ore_sense",0x7D7C310000000005L),new Milestone("7dtc_lumberjack","lumberjack",0x7D7C310000000006L),new Milestone("7dtc_key_hunter","key_hunter",0x7D7C310000000007L),new Milestone("7dtc_salvager","salvager",0x7D7C310000000008L),new Milestone("7dtc_gunsmithing","gunsmithing",0x7D7C310000000009L),new Milestone("7dtc_ammunition_1","ammunition_1",0x7D7C31000000000AL),new Milestone("7dtc_ammunition_2","ammunition_2",0x7D7C31000000000BL),new Milestone("7dtc_mechanics_1","mechanics_1",0x7D7C31000000000CL),new Milestone("7dtc_mechanics_2","mechanics_2",0x7D7C31000000000DL),new Milestone("7dtc_automation","advanced_manufacturing",0x7D7C31000000000EL),new Milestone("7dtc_sentry_engineering","sentry_engineering",0x7D7C31000000000FL),new Milestone("7dtc_turret_engineering","turret_engineering",0x7D7C310000000010L),new Milestone("7dtc_adrenaline_rush","emergency_mobility",0x7D7C310000000011L),new Milestone("7dtc_breakfall_reflex","breakfall_reflex",0x7D7C310000000012L),new Milestone("7dtc_ender_traversal","ender_traversal",0x7D7C310000000013L),new Milestone("7dtc_pain_tolerance","pain_tolerance",0x7D7C310000000014L),new Milestone("7dtc_second_wind","second_wind",0x7D7C310000000015L),new Milestone("7dtc_enduring_vitality","enduring_vitality",0x7D7C310000000016L),new Milestone("7dtc_key_conservation","key_conservation",0x7D7C310000000017L),new Milestone("7dtc_dark_adaptation","dark_adaptation",0x7D7C310000000018L),new Milestone("7dtc_master_salvager","master_salvager",0x7D7C310000000019L),new Milestone("7dtc_create_automation","create_automation",0x7D7C31000000001AL),new Milestone("7dtc_fortress_construction","fortress_construction",0x7D7C31000000001BL),new Milestone("7dtc_automated_defense","automated_defense",0x7D7C31000000001CL),
            new Milestone("7dtc_low_profile","low_profile",0x7D7C31000000001FL),new Milestone("7dtc_double_jump","double_jump",0x7D7C310000000020L),new Milestone("7dtc_beast_rider","beast_rider",0x7D7C310000000021L),new Milestone("7dtc_threat_sense","threat_sense",0x7D7C310000000023L),new Milestone("7dtc_ranger","ranger",0x7D7C310000000024L),new Milestone("7dtc_evasive_rush","evasive_rush",0x7D7C310000000025L),new Milestone("7dtc_extended_reach","extended_reach",0x7D7C310000000026L));
        private static final Map<String,String> FINAL_SKILLS=Map.ofEntries(Map.entry("mobility_3_slot01","mobility:emergency_mobility"),Map.entry("mobility_3_slot02","mobility:breakfall_reflex"),Map.entry("mobility_3_slot03","mobility:ender_traversal"),Map.entry("combat_3_slot01","combat:enduring_vitality"),Map.entry("combat_3_slot02","combat:pain_tolerance"),Map.entry("combat_3_slot03","combat:second_wind"),Map.entry("scavenging_3_slot01","scavenging:key_conservation"),Map.entry("scavenging_3_slot02","scavenging:dark_adaptation"),Map.entry("scavenging_3_slot03","scavenging:master_salvager"),Map.entry("engineering_3_slot01","engineering:create_automation"),Map.entry("engineering_3_slot02","engineering:fortress_construction"),Map.entry("engineering_3_slot03","engineering:automated_defense"));
        private static final Map<String,String> PATH_STARTS=Map.ofEntries(
            Map.entry("mobility_2_slot02","mobility"),Map.entry("mobility_1_slot01","mobility"),Map.entry("mobility_1_slot27","mobility"),
            Map.entry("combat_1_slot01","combat"),Map.entry("combat_1_slot02","combat"),Map.entry("combat_1_slot26","combat"),
            Map.entry("scavenging_1_slot01","scavenging"),Map.entry("scavenging_1_slot02","scavenging"),Map.entry("scavenging_1_slot24","scavenging"),
            Map.entry("engineering_1_slot02","engineering"),Map.entry("engineering_1_slot26","engineering"),Map.entry("engineering_1_slot01","engineering"));
        private record RepeatGate(String capability, String tag, int cost) {}
        private static final Map<String,RepeatGate> REPEAT_GATES=Map.ofEntries(
            Map.entry("combat_2_slot01",new RepeatGate("field_recovery","7dtc_field_recovery",2)),
            Map.entry("engineering_2_slot04",new RepeatGate("automation","7dtc_automation",4)),
            Map.entry("engineering_2_slot06",new RepeatGate("automation","7dtc_automation",4))
        );
        static void onSkillUnlocked(ServerPlayer player,String skillId){
            String slot=structuralSlot(skillId);
            String pathBranch=PATH_STARTS.get(slot);if(pathBranch!=null&&!acceptPathChoice(player,skillId,pathBranch))return;
            String f=FINAL_SKILLS.get(slot);if(f!=null)masterBranch(player,f);
            RepeatGate gate=REPEAT_GATES.get(slot);
            if(gate!=null){String learned="7dtc_repeat_learned_"+gate.capability(),refunded="7dtc_repeat_refunded_"+slot;if(player.getPersistentData().getBoolean(learned)){SkillsAPI.getCategory(SURVIVOR).ifPresent(c->c.addExtraPoints(player,gate.cost()));player.getPersistentData().putBoolean(refunded,true);player.displayClientMessage(Component.literal("Shared milestone already learned: skill point cost refunded.").withStyle(net.minecraft.ChatFormatting.GREEN),true);}else player.getPersistentData().putBoolean(learned,true);}
            sendUnlockNotice(player, skillId);
            syncPoints(player, true);
            player.server.execute(()->discoverMilestones(player,true));
        }

        private static void sendUnlockNotice(ServerPlayer player, String skillId) {
            if (SKILL_DEFINITIONS.isEmpty()) reloadSkillDefinitionIndex();
            String definition = SKILL_DEFINITIONS.get(skillId);
            UnlockNotice notice = definition == null ? null : UNLOCK_NOTICES.get(definition);
            if (notice == null) return;
            long unlockedCopies = SkillsAPI.getCategory(SURVIVOR).map(category ->
                category.streamUnlockedSkills(player)
                    .filter(skill -> definition.equals(SKILL_DEFINITIONS.get(skill.getId())))
                    .count()
            ).orElse(0L);
            if (unlockedCopies != 1L) return;
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SkillUnlockNoticePacket(notice.title(), notice.message()));
        }

        private static boolean hasUnlockedDefinition(ServerPlayer player, String definition) {
            if (SKILL_DEFINITIONS.isEmpty()) reloadSkillDefinitionIndex();
            return SkillsAPI.getCategory(SURVIVOR).map(category ->
                category.streamUnlockedSkills(player)
                    .anyMatch(skill -> definition.equals(SKILL_DEFINITIONS.get(skill.getId())))
            ).orElse(false);
        }
        static void onSkillLocked(ServerPlayer player,String skillId){
            String slot=structuralSlot(skillId);
            String value=FINAL_SKILLS.get(slot);if(value!=null){String branch=value.substring(0,value.indexOf(':'));player.server.execute(()->clearBranchMasteryIfNoFinalRemains(player,branch));}
            RepeatGate gate=REPEAT_GATES.get(slot);if(gate==null)return;
            String refunded="7dtc_repeat_refunded_"+slot;
            if(player.getPersistentData().getBoolean(refunded)){SkillsAPI.getCategory(SURVIVOR).ifPresent(c->c.addExtraPoints(player,-gate.cost()));player.getPersistentData().remove(refunded);}
            player.server.execute(()->{if(!player.getTags().contains(gate.tag()))player.getPersistentData().remove("7dtc_repeat_learned_"+gate.capability());});
        }
        private static void masterBranch(ServerPlayer player,String value){String branch=value.substring(0,value.indexOf(':'));player.getPersistentData().putBoolean("7dtc_mastered_"+branch,true);player.getPersistentData().putString("7dtc_first_final_"+branch,value);player.displayClientMessage(Component.literal("Branch mastered: the other "+branch+" paths are now available.").withStyle(net.minecraft.ChatFormatting.GREEN),true);}
        private static boolean acceptPathChoice(ServerPlayer player,String skillId,String branch){if(player.getPersistentData().getBoolean("7dtc_mastered_"+branch))return true;String currentSlot=structuralSlot(skillId);boolean other=SkillsAPI.getCategory(SURVIVOR).map(c->PATH_STARTS.entrySet().stream().anyMatch(e->e.getValue().equals(branch)&&!e.getKey().equals(currentSlot)&&skillAtSlot(c,e.getKey()).map(s->s.getState(player)==net.puffish.skillsmod.api.Skill.State.UNLOCKED).orElse(false))).orElse(false);if(!other)return true;SkillsAPI.getCategory(SURVIVOR).flatMap(c->c.getSkill(skillId)).ifPresent(s->s.lock(player));player.displayClientMessage(Component.literal("Complete a final "+branch+" milestone before starting another path in this branch.").withStyle(net.minecraft.ChatFormatting.RED),false);return false;}
        private static void clearBranchMasteryIfNoFinalRemains(ServerPlayer player,String branch){boolean any=SkillsAPI.getCategory(SURVIVOR).map(c->FINAL_SKILLS.entrySet().stream().anyMatch(e->e.getValue().startsWith(branch+":")&&skillAtSlot(c,e.getKey()).map(s->s.getState(player)==net.puffish.skillsmod.api.Skill.State.UNLOCKED).orElse(false))).orElse(false);if(!any){player.getPersistentData().remove("7dtc_mastered_"+branch);player.getPersistentData().remove("7dtc_first_final_"+branch);}}
        private static void restoreBranchMastery(ServerPlayer player){SkillsAPI.getCategory(SURVIVOR).ifPresent(c->FINAL_SKILLS.forEach((slot,value)->skillAtSlot(c,slot).filter(s->s.getState(player)==net.puffish.skillsmod.api.Skill.State.UNLOCKED).ifPresent(s->{String branch=value.substring(0,value.indexOf(':'));player.getPersistentData().putBoolean("7dtc_mastered_"+branch,true);if(player.getPersistentData().getString("7dtc_first_final_"+branch).isEmpty())player.getPersistentData().putString("7dtc_first_final_"+branch,value);})));}
        private static String structuralSlot(String skillId){return skillId;}
        private static java.util.Optional<net.puffish.skillsmod.api.Skill> skillAtSlot(net.puffish.skillsmod.api.Category category,String slot){return category.getSkill(slot);}
        private static void discoverMilestones(ServerPlayer player,boolean pin){for(Milestone m:MILESTONES){if(!player.getTags().contains(m.tag()))continue;String marker="7dtc_milestone_"+m.advancement();boolean seen=player.getPersistentData().getBoolean(marker);award(player,new ResourceLocation("seven_days_to_craft","skills/"+m.advancement()));if(!seen){player.getPersistentData().putBoolean(marker,true);if(pin)pinQuest(player,m.questId());}}}
        private static void pinQuest(ServerPlayer player,long id){if(ServerQuestFile.INSTANCE==null)return;Quest q=ServerQuestFile.INSTANCE.getQuest(id);if(q!=null)TeamData.get(player).setQuestPinned(player,id,true);}
        private static final Class<?>[] ALL = {
            BreakfallReady.class, CatLeap.class, ClimbPoles.class, ClimbUp.class,
            ClingToCliff.class, Crawl.class, Dive.class, Dodge.class, FastRun.class,
            FastSwim.class, Flipping.class, HangDown.class, HorizontalWallRun.class,
            JumpFromBar.class, QuickTurn.class, Roll.class, SkyDive.class, Slide.class,
            Tap.class, Vault.class, VerticalWallRun.class, WallJump.class, WallSlide.class
        };
        private static final Class<?>[] BASIC = { ClingToCliff.class, ClimbUp.class };
        private static final Class<?>[] ADVANCED = {
            BreakfallReady.class, Crawl.class, Dive.class, FastRun.class,
            FastSwim.class, SkyDive.class, Slide.class, Vault.class,
            CatLeap.class, ClimbPoles.class, HangDown.class,
            HorizontalWallRun.class, JumpFromBar.class,
            VerticalWallRun.class, WallJump.class, WallSlide.class
        };

        @SubscribeEvent
        public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                reloadSkillDefinitionIndex();
                SkillTreeRevisionReset.apply(player);
                restoreBranchMastery(player);
                apply(player);
                syncPoints(player, true);
                restoreOnboardingCraftProgress(player);
                discoverMilestones(player, false);
            }
        }        @SubscribeEvent
        public static void onSmoothStonePlaced(BlockEvent.EntityPlaceEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            if (event.getPlacedBlock().is(Blocks.SMOOTH_STONE) && !player.getTags().contains("7dtc_fortress_construction")) {
                event.setCanceled(true);
                player.displayClientMessage(Component.literal("Fortress Construction is required to place Smooth Stone.").withStyle(net.minecraft.ChatFormatting.RED), true);
                return;
            }
            if (event.getPlacedBlock().is(Blocks.SMOOTH_STONE) && player.getTags().contains("7dtc_fortress_construction")) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/fortress_place"));
            }
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock());
            if (blockId.toString().equals("corail_recycler:recycler") && !player.getTags().contains("7dtc_salvager")) {
                event.setCanceled(true);
                player.displayClientMessage(Component.literal("Salvager is required to place the Recycler.").withStyle(net.minecraft.ChatFormatting.RED), true);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void protectOwnedGraves(BlockEvent.BreakEvent event) {
            if (!(event.getPlayer() instanceof ServerPlayer player)) return;
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());
            if (!"gravestone:gravestone".equals(blockId.toString())) return;
            var blockEntity = event.getLevel().getBlockEntity(event.getPos());
            if (blockEntity == null) {
                event.setCanceled(true);
                return;
            }
            try {
                Object death = blockEntity.getClass().getMethod("getDeath").invoke(blockEntity);
                UUID owner = (UUID) death.getClass().getMethod("getPlayerUUID").invoke(death);
                if (!player.getUUID().equals(owner)) {
                    event.setCanceled(true);
                    player.displayClientMessage(Component.literal("Only the owner can break this grave.")
                        .withStyle(net.minecraft.ChatFormatting.RED), true);
                }
            } catch (ReflectiveOperationException error) {
                event.setCanceled(true);
                LOGGER.error("Could not verify grave ownership at {}; denying the break safely", event.getPos(), error);
            }
        }

        @SubscribeEvent
        public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            if (event.getCrafting().is(Items.CRAFTING_TABLE)) {
                award(player, new ResourceLocation("seven_days_to_craft", "onboarding/crafting_table"));
            }
            if (event.getCrafting().is(ItemTags.BEDS)) {
                award(player, new ResourceLocation("seven_days_to_craft", "onboarding/any_bed"));
            }
            if (event.getCrafting().is(Items.FURNACE)) {
                award(player, new ResourceLocation("seven_days_to_craft", "onboarding/furnace"));
            }
            if (event.getCrafting().is(BuiltInRegistries.ITEM.get(new ResourceLocation("cgm", "workbench")))) {
                award(player, new ResourceLocation("seven_days_to_craft", "onboarding/workbench"));
            }
        }

        @SubscribeEvent
        public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
            Player player = event.getEntity();
            if (!(player instanceof ServerPlayer serverPlayer)) return;
            int rank = getBasicSkillRank(serverPlayer, "toolproficiency", 6);
            if (rank > 0 && player.getMainHandItem().isCorrectToolForDrops(event.getState())) {
                event.setNewSpeed(event.getNewSpeed() * (1.0F + 0.10F * rank));
            }
        }

        @SubscribeEvent
        public static void onRepairKitUse(PlayerInteractEvent.RightClickItem event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            if (!BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()).toString().equals("iron_repair_kits:copper_repair_kit")) return;
            int rank = getBasicSkillRank(player, "repairefficiency", 4);
            var target = player.getOffhandItem();
            if (rank <= 0 || target.isEmpty() || !target.isDamaged()) return;
            var data = player.getPersistentData();
            data.putInt("7dtc_repair_before", target.getDamageValue());
            data.putInt("7dtc_repair_rank", rank);
            data.putLong("7dtc_repair_check_until", player.level().getGameTime() + 5L);
        }

        @SubscribeEvent
        public static void onMerchantInteract(PlayerInteractEvent.EntityInteract event) {
            if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof AbstractVillager villager)) return;
            int rank = getBasicSkillRank(player, "haggler", 4);
            var data = player.getPersistentData();
            if (rank <= 0 || data.getBoolean("7dtc_haggler_active")) return;
            int[] discounts = new int[villager.getOffers().size()];
            for (int i = 0; i < discounts.length; i++) {
                MerchantOffer offer = villager.getOffers().get(i);
                discounts[i] = Math.max(1, (int)Math.floor(offer.getBaseCostA().getCount() * 0.04D * rank));
                offer.addToSpecialPriceDiff(-discounts[i]);
            }
            data.putBoolean("7dtc_haggler_active", true);
            data.putInt("7dtc_haggler_entity", villager.getId());
            data.putIntArray("7dtc_haggler_discounts", discounts);
        }

        @SubscribeEvent
        public static void onMerchantClosed(PlayerContainerEvent.Close event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            var data = player.getPersistentData();
            if (!data.getBoolean("7dtc_haggler_active")) return;
            var entity = player.serverLevel().getEntity(data.getInt("7dtc_haggler_entity"));
            if (entity instanceof AbstractVillager villager) {
                int[] discounts = data.getIntArray("7dtc_haggler_discounts");
                for (int i = 0; i < Math.min(discounts.length, villager.getOffers().size()); i++) villager.getOffers().get(i).addToSpecialPriceDiff(discounts[i]);
            }
            data.remove("7dtc_haggler_active");
            data.remove("7dtc_haggler_entity");
            data.remove("7dtc_haggler_discounts");
        }

        @SubscribeEvent
        public static void onPlayerHurt(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
            if (event.getSource().is(DamageTypes.FALL) && player.getTags().contains("7dtc_breakfall_reflex")) {
                long now = player.level().getGameTime();
                var data = player.getPersistentData();
                if (now >= data.getLong("7dtc_breakfall_cooldown_until")) {
                    event.setCanceled(true);
                    data.putLong("7dtc_breakfall_cooldown_until", now + 600L);
                    award((ServerPlayer) player, new ResourceLocation("seven_days_to_craft", "training/breakfall_ready"));
                    award((ServerPlayer) player, new ResourceLocation("seven_days_to_craft", "training/breakfall_fall"));
                    award((ServerPlayer) player, new ResourceLocation("seven_days_to_craft", "training/breakfall_prevented"));
                    player.displayClientMessage(Component.literal("Breakfall Reflex prevented fall damage!"), true);
                    return;
                }
            }
            if (event.getSource().is(DamageTypes.FALL) && player.getTags().contains("7dtc_parcool_wall_grip")) player.getPersistentData().putBoolean("7dtc_wall_grip_fall_since_cling", true);
            player.getPersistentData().putLong("7dtc_last_damage_tick", player.level().getGameTime());
            triggerEvasiveRush(player);
            if (player instanceof ServerPlayer serverPlayer && player.getTags().contains("7dtc_enduring_vitality")) {
                award(serverPlayer, new ResourceLocation("seven_days_to_craft", "training/enduring_vitality_damage"));
            }
            if (!(event.getSource().getEntity() instanceof Mob)) return;

            long survivalDay = Math.floorDiv(player.level().getDayTime() + 6000L, 24000L) + 1L;
            if (survivalDay <= 3L) {
                event.setAmount(event.getAmount() * 0.80F);
            } else if (survivalDay <= 6L) {
                event.setAmount(event.getAmount() * 0.90F);
            }
        }

        @SubscribeEvent
        public static void onRangedDamage(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof net.minecraft.world.entity.monster.Monster)) return;
            if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
            if (!attacker.getTags().contains("7dtc_ranger")) return;
            if (event.getSource().getDirectEntity() == attacker) return;
            award(attacker, new ResourceLocation("seven_days_to_craft", "training/ranger_hit"));
        }

        @SubscribeEvent
        public static void onExtendedReachHit(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof Enemy)) return;
            if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
            if (event.getSource().getDirectEntity() != attacker || !attacker.getTags().contains("7dtc_extended_reach")) return;
            if (attacker.distanceToSqr(event.getEntity()) >= 12.25D) {
                award(attacker, new ResourceLocation("seven_days_to_craft", "training/extended_reach_hit"));
            }
        }

        @SubscribeEvent
        public static void onHostileKilled(LivingDeathEvent event) {
            if (!(event.getSource().getEntity() instanceof ServerPlayer player) || !(event.getEntity() instanceof net.minecraft.world.entity.monster.Monster)) return;
            if (player.getTags().contains("7dtc_key_hunter")) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/key_hunter_kill"));
            }
            if (!player.getTags().contains("7dtc_second_wind") || player.getHealth() >= player.getMaxHealth()) return;
            long now = player.level().getGameTime();
            var data = player.getPersistentData();
            if (now < data.getLong("7dtc_second_wind_cooldown_until")) return;
            data.putLong("7dtc_second_wind_cooldown_until", now + 200L);
            data.putLong("7dtc_second_wind_next_heal", now + 20L);
            data.putInt("7dtc_second_wind_heals_remaining", 4);
            award(player, new ResourceLocation("seven_days_to_craft", "training/second_wind_wounded_kill"));
            award(player, new ResourceLocation("seven_days_to_craft", "training/second_wind_trigger"));
            player.displayClientMessage(Component.literal("Second Wind activated!"), true);
        }

        private static void trackWallGripTraining(ServerPlayer player) {
            var data = player.getPersistentData();
            if (!player.getTags().contains("7dtc_parcool_wall_grip")) {
                data.remove("7dtc_wall_grip_prev_cling");
                data.remove("7dtc_wall_grip_prev_climb");
                return;
            }

            Parkourability ability = Parkourability.get(player);
            // ParCool attaches this capability asynchronously when a player joins.
            // On an integrated server, a remote player can tick once before it is
            // available. Training can safely wait for the following tick.
            if (ability == null) {
                data.remove("7dtc_wall_grip_prev_cling");
                data.remove("7dtc_wall_grip_prev_climb");
                return;
            }
            ClingToCliff clingAbility = ability.get(ClingToCliff.class);
            ClimbUp climbAbility = ability.get(ClimbUp.class);
            if (clingAbility == null || climbAbility == null) return;
            boolean clinging = clingAbility.isDoing();
            boolean climbing = climbAbility.isDoing();
            boolean previousCling = data.getBoolean("7dtc_wall_grip_prev_cling");
            boolean previousClimb = data.getBoolean("7dtc_wall_grip_prev_climb");

            if (clinging && !previousCling) {
                data.putInt("7dtc_wall_grip_cling_count", data.getInt("7dtc_wall_grip_cling_count") + 1);
                data.putBoolean("7dtc_wall_grip_fall_since_cling", false);
                award(player, new ResourceLocation("seven_days_to_craft", "training/wall_grip_cling"));
            }
            if (climbing && !previousClimb) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/wall_grip_climb"));
                if (data.getInt("7dtc_wall_grip_cling_count") >= 2 && !data.getBoolean("7dtc_wall_grip_fall_since_cling")) {
                    award(player, new ResourceLocation("seven_days_to_craft", "training/wall_grip_repeat"));
                }
            }

            data.putBoolean("7dtc_wall_grip_prev_cling", clinging);
            data.putBoolean("7dtc_wall_grip_prev_climb", climbing);
        }
        private static void restoreOnboardingCraftProgress(ServerPlayer player) {
            if (player.getStats().getValue(Stats.ITEM_CRAFTED.get(Items.CRAFTING_TABLE)) > 0) {
                award(player, new ResourceLocation("seven_days_to_craft", "onboarding/crafting_table"));
            }
            boolean craftedBed = BuiltInRegistries.ITEM.stream()
                .filter(item -> item.builtInRegistryHolder().is(ItemTags.BEDS))
                .anyMatch(item -> player.getStats().getValue(Stats.ITEM_CRAFTED.get(item)) > 0);
            if (craftedBed) {
                award(player, new ResourceLocation("seven_days_to_craft", "onboarding/any_bed"));
            }
            if (player.getStats().getValue(Stats.ITEM_CRAFTED.get(Items.FURNACE)) > 0) {
                award(player, new ResourceLocation("seven_days_to_craft", "onboarding/furnace"));
            }
        }
        @SubscribeEvent
        public static void onTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;

            trackWallGripTraining(player);
            applyRepairEfficiency(player);
            applyEfficientMetabolism(player);
            applyLowHealthPassives(player);
            applySecondWind(player);
            applyPainToleranceTraining(player);
            applyEnduringVitality(player);
            applyDarkAdaptation(player);
            applyMovementSkills(player);
            trackLowProfileTraining(player);
            enforceBackpackAccess(player);
            enforceTraversalEquipment(player);
            if (player.tickCount % 5 == 0) {
                trackPossessionTraining(player);
                trackOnboardingArmorSet(player);
                if (player.getInventory().contains(new ItemStack(Items.FURNACE))) {
                    award(player, new ResourceLocation("seven_days_to_craft", "onboarding/furnace"));
                }
            }
            if (player.tickCount % 20 == 0) {
                apply(player);
                syncPoints(player, false);
                trackWrdExpedition(player);
            }
        }

        private static void trackWrdExpedition(ServerPlayer player) {
            if (!player.getPersistentData().getBoolean("7dtc_wrd_contact")) {
                boolean contact = player.level().dimension().location().getNamespace().equals("wrd");
                BlockPos center = player.blockPosition();
                for (int x = -6; !contact && x <= 6; x++) for (int y = -4; !contact && y <= 4; y++) for (int z = -6; !contact && z <= 6; z++) {
                    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(center.offset(x, y, z)).getBlock());
                    if (blockId != null && blockId.getNamespace().equals("wrd")) contact = true;
                }
                if (contact) {
                    player.getPersistentData().putBoolean("7dtc_wrd_contact", true);
                    award(player, new ResourceLocation("seven_days_to_craft", "wrd/first_contact"));
                }
            }

            boolean hasDungeonKey = player.getInventory().items.stream().anyMatch(stack -> {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                return !stack.isEmpty() && id != null && id.getNamespace().equals("wrd") && id.getPath().contains("keystrongholdchest");
            });
            if (hasDungeonKey) award(player, new ResourceLocation("seven_days_to_craft", "wrd/obtain_key"));

            String[] bossAdvancements = {
                "ad_ancient_golem", "ad_ancient_guardian", "ad_necromancer", "ad_undead_mage", "ad_ancient_mummy", "ad_host",
                "ad_all_seer", "ad_king", "ad_doom_golem", "ad_doom_pillar", "ad_ice_mage", "ad_heart_of_ice",
                "ad_king", "ad_heart_of_ender", "ad_super_enforcer", "ad_giant_snake", "ad_aqua_guard", "ad_crystal_spider",
                "ad_giant_mummy", "ad_faceof_the_maw", "ad_the_corpse", "ad_the_beyond", "ad_the_fallen", "ad_thank_you"
            };
            for (String path : bossAdvancements) {
                var advancement = player.server.getAdvancements().getAdvancement(new ResourceLocation("wrd", path));
                if (advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone()) {
                    award(player, new ResourceLocation("seven_days_to_craft", "wrd/defeat_guardian"));
                    break;
                }
            }
        }

        private static void trackOnboardingArmorSet(ServerPlayer player) {
            for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET }) {
                ItemStack stack = player.getItemBySlot(slot);
                if (stack.isEmpty() || (!(stack.getItem() instanceof ArmorItem) && Mob.getEquipmentSlotForItem(stack) != slot)) return;
            }
            award(player, new ResourceLocation("seven_days_to_craft", "onboarding/armor_set"));
        }

        @SubscribeEvent
        public static void onUseRecycler(PlayerInteractEvent.RightClickBlock event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(event.getPos()).getBlock());
            if (!blockId.toString().equals("corail_recycler:recycler")) return;
            if (player.getTags().contains("7dtc_salvager")) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/salvager_use"));
                if (player.getTags().contains("7dtc_master_salvager")) {
                    award(player, new ResourceLocation("seven_days_to_craft", "training/master_salvager_use"));
                }
                return;
            }
            event.setCanceled(true);
            event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.DENY);
            player.displayClientMessage(Component.literal("Salvager is required to use the Recycler.").withStyle(net.minecraft.ChatFormatting.RED), true);
        }

        @SubscribeEvent
        public static void onUseBackpack(PlayerInteractEvent.RightClickItem event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            String itemId = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()).toString();
            if (!itemId.equals("quark:backpack")) return;
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
            String message = player.getTags().contains("7dtc_backpack")
                ? "Equip the Quark Backpack in the Curios Back slot."
                : "Backpack Training is required to equip the Quark Backpack.";
            player.displayClientMessage(Component.literal(message).withStyle(net.minecraft.ChatFormatting.RED), true);
        }
        @SubscribeEvent
        public static void onUseTraversalEquipment(PlayerInteractEvent.RightClickItem event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            String itemId = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()).toString();
            String requirement = traversalRequirement(player, itemId);
            if (requirement == null) {
                awardTraversalUse(player, itemId);
                return;
            }
            event.setCanceled(true);
            player.displayClientMessage(Component.literal(requirement).withStyle(net.minecraft.ChatFormatting.RED), true);
        }        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onLeftClickTraversalBlock(PlayerInteractEvent.LeftClickBlock event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            String itemId = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()).toString();
            String requirement = traversalRequirement(player, itemId);
            if (requirement == null) {
                awardTraversalUse(player, itemId);
                return;
            }
            event.setCanceled(true);
            player.displayClientMessage(Component.literal(requirement).withStyle(net.minecraft.ChatFormatting.RED), true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onLeftClickTraversalEntity(AttackEntityEvent event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            String itemId = BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString();
            String requirement = traversalRequirement(player, itemId);
            if (requirement == null) return;
            event.setCanceled(true);
            player.displayClientMessage(Component.literal(requirement).withStyle(net.minecraft.ChatFormatting.RED), true);
        }

        @SubscribeEvent
        public static void onCurioEquip(CurioEquipEvent event) {
            if (!(event.getSlotContext().entity() instanceof ServerPlayer player) || player.connection == null) return;
            String itemId = BuiltInRegistries.ITEM.getKey(event.getStack().getItem()).toString();
            if (!itemId.equals("quark:backpack")) return;
            if (!player.getTags().contains("7dtc_backpack")) {
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
                player.displayClientMessage(Component.literal("Backpack Training is required to equip the Quark Backpack.").withStyle(net.minecraft.ChatFormatting.RED), true);
                return;
            }
            if (!event.getSlotContext().identifier().equals("back")) {
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
                player.displayClientMessage(Component.literal("The Quark Backpack can only use the Curios Back slot.").withStyle(net.minecraft.ChatFormatting.RED), true);
                return;
            }
            award(player, new ResourceLocation("seven_days_to_craft", "training/backpack_equip"));
        }
        private static void enforceBackpackAccess(ServerPlayer player) {
            if (player.tickCount % 5 != 0) return;
            ItemStack chestStack = player.getItemBySlot(EquipmentSlot.CHEST);
            if (!chestStack.isEmpty() && BuiltInRegistries.ITEM.getKey(chestStack.getItem()).toString().equals("quark:backpack")) {
                ItemStack backpack = chestStack.copy();
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
                if (!player.getInventory().add(backpack)) player.drop(backpack, false);
                player.displayClientMessage(Component.literal("Equip the Quark Backpack in the Curios Back slot.").withStyle(net.minecraft.ChatFormatting.RED), true);
            }
            if (player.getTags().contains("7dtc_backpack")) return;
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                var equipped = handler.findCurios(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("quark:backpack"));
                for (var result : equipped) {
                    ItemStack backpack = result.stack().copy();
                    handler.setEquippedCurio(result.slotContext().identifier(), result.slotContext().index(), ItemStack.EMPTY);
                    if (!player.getInventory().add(backpack)) player.drop(backpack, false);
                    player.displayClientMessage(Component.literal("Backpack Training is required to equip the Quark Backpack.").withStyle(net.minecraft.ChatFormatting.RED), true);
                }
            });
        }
        @SubscribeEvent
        public static void onTrainingBlockInteract(PlayerInteractEvent.RightClickBlock event) {
            if (!(event.getEntity() instanceof ServerPlayer player)) return;
            BlockPos pos = event.getPos().immutable();
            var block = event.getLevel().getBlockState(pos).getBlock();
            if (block instanceof BedBlock) {
                award(player, new ResourceLocation("seven_days_to_craft", "onboarding/any_bed"));
            } else if (block == Blocks.CRAFTING_TABLE) {
                award(player, new ResourceLocation("seven_days_to_craft", "onboarding/crafting_table"));
            } else if (block instanceof AbstractFurnaceBlock) {
                award(player, new ResourceLocation("seven_days_to_craft", "onboarding/furnace"));
            } else if (BuiltInRegistries.BLOCK.getKey(block).equals(new ResourceLocation("cgm", "workbench"))) {
                award(player, new ResourceLocation("seven_days_to_craft", "onboarding/workbench"));
            }

            ResourceLocation before = BuiltInRegistries.BLOCK.getKey(block);
            if (before != null && before.getNamespace().equals("create")) {
                String path = before.getPath();
                if (path.equals("cogwheel")) player.getPersistentData().putBoolean("7dtc_training_cogwheel", true);
                if (path.equals("shaft")) player.getPersistentData().putBoolean("7dtc_training_shaft", true);
                awardBasicMechanicsWhenReady(player);
                if (path.equals("mechanical_press") || path.equals("mechanical_mixer") || path.equals("encased_fan")) {
                    award(player, new ResourceLocation("seven_days_to_craft", "training/advanced_component_obtain_or_use"));
                }
                if (player.getTags().contains("7dtc_mechanics_2") && isCreateProcessingComponent(path)) {
                    award(player, new ResourceLocation("seven_days_to_craft", "training/advanced_mechanics_use"));
                }
                if (player.getTags().contains("7dtc_create_automation") && isCreateAutomationComponent(path)) {
                    award(player, new ResourceLocation("seven_days_to_craft", "training/create_automation_use"));
                }
            }
            if (before != null && before.toString().equals("minecraft:smooth_stone")) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/smooth_stone_obtain_or_use"));
            }
            if (before == null || !before.getNamespace().equals("wrd") || !before.getPath().contains("chest") || before.getPath().contains("open")) return;
            player.server.execute(() -> {
                ResourceLocation after = BuiltInRegistries.BLOCK.getKey(player.level().getBlockState(pos).getBlock());
                if (after == null || !after.getNamespace().equals("wrd")) return;
                boolean opened = after.getPath().contains("open") || !after.equals(before);
                if (!opened) return;
                award(player, new ResourceLocation("seven_days_to_craft", "wrd/open_locked_chest"));
                String path = before.getPath();
                if (path.contains("treasure") || path.contains("bone_chest")) {
                    award(player, new ResourceLocation("seven_days_to_craft", "wrd/claim_treasure"));
                }
            });
        }

        private static void trackPossessionTraining(ServerPlayer player) {
            boolean backpackInInventory = hasItem(player, "quark:backpack");
            final boolean[] backpackInCuriosBack = {false};
            CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                backpackInCuriosBack[0] = handler.findCurios(stack -> isItem(stack, "quark:backpack")).stream()
                    .anyMatch(result -> result.slotContext().identifier().equals("back"))
            );
            if (backpackInInventory || backpackInCuriosBack[0]) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/backpack_obtain"));
            }
            if (backpackInCuriosBack[0]) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/backpack_equip"));
            }
            if (hasItem(player, "grapplemod:grapplinghook")) award(player, new ResourceLocation("seven_days_to_craft", "training/grappler_obtain"));
            if (hasItem(player, "corail_recycler:recycler")) award(player, new ResourceLocation("seven_days_to_craft", "training/recycler_obtain"));
            if (hasItem(player, "grapplemod:launcheritem") || hasItem(player, "grapplemod:enderhook")) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/ender_obtain"));
            }

            // Construction lessons accept an item from any legitimate source. This
            // covers crafting, loot, trading and items handed over by another player.
            if (hasItem(player, "tacz:modern_kinetic_gun")) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/firearm_obtain_or_use"));
            }
            if (hasItem(player, "cgm:basic_bullet")) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/basic_ammunition_obtain_or_use"));
            }
            if (hasAnyItem(player, "cgm:advanced_bullet", "cgm:shell")) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/advanced_ammunition_obtain_or_use"));
            }
            if (hasItem(player, "create:cogwheel")) player.getPersistentData().putBoolean("7dtc_training_cogwheel", true);
            if (hasItem(player, "create:shaft")) player.getPersistentData().putBoolean("7dtc_training_shaft", true);
            awardBasicMechanicsWhenReady(player);
            if (hasAnyItem(player, "create:mechanical_press", "create:mechanical_mixer", "create:encased_fan")) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/advanced_component_obtain_or_use"));
            }
            if (hasItem(player, "minecraft:smooth_stone")) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/smooth_stone_obtain_or_use"));
            }
        }

        private static boolean hasAnyItem(ServerPlayer player, String... ids) {
            for (String id : ids) if (hasItem(player, id)) return true;
            return false;
        }

        private static void awardBasicMechanicsWhenReady(ServerPlayer player) {
            var data = player.getPersistentData();
            if (data.getBoolean("7dtc_training_cogwheel") && data.getBoolean("7dtc_training_shaft")) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/basic_components_obtain_or_use"));
            }
        }

        @SubscribeEvent
        public static void onTrainingBlockBroken(BlockEvent.BreakEvent event) {
            if (!(event.getPlayer() instanceof ServerPlayer player)) return;
            if (player.getTags().contains("7dtc_ore_sense")
                && event.getState().is(Tags.Blocks.ORES)
                && player.level().getBrightness(LightLayer.SKY, event.getPos()) == 0
                && player.level().getBrightness(LightLayer.BLOCK, event.getPos()) <= 6) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/ore_sense_mine"));
            }
            if (player.getTags().contains("7dtc_lumberjack")
                && event.getState().is(net.minecraft.tags.BlockTags.LOGS)
                && player.getMainHandItem().getItem() instanceof AxeItem) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/lumberjack_fell"));
            }
        }

        private static void awardTraversalUse(ServerPlayer player, String itemId) {
            if (itemId.equals("grapplemod:grapplinghook") && player.getTags().contains("7dtc_grappler")) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/grappler_use"));
            }
            if ((itemId.equals("grapplemod:launcheritem") || itemId.equals("grapplemod:enderhook"))
                && player.getTags().contains("7dtc_ender_traversal")) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/ender_use"));
            }
        }

        private static boolean isCreateProcessingComponent(String path) {
            return path.equals("mechanical_press") || path.equals("mechanical_mixer")
                || path.equals("millstone") || path.equals("crushing_wheel")
                || path.equals("encased_fan") || path.equals("depot");
        }

        private static boolean isCreateAutomationComponent(String path) {
            return path.equals("mechanical_arm") || path.equals("deployer")
                || path.equals("chute") || path.equals("smart_chute")
                || path.equals("brass_funnel") || path.equals("brass_tunnel")
                || path.equals("content_observer") || path.equals("stockpile_switch");
        }

        private static void trackLowProfileTraining(ServerPlayer player) {
            if (!player.getTags().contains("7dtc_low_profile") || !player.isCrouching() || player.tickCount % 10 != 0) return;
            boolean unnoticed = !player.serverLevel().getEntitiesOfClass(
                net.minecraft.world.entity.monster.Monster.class,
                player.getBoundingBox().inflate(8.0D),
                mob -> mob.isAlive() && mob.getTarget() != player
            ).isEmpty();
            if (unnoticed) award(player, new ResourceLocation("seven_days_to_craft", "training/low_profile_approach"));
        }

        private static boolean hasItem(ServerPlayer player, String id) {
            for (ItemStack stack : player.getInventory().items) {
                if (isItem(stack, id)) return true;
            }
            for (ItemStack stack : player.getInventory().offhand) {
                if (isItem(stack, id)) return true;
            }
            return false;
        }

        private static boolean isItem(ItemStack stack, String id) {
            return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(id);
        }

        private static void enforceTraversalEquipment(ServerPlayer player) {
            if (player.tickCount % 5 != 0) return;
            for (var stack : java.util.List.of(player.getMainHandItem(), player.getOffhandItem())) {
                if (stack.isEmpty()) continue;
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                if (traversalRequirement(player, itemId) != null) {
                    player.getCooldowns().addCooldown(stack.getItem(), 10);
                }
            }
        }

        private static String traversalRequirement(Player player, String itemId) {
            return traversalRequirement(
                player.getTags().contains("7dtc_grappler"),
                player.getTags().contains("7dtc_ender_traversal"),
                itemId
            );
        }

        private static String traversalRequirement(boolean grappler, boolean enderTraversal, String itemId) {
            if (itemId.equals("grapplemod:launcheritem") || itemId.equals("grapplemod:enderhook")) {
                return enderTraversal
                    ? null : "Ender Traversal is required to use this item.";
            }
            if (itemId.equals("alexsmobs:dimensional_carver")
                || itemId.equals("alexsmobs:straddleboard")
                || itemId.equals("alexsmobs:flying_fish_boots")
                || itemId.equals("alexsmobs:tarantula_hawk_elytra")) {
                return enderTraversal
                    ? null : "Ender Traversal is required to use this item.";
            }
            if (itemId.equals("alexsmobs:squid_grapple") || itemId.equals("alexsmobs:vine_lasso")) {
                return grappler
                    ? null : "Grappler License is required to use this item.";
            }
            if (itemId.startsWith("grapplemod:") && itemId.endsWith("hook")) {
                return grappler
                    ? null : "Grappler License is required to use this item.";
            }
            return null;
        }

        private static void applyRepairEfficiency(ServerPlayer player) {
            var data = player.getPersistentData();
            long until = data.getLong("7dtc_repair_check_until");
            if (until == 0L || player.level().getGameTime() > until) {
                if (until != 0L) data.remove("7dtc_repair_check_until");
                return;
            }
            var target = player.getOffhandItem();
            int before = data.getInt("7dtc_repair_before");
            if (!target.isEmpty() && target.getDamageValue() < before) {
                int repaired = before - target.getDamageValue();
                int extra = Math.max(1, Math.round(repaired * 0.075F * data.getInt("7dtc_repair_rank")));
                target.setDamageValue(Math.max(0, target.getDamageValue() - extra));
                data.remove("7dtc_repair_check_until");
            }
        }

        private static void applyEfficientMetabolism(ServerPlayer player) {
            var data = player.getPersistentData();
            float current = player.getFoodData().getExhaustionLevel();
            int rank = getBasicSkillRank(player, "efficientmetabolism", 4);
            if (rank <= 0) {
                data.putFloat("7dtc_previous_exhaustion", current);
                return;
            }

            if (!data.contains("7dtc_previous_exhaustion")) {
                data.putFloat("7dtc_previous_exhaustion", current);
                return;
            }

            float previous = data.getFloat("7dtc_previous_exhaustion");
            float generated = current - previous;
            if (generated < -0.001F) generated += 4.0F;
            if (generated > 0.0F && generated <= 4.0F) {
                current = Math.max(0.0F, current - (generated * 0.07F * rank));
                player.getFoodData().setExhaustion(current);
            }
            data.putFloat("7dtc_previous_exhaustion", current);
        }
        private static void applyLowHealthPassives(ServerPlayer player) {
            if (!player.isAlive() || player.getMaxHealth() <= 0.0F) return;

            long now = player.level().getGameTime();
            float healthRatio = player.getHealth() / player.getMaxHealth();
            var data = player.getPersistentData();

            if (player.getTags().contains("7dtc_adrenaline_rush")) {
                long activeUntil = data.getLong("7dtc_adrenaline_active_until");
                if (healthRatio <= 0.20F && now >= data.getLong("7dtc_adrenaline_cooldown_until")) {
                    activeUntil = now + 300L;
                    data.putLong("7dtc_adrenaline_active_until", activeUntil);
                    data.putLong("7dtc_adrenaline_cooldown_until", now + 3600L);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0, false, true, true));
                    award(player, new ResourceLocation("seven_days_to_craft", "training/emergency_mobility_threshold"));
                    award(player, new ResourceLocation("seven_days_to_craft", "training/emergency_mobility_trigger"));
                    player.displayClientMessage(Component.literal("Adrenaline Rush activated!"), true);
                }
                if (now < activeUntil) {
                    Stamina stamina = Stamina.get(player);
                    if (stamina != null) stamina.setValue(stamina.getMaxValue());
                    if (player.isSprinting()) award(player, new ResourceLocation("seven_days_to_craft", "training/emergency_mobility_sprint"));
                }
            }

            if (player.getTags().contains("7dtc_pain_tolerance")
                && healthRatio <= 0.20F
                && now >= data.getLong("7dtc_pain_tolerance_cooldown_until")) {
                data.putLong("7dtc_pain_tolerance_cooldown_until", now + 6000L);
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 0, false, true, true));
                award(player, new ResourceLocation("seven_days_to_craft", "training/pain_tolerance_threshold"));
                award(player, new ResourceLocation("seven_days_to_craft", "training/pain_tolerance_trigger"));
                data.putLong("7dtc_pain_tolerance_training_until", now + 1200L);
                player.displayClientMessage(Component.literal("Pain Tolerance activated!"), true);
            }
        }

        private static void triggerEvasiveRush(Player player) {
            if (!player.getTags().contains("7dtc_evasive_rush")) return;
            long now = player.level().getGameTime();
            var data = player.getPersistentData();
            if (now < data.getLong("7dtc_evasive_rush_cooldown_until")) return;

            var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed == null) return;
            speed.removeModifier(EVASIVE_RUSH_SPEED_ID);
            speed.addTransientModifier(new AttributeModifier(EVASIVE_RUSH_SPEED_ID, "7DTC Evasive Rush", 0.25D,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
            data.putLong("7dtc_evasive_rush_active_until", now + 100L);
            data.putLong("7dtc_evasive_rush_cooldown_until", now + 600L);
            if (player instanceof ServerPlayer serverPlayer) {
                award(serverPlayer, new ResourceLocation("seven_days_to_craft", "training/evasive_rush_trigger"));
            }
            player.displayClientMessage(Component.literal("Evasive Rush activated!"), true);
        }

        private static void applyMovementSkills(ServerPlayer player) {
            var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed == null) return;

            boolean silentStep = player.getTags().contains("7dtc_silent_step") && player.isCrouching();
            boolean hasSilentStep = speed.getModifier(SILENT_STEP_SPEED_ID) != null;
            if (silentStep && !hasSilentStep) {
                // Minecraft still applies its crouching input multiplier after this
                // modifier, keeping crouched movement below ordinary walking speed.
                speed.addTransientModifier(new AttributeModifier(SILENT_STEP_SPEED_ID, "7DTC Silent Step", 0.10D,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
            } else if (!silentStep && hasSilentStep) {
                speed.removeModifier(SILENT_STEP_SPEED_ID);
            }

            long now = player.level().getGameTime();
            if (now < player.getPersistentData().getLong("7dtc_evasive_rush_active_until") && player.isSprinting()) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/evasive_rush_sprint"));
            }
            if (now >= player.getPersistentData().getLong("7dtc_evasive_rush_active_until")) {
                speed.removeModifier(EVASIVE_RUSH_SPEED_ID);
            }
        }

        private static void applyPainToleranceTraining(ServerPlayer player) {
            var data = player.getPersistentData();
            long until = data.getLong("7dtc_pain_tolerance_training_until");
            if (until > 0L && player.level().getGameTime() >= until) {
                award(player, new ResourceLocation("seven_days_to_craft", "training/pain_tolerance_observe"));
                data.remove("7dtc_pain_tolerance_training_until");
            }
        }

        private static void applySecondWind(ServerPlayer player) {
            var data = player.getPersistentData();
            int remaining = data.getInt("7dtc_second_wind_heals_remaining");
            if (remaining <= 0) return;
            long now = player.level().getGameTime();
            if (now < data.getLong("7dtc_second_wind_next_heal")) return;
            if (player.isAlive() && player.getHealth() < player.getMaxHealth()) player.heal(0.5F);
            remaining--;
            data.putInt("7dtc_second_wind_heals_remaining", remaining);
            data.putLong("7dtc_second_wind_next_heal", now + 20L);
            if (remaining <= 0) award(player, new ResourceLocation("seven_days_to_craft", "training/second_wind_recover"));
        }

        private static void applyDarkAdaptation(ServerPlayer player) {
            if (!player.getTags().contains("7dtc_dark_adaptation")) return;
            var data = player.getPersistentData();
            if (player.getLightLevelDependentMagicValue() >= 0.30F) {
                data.remove("7dtc_dark_adaptation_ticks");
                return;
            }
            int ticks = data.getInt("7dtc_dark_adaptation_ticks") + 1;
            data.putInt("7dtc_dark_adaptation_ticks", ticks);
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 8, 0, false, false, false));
            award(player, new ResourceLocation("seven_days_to_craft", "training/dark_adaptation_enter"));
            if (ticks >= 40) award(player, new ResourceLocation("seven_days_to_craft", "training/dark_adaptation_wait"));
            if (ticks >= 100) award(player, new ResourceLocation("seven_days_to_craft", "training/dark_adaptation_confirm"));
        }

        private static void applyEnduringVitality(ServerPlayer player) {
            if (!player.getTags().contains("7dtc_enduring_vitality") || !player.isAlive() || player.getHealth() >= player.getMaxHealth()) return;

            long now = player.level().getGameTime();
            var data = player.getPersistentData();
            if (now - data.getLong("7dtc_last_damage_tick") < 400L) return;
            if (now - data.getLong("7dtc_enduring_vitality_last_heal") < 900L) return;

            award(player, new ResourceLocation("seven_days_to_craft", "training/enduring_vitality_wait"));
            player.heal(4.0F);
            award(player, new ResourceLocation("seven_days_to_craft", "training/enduring_vitality_recovery"));
            data.putLong("7dtc_enduring_vitality_last_heal", now);
        }
        private static void syncPoints(ServerPlayer player, boolean force) {
            var category = SkillsAPI.getCategory(SURVIVOR);
            int points = category.map(value -> value.getPointsLeft(player)).orElse(0);
            if (category.map(value -> value.getSpentPoints(player) > 0).orElse(false)) {
                award(player, new ResourceLocation("seven_days_to_craft", "onboarding/first_skill"));
            }
            int oreSenseRange = player.getTags().contains("7dtc_ore_sense_2")
                && player.getTags().contains("7dtc_ore_sense") ? Integer.MAX_VALUE
                : player.getTags().contains("7dtc_ore_sense") ? 20 : 0;
            boolean grappler = player.getTags().contains("7dtc_grappler");
            boolean enderTraversal = player.getTags().contains("7dtc_ender_traversal");
            boolean threatSense = player.getTags().contains("7dtc_threat_sense");
            boolean dayTime = hasUnlockedDefinition(player, "survivor_root");
            int previous = player.getPersistentData().getInt("7dtc_last_skill_points");
            int previousOreSenseRange = player.getPersistentData().getInt("7dtc_last_ore_sense_range");
            boolean previousGrappler = player.getPersistentData().getBoolean("7dtc_last_grappler");
            boolean previousEnderTraversal = player.getPersistentData().getBoolean("7dtc_last_ender_traversal");
            boolean previousThreatSense = player.getPersistentData().getBoolean("7dtc_last_threat_sense");
            boolean previousDayTime = player.getPersistentData().getBoolean("7dtc_last_day_time");
            if (force || points != previous || oreSenseRange != previousOreSenseRange
                || grappler != previousGrappler || enderTraversal != previousEnderTraversal
                || threatSense != previousThreatSense || dayTime != previousDayTime) {
                player.getPersistentData().putInt("7dtc_last_skill_points", points);
                player.getPersistentData().putInt("7dtc_last_ore_sense_range", oreSenseRange);
                player.getPersistentData().putBoolean("7dtc_last_grappler", grappler);
                player.getPersistentData().putBoolean("7dtc_last_ender_traversal", enderTraversal);
                player.getPersistentData().putBoolean("7dtc_last_threat_sense", threatSense);
                player.getPersistentData().putBoolean("7dtc_last_day_time", dayTime);
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SkillPointsPacket(points, oreSenseRange, grappler, enderTraversal, threatSense, dayTime));
            }
        }

        private static void award(ServerPlayer player, ResourceLocation id) {
            var advancement = player.server.getAdvancements().getAdvancement(id);
            if (advancement != null) {
                var progress = player.getAdvancements().getOrStartProgress(advancement);
                for (String criterion : progress.getRemainingCriteria()) player.getAdvancements().award(advancement, criterion);
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static void apply(ServerPlayer player) {
            Set<String> tags = player.getTags();
            boolean basic = tags.contains("7dtc_parcool_basic");
            boolean advanced = tags.contains("7dtc_parcool_wall_grip");
            int mask = (basic ? 1 : 0) | (advanced ? 2 : 0);
            if (player.getPersistentData().getInt("7dtc_parcool_mask") == mask && player.tickCount > 40) return;

            Limitation limitation = Limitation.getIndividual(player);
            // ParCool may not have attached a remote LAN player's individual
            // limitation capability during the login event yet. The regular
            // player tick retries this method once initialization completes.
            if (limitation == null) return;
            limitation.setDefault();
            limitation.enable();
            for (Class action : ALL) limitation.permit(action, false);
            if (basic) for (Class action : BASIC) limitation.permit(action, true);
            if (advanced) {
                for (Class action : BASIC) limitation.permit(action, true);
                for (Class action : ADVANCED) limitation.permit(action, true);
            }
            limitation.apply();
            player.getPersistentData().putInt("7dtc_parcool_mask", mask);
        }
    }

    public record SkillPointsPacket(int points, int oreSenseRange, boolean grappler, boolean enderTraversal, boolean threatSense, boolean dayTime) {
        static void encode(SkillPointsPacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.points);
            buffer.writeVarInt(packet.oreSenseRange);
            buffer.writeBoolean(packet.grappler);
            buffer.writeBoolean(packet.enderTraversal);
            buffer.writeBoolean(packet.threatSense);
            buffer.writeBoolean(packet.dayTime);
        }
        static SkillPointsPacket decode(FriendlyByteBuf buffer) {
            return new SkillPointsPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
        }
        static void handle(SkillPointsPacket packet, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> {
                ClientOverlay.points = packet.points;
                ClientOverlay.grappler = packet.grappler;
                ClientOverlay.enderTraversal = packet.enderTraversal;
                ClientOverlay.threatSense = packet.threatSense;
                ClientOverlay.dayTime = packet.dayTime;
                OreSenseOverlay.setRange(packet.oreSenseRange);
            });
            context.get().setPacketHandled(true);
        }
    }

    public record SkillUnlockNoticePacket(String title, String message) {
        static void encode(SkillUnlockNoticePacket packet, FriendlyByteBuf buffer) {
            buffer.writeUtf(packet.title, 96);
            buffer.writeUtf(packet.message, 512);
        }
        static SkillUnlockNoticePacket decode(FriendlyByteBuf buffer) {
            return new SkillUnlockNoticePacket(buffer.readUtf(96), buffer.readUtf(512));
        }
        static void handle(SkillUnlockNoticePacket packet, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> ClientOverlay.enqueueNotice(
                "ABILITY UNLOCKED: " + packet.title, packet.message, 5200L, 0xFFFF55));
            context.get().setPacketHandled(true);
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ClientOverlay {
        private static int points;
        private static boolean grappler;
        private static boolean enderTraversal;
        private static boolean threatSense;
        private static boolean dayTime;
        private record Notice(String title, String message, long duration, int accent, boolean compact) {}
        private static final Deque<Notice> NOTICE_QUEUE = new ArrayDeque<>();
        private static final Map<ResourceLocation, Integer> LAST_EFFECTS = new HashMap<>();
        private static Notice activeNotice;
        private static long noticeStarted;
        private static long nextNoticeAt;
        private static int trackedPlayerEntityId = Integer.MIN_VALUE;
        private static boolean effectsInitialized;

        static void enqueueNotice(String title, String message, long duration, int accent) {
            NOTICE_QUEUE.addLast(new Notice(title, message, duration, accent, false));
        }

        public static boolean hasThreatSense() {
            return threatSense;
        }

        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            var minecraft = Minecraft.getInstance();
            var player = minecraft.player;
            if (player == null || minecraft.level == null) {
                LAST_EFFECTS.clear();
                effectsInitialized = false;
                trackedPlayerEntityId = Integer.MIN_VALUE;
                return;
            }
            if (trackedPlayerEntityId != player.getId()) {
                LAST_EFFECTS.clear();
                effectsInitialized = false;
                trackedPlayerEntityId = player.getId();
            }
            Map<ResourceLocation, Integer> current = new HashMap<>();
            for (MobEffectInstance effect : player.getActiveEffects()) {
                ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect());
                if (id == null) continue;
                current.put(id, effect.getAmplifier());
                Integer previous = LAST_EFFECTS.get(id);
                if (effectsInitialized && (previous == null || effect.getAmplifier() > previous)) {
                    String kind = effect.getEffect().getCategory() == MobEffectCategory.HARMFUL ? "DEBUFF" : "BUFF";
                    int accent = effect.getEffect().getCategory() == MobEffectCategory.HARMFUL ? 0xFFFF5555 : 0xFF55FF55;
                    String description = describeEffect(id, effect);
                    // Unknown mod effects already have the normal status icon. Do not
                    // invent a misleading explanation for mechanics we cannot verify.
                    if (description != null) {
                        NOTICE_QUEUE.addLast(new Notice(kind + ": " + effect.getEffect().getDisplayName().getString(), description, 2800L, accent, true));
                    }
                }
            }
            LAST_EFFECTS.clear();
            LAST_EFFECTS.putAll(current);
            effectsInitialized = true;
        }

        private static String describeEffect(ResourceLocation id, MobEffectInstance effect) {
            int level = effect.getAmplifier() + 1;
            int seconds = Math.max(0, (effect.getDuration() + 19) / 20);
            String duration = effect.isInfiniteDuration() ? "Permanent" : "Duration: " + seconds + " seconds";
            String key = id.toString();
            String impact;
            if (key.equals("alexsmobs:earthquake")) impact = "The Rocky Roller's tremor shakes your view.";
            else if (key.equals("alexsmobs:exsanguination")) impact = "Bleeding damages you over time.";
            else if (key.equals("alexsmobs:ender_flu")) impact = "Ender infection can hurt you and release Enderiophages.";
            else if (key.equals("hordes:infected")) impact = "Infection drains hunger; later stages can slow you.";
            else if (effect.getEffect() == MobEffects.DAMAGE_RESISTANCE) impact = "You take " + Math.min(80, level * 20) + "% less damage.";
            else if (effect.getEffect() == MobEffects.MOVEMENT_SPEED) impact = "Your movement speed is increased by " + (level * 20) + "%.";
            else if (effect.getEffect() == MobEffects.MOVEMENT_SLOWDOWN) impact = "Your movement speed is reduced by " + (level * 15) + "%.";
            else if (effect.getEffect() == MobEffects.DAMAGE_BOOST) impact = "Your melee attacks deal " + (level * 3) + " extra damage.";
            else if (effect.getEffect() == MobEffects.WEAKNESS) impact = "Your melee attacks deal less damage.";
            else if (effect.getEffect() == MobEffects.DIG_SPEED) impact = "Mining speed is increased.";
            else if (effect.getEffect() == MobEffects.DIG_SLOWDOWN) impact = "Mining speed is reduced.";
            else if (effect.getEffect() == MobEffects.HUNGER) impact = "Your hunger drains faster.";
            else if (effect.getEffect() == MobEffects.REGENERATION) impact = "You regenerate health over time.";
            else if (effect.getEffect() == MobEffects.POISON) impact = "You take periodic poison damage, but it cannot kill you.";
            else if (effect.getEffect() == MobEffects.WITHER) impact = "You take periodic wither damage that can kill you.";
            else if (effect.getEffect() == MobEffects.FIRE_RESISTANCE) impact = "Fire and lava cannot damage you.";
            else if (effect.getEffect() == MobEffects.WATER_BREATHING) impact = "You can breathe underwater.";
            else if (effect.getEffect() == MobEffects.NIGHT_VISION) impact = "Dark areas are easier to see.";
            else if (effect.getEffect() == MobEffects.INVISIBILITY) impact = "Most creatures have more difficulty seeing you.";
            else if (effect.getEffect() == MobEffects.ABSORPTION) impact = "You have temporary absorption hearts.";
            else if (effect.getEffect() == MobEffects.HEALTH_BOOST) impact = "Your maximum health is temporarily increased.";
            else return null;
            return impact + " " + duration + (level > 1 ? " (Level " + level + ")" : "");
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void blockRestrictedTraversalAttack(InputEvent.InteractionKeyMappingTriggered event) {
            if (!event.isAttack()) return;
            var player = Minecraft.getInstance().player;
            if (player == null) return;
            ItemStack stack = player.getItemInHand(event.getHand());
            if (stack.isEmpty()) return;
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            String requirement = Events.traversalRequirement(grappler, enderTraversal, itemId);
            if (requirement == null) return;
            event.setSwingHand(false);
            event.setCanceled(true);
            player.displayClientMessage(Component.literal(requirement).withStyle(net.minecraft.ChatFormatting.RED), true);
        }
        @SubscribeEvent
        public static void render(RenderGuiEvent.Post event) {
            var minecraft = Minecraft.getInstance();
            if (minecraft.options.hideGui) return;
            int screenWidth = event.getWindow().getGuiScaledWidth();
            if (dayTime && minecraft.level != null) {
                long ticks = Math.floorMod(minecraft.level.getDayTime(), 24000L);
                int minutes = (int) (((ticks + 6000L) % 24000L) * 60L / 1000L);
                // Minecraft time 0 is 06:00. Shift the calendar by six hours
                // so the displayed day changes at 00:00 with the clock.
                long day = Math.floorDiv(minecraft.level.getDayTime() + 6000L, 24000L) + 1L;
                Component clock = Component.literal(String.format("Day %d  %02d:%02d", day, minutes / 60, minutes % 60));
                int width = minecraft.font.width(clock);
                int x = screenWidth - width - 8;
                event.getGuiGraphics().fill(x - 4, 5, screenWidth - 4, 18, 0x99000000);
                event.getGuiGraphics().drawString(minecraft.font, clock, x, 8, 0xFFFFFFFF, true);
            }

            renderNotice(event);

            if (points > 0) {
                Component text = Component.literal("Skill Points: " + points + " (Press K)");
                int x = 8;
                int y = event.getWindow().getGuiScaledHeight() - 64;
                int width = minecraft.font.width(text);
                event.getGuiGraphics().fill(x - 3, y - 3, x + width + 3, y + 11, 0x99000000);
                event.getGuiGraphics().drawString(minecraft.font, text, x, y, 0xFFFF55, true);
            }
        }

        private static void renderNotice(RenderGuiEvent.Post event) {
            long now = System.currentTimeMillis();
            if (activeNotice != null && now - noticeStarted >= activeNotice.duration()) {
                activeNotice = null;
                nextNoticeAt = now + 250L;
            }
            if (activeNotice == null && now >= nextNoticeAt && !NOTICE_QUEUE.isEmpty()) {
                activeNotice = NOTICE_QUEUE.removeFirst();
                noticeStarted = now;
            }
            if (activeNotice == null) return;
            var minecraft = Minecraft.getInstance();
            if (activeNotice.compact()) {
                String message = activeNotice.title() + ": " + activeNotice.message();
                int maxWidth = Math.min(350, event.getWindow().getGuiScaledWidth() - 24);
                List<net.minecraft.util.FormattedCharSequence> lines = minecraft.font.split(Component.literal(message), maxWidth);
                int textWidth = 0;
                for (var line : lines) textWidth = Math.max(textWidth, minecraft.font.width(line));
                int left = (event.getWindow().getGuiScaledWidth() - textWidth) / 2;
                int top = event.getWindow().getGuiScaledHeight() - 82 - (lines.size() - 1) * 10;
                event.getGuiGraphics().fill(left - 4, top - 2, left + textWidth + 4, top + lines.size() * 10 + 1, 0x99000000);
                int y = top;
                for (var line : lines) {
                    event.getGuiGraphics().drawString(minecraft.font, line, left, y, activeNotice.accent(), true);
                    y += 10;
                }
                return;
            }
            int maxTextWidth = Math.min(360, event.getWindow().getGuiScaledWidth() - 40);
            List<net.minecraft.util.FormattedCharSequence> lines = minecraft.font.split(Component.literal(activeNotice.message()), maxTextWidth - 20);
            int contentWidth = minecraft.font.width(activeNotice.title());
            for (var line : lines) contentWidth = Math.max(contentWidth, minecraft.font.width(line));
            int panelWidth = Math.min(maxTextWidth, contentWidth + 20);
            int panelHeight = 20 + lines.size() * 10;
            int left = (event.getWindow().getGuiScaledWidth() - panelWidth) / 2;
            int top = 52;
            event.getGuiGraphics().fill(left, top, left + panelWidth, top + panelHeight, 0xDD101010);
            event.getGuiGraphics().fill(left, top, left + 3, top + panelHeight, activeNotice.accent());
            event.getGuiGraphics().drawCenteredString(minecraft.font, activeNotice.title(), left + panelWidth / 2, top + 5, activeNotice.accent());
            int y = top + 16;
            for (var line : lines) {
                event.getGuiGraphics().drawString(minecraft.font, line, left + 10, y, 0xFFFFFFFF, false);
                y += 10;
            }
        }
    }
}
