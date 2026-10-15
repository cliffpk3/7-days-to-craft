package dev.sevendaystocraft.locks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/** Keeps artifacts exclusive to the keyed chest reward path. */
public final class ArtifactLootFilterModifier extends LootModifier {
    public static final Codec<ArtifactLootFilterModifier> CODEC = RecordCodecBuilder.create(instance ->
        codecStart(instance).apply(instance, ArtifactLootFilterModifier::new));

    public ArtifactLootFilterModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
                                                            LootContext context) {
        generatedLoot.removeIf(stack -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id == null) return false;
            String namespace = id.getNamespace();
            return namespace.equals("artifacts")
                || namespace.equals("nameless_trinkets")
                || namespace.equals("celestial_artifacts");
        });
        return generatedLoot;
    }

    @Override
    public Codec<? extends LootModifier> codec() {
        return CODEC;
    }
}
