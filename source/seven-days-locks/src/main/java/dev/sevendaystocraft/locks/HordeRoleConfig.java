package dev.sevendaystocraft.locks;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** Small gameplay knobs for the custom horde roles. */
final class HordeRoleConfig {
    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.DoubleValue FISHER_CHANCE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("7DTC horde and city role settings. Applies to newly spawned mobs.").push("horde");
        FISHER_CHANCE = builder.comment("Fraction of eligible horde and city zombies assigned the Grappler role. In hordes, replaces Assault, not special roles.")
            .defineInRange("fisherChance", 0.05D, 0.0D, 0.30D);
        builder.pop();
        SPEC = builder.build();
    }

    private HordeRoleConfig() {}

    static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "seven-days-horde-roles.toml");
    }

    static double fisherChance() {
        return FISHER_CHANCE.get();
    }
}
