package dev.sevendaystocraft.locks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Manual audit of loaded hostile entities; no entity is inserted into the world. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MobAudit {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private MobAudit() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("7dtc_mob_audit")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                ServerLevel level = context.getSource().getLevel();
                JsonObject root = new JsonObject();
                root.addProperty("schemaVersion", 1);
                JsonObject mobs = new JsonObject();
                JsonArray failures = new JsonArray();
                ForgeRegistries.ENTITY_TYPES.getEntries().stream()
                    .sorted(Comparator.comparing(entry -> entry.getKey().location().toString()))
                    .forEach(entry -> inspect(level, entry.getKey().location().toString(), entry.getValue(), mobs, failures));
                root.add("mobs", mobs);
                root.add("failures", failures);
                try {
                    Path file = FMLPaths.GAMEDIR.get().resolve("staging/mob-audit-runtime.json");
                    Files.createDirectories(file.getParent());
                    Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
                    context.getSource().sendSuccess(() -> Component.literal("Audited " + mobs.size()
                        + " hostile mobs; " + failures.size() + " failures. See staging/mob-audit-runtime.json"), false);
                    return Command.SINGLE_SUCCESS;
                } catch (Exception error) {
                    context.getSource().sendFailure(Component.literal("Mob audit failed: " + error.getMessage()));
                    return 0;
                }
            }));
    }

    private static void inspect(ServerLevel level, String id, EntityType<?> type, JsonObject mobs, JsonArray failures) {
        try {
            Entity entity = type.create(level);
            if (!(entity instanceof Mob mob) || !(mob instanceof Enemy)) return;
            JsonObject row = new JsonObject();
            row.addProperty("name", mob.getType().getDescription().getString());
            row.addProperty("class", mob.getClass().getName());
            row.addProperty("maxHealth", mob.getMaxHealth());
            attribute(row, "attackDamage", mob, Attributes.ATTACK_DAMAGE);
            attribute(row, "armor", mob, Attributes.ARMOR);
            attribute(row, "armorToughness", mob, Attributes.ARMOR_TOUGHNESS);
            attribute(row, "movementSpeed", mob, Attributes.MOVEMENT_SPEED);
            attribute(row, "followRange", mob, Attributes.FOLLOW_RANGE);
            attribute(row, "knockbackResistance", mob, Attributes.KNOCKBACK_RESISTANCE);
            row.addProperty("width", mob.getBbWidth());
            row.addProperty("height", mob.getBbHeight());
            JsonArray goals = new JsonArray();
            mob.goalSelector.getAvailableGoals().forEach(goal -> goals.add(goal.getGoal().getClass().getName()));
            row.add("goalClasses", goals);
            mobs.add(id, row);
            mob.discard();
        } catch (RuntimeException | LinkageError error) {
            failures.add(id + ": " + error.getClass().getSimpleName() + ": " + error.getMessage());
        }
    }

    private static void attribute(JsonObject row, String name, Mob mob, Attribute attribute) {
        var value = mob.getAttribute(attribute);
        if (value != null) row.addProperty(name, value.getBaseValue());
    }
}
