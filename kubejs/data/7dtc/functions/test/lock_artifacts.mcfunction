# 7DTC teste (10/11/2026): artefatos dos baus trancados. Coloca um bau de cada tier a leste (1 a 4) e da 16 chaves de cada.
# Abra com a chave do tier; quebre e recoloque rodando a funcao de novo para sortear outra vez.
give @s wrd:keystrongholdchest_iron 16
give @s wrd:keystrongholdchest_gold 16
give @s wrd:keystrongholdchest_diamond 16
give @s wrd:keystrongholdchest_treasure 16
setblock ~2 ~ ~ quark:ancient_chest{LootTable:"minecraft:chests/simple_dungeon"} replace
setblock ~2 ~ ~2 quark:acacia_chest{LootTable:"minecraft:chests/simple_dungeon"} replace
setblock ~2 ~ ~4 quark:prismarine_chest{LootTable:"minecraft:chests/simple_dungeon"} replace
setblock ~2 ~ ~6 quark:nether_brick_chest{LootTable:"minecraft:chests/simple_dungeon"} replace
tellraw @s {"text":"[7DTC] Baus trancados tier 1 a 4 colocados a leste (z+0, +2, +4, +6).","color":"yellow"}
