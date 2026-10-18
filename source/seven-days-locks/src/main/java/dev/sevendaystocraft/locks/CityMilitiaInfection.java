package dev.sevendaystocraft.locks;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.nekoyuni.SimpleEnemyMod.entity.unit.USunitEntity;
import com.tacz.guns.api.item.IGun;
import net.smileycorp.hordes.infection.HordesInfection;

/** Reanimate city militiamen as ordinary equipped zombies. */
@Mod.EventBusSubscriber(modid = SevenDaysLocks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CityMilitiaInfection {
    private static final String CONVERTED_TAG = "7dtc_reanimated_militia";
    private static final int MAX_REANIMATED_NEARBY = 4;
    private static final int MAX_HOSTILES_NEARBY = 30;

    private CityMilitiaInfection() {}

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof USunitEntity unit)
            || !(unit.level() instanceof ServerLevel level)
            || !(event.getSource().getEntity() instanceof Mob killer)
            || !(killer instanceof Enemy)) return;

        // Forge posts LivingDeathEvent before the native death sound. Silence only
        // monster kills, then play one of the seven supplied scream variations.
        unit.setSilent(true);
        level.playSound(null, unit.blockPosition(), SevenDaysLocks.MILITIA_INFECTED_DEATH.get(),
            SoundSource.HOSTILE, 1.25F, 1.0F);

        if (unit.hasEffect(HordesInfection.INFECTED.get())
            || CityRaid.cityStart(level, unit.blockPosition()) == null
            || !CityAmbientEncounter.isCityZombie(killer)) return;

        if (level.getEntitiesOfClass(Mob.class, unit.getBoundingBox().inflate(32, 12, 32),
            mob -> mob.getTags().contains(CONVERTED_TAG)).size() >= MAX_REANIMATED_NEARBY) return;
        if (level.getEntitiesOfClass(Mob.class, unit.getBoundingBox().inflate(32, 12, 32),
            CityAmbientEncounter::isCityZombie).size() >= MAX_HOSTILES_NEARBY) return;

        Zombie zombie = new Zombie(level);
        zombie.moveTo(unit.getX(), unit.getY(), unit.getZ(), unit.getYRot(), unit.getXRot());
        zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(unit.blockPosition()),
            MobSpawnType.CONVERSION, null, null);
        zombie.addTag("7dtc_city_raid");
        zombie.addTag(CONVERTED_TAG);
        equipFromMilitia(unit, zombie);
        level.addFreshEntity(zombie);
    }

    @SubscribeEvent
    public static void onInfectionConversion(LivingConversionEvent.Post event) {
        if (!(event.getEntity() instanceof USunitEntity unit)
            || !(event.getOutcome() instanceof Zombie zombie)) return;
        if (unit.level() instanceof ServerLevel level
            && CityRaid.cityStart(level, unit.blockPosition()) != null) {
            zombie.addTag("7dtc_city_raid");
        }
        zombie.addTag(CONVERTED_TAG);
        equipFromMilitia(unit, zombie);
    }

    private static void equipFromMilitia(USunitEntity unit, Zombie zombie) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack original = unit.getItemBySlot(slot);
            if (!original.isEmpty()) {
                ItemStack copy = original.copy();
                if (slot == EquipmentSlot.MAINHAND) {
                    IGun gun = IGun.getIGunOrNull(copy);
                    if (gun != null) {
                        gun.setCurrentAmmoCount(copy, 0);
                        gun.setDummyAmmoAmount(copy, 0);
                    }
                }
                zombie.setItemSlot(slot, copy);
            } else if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                ItemStack uniform = uniformPiece(slot);
                if (!uniform.isEmpty()) zombie.setItemSlot(slot, uniform);
            }
            zombie.setDropChance(slot, 0.0F);
        }
    }

    private static ItemStack uniformPiece(EquipmentSlot slot) {
        ItemStack stack = switch (slot) {
            case HEAD -> new ItemStack(Items.LEATHER_HELMET);
            case CHEST -> new ItemStack(Items.LEATHER_CHESTPLATE);
            case LEGS -> new ItemStack(Items.LEATHER_LEGGINGS);
            case FEET -> new ItemStack(Items.LEATHER_BOOTS);
            default -> ItemStack.EMPTY;
        };
        if (!stack.isEmpty()) stack.getOrCreateTagElement("display").putInt("color", 0x59624B);
        return stack;
    }
}
