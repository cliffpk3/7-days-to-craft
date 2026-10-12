# Integração do Born in Chaos

Atualizado em 12/10/2026.

## Estruturas

As 14 famílias de estruturas continuam disponíveis, com espaçamento aumentado.
Como a densidade de uma estrutura de `random_spread` cai aproximadamente com o
quadrado do espaçamento, os novos valores deixam os encontros de duas a cinco
vezes menos frequentes, conforme a estrutura.

| Estrutura | Espaçamento original | Espaçamento 7DTC | Papel |
|---|---:|---:|---|
| Clown Caravan (plains/taiga) | 40 | 80 | Encontro raro, recompensa nível 2 |
| Clown Caravan (savanna) | 50 | 96 | Encontro raro, recompensa nível 2 |
| Dark Tower (forest/taiga) | 40–45 | 96 | Dungeon principal com progressão 1 → 2 → 3 |
| Dark Tower (plains) | 48 | 104 | Dungeon principal com progressão 1 → 2 → 3 |
| Farm | 35 | 72 | Estrutura menor, recompensa nível 1 |
| Firewell | 33 | 72 | Estrutura incomum, recompensa nível 2 |
| Dark Titan Grave | 55 | 112 | Marco muito raro |
| Petasi Grave | 50 | 104 | Marco muito raro |
| Infernal Pumpkin | 23 | 56 | Encontro incomum |
| Mound of Hounds | 26 | 64 | Encontro incomum |
| Observation Tower (forest) | 30 | 72 | Exploração, níveis 1 |
| Observation Tower (plains) | 35 | 80 | Exploração, níveis 1 |

Os arquivos executáveis ficam em
`kubejs/data/born_in_chaos_v1/worldgen/structure_set/`.

## Loot e chaves

O nível é decidido pela tabela gravada no próprio baú da estrutura:

| Loot table | Nível 7DTC | Baú | Chave |
|---|---:|---|---|
| `minecraft:chests/basic_chest` | 1 | Ashen Chest | Iron Level 1 Key |
| `minecraft:chests/farm_drop` | 1 | Ashen Chest | Iron Level 1 Key |
| `minecraft:chests/chest_level_1` | 1 | Ashen Chest | Iron Level 1 Key |
| `minecraft:chests/firewell_d` | 2 | Acacia Chest | Golden Level 2 Key |
| `minecraft:chests/shater` | 2 | Acacia Chest | Golden Level 2 Key |
| `minecraft:chests/chest_level_2` | 2 | Acacia Chest | Golden Level 2 Key |
| `minecraft:chests/chest_level_3` | 3 | Prismarine Chest | Diamond Level 3 Key |

A Dark Tower contém dois baús básicos, três de nível 1, dois de nível 2 e dois de
nível 3. A progressão original foi preservada. As tabelas de nível 3 incluem as
armas raras do próprio Born in Chaos; por isso não foram promovidas para nível 4.

## Monstros, profundidade e drops

A classificação está em `kubejs/config/7dtc_mob_registry.json`.

- Nível 1: mortos vivos e criaturas comuns; spawn natural reduzido para 75%.
- Nível 2: combatentes especiais; limitados principalmente entre Y -40 e Y 32 e
  spawn natural reduzido para 60%.
- Nível 3: elites; limitados principalmente entre Y -64 e Y -8 e spawn natural
  reduzido para 40%.
- Nível 4: Lifestealer e Missionary; limitados entre Y -64 e Y -40 e spawn
  natural reduzido para 18%.
- Chefes de estrutura e formas invocadas recebem classificação, mas não ganham
  uma nova injeção artificial de spawn.

Todos os hostis passam pelas regras existentes de armas Divine Weaponry,
equipamentos encontrados e chaves. A chance da chave usa o nível de ameaça:
nível 2 gera chave 2, nível 3 gera chave 3 e nível 4 gera chave 4. Infernal Mobs
continua tendo prioridade e gera chave nível 3 com 10% de chance.

## Aparência dos zumbis

`TZP_1.20.1_2.7` é carregado automaticamente pelo Paxi. O pacote contém 3.580
texturas de zumbis e derivados e usa Entity Texture Features. Ele altera somente
a aparência; vida, dano, velocidade e IA permanecem inalterados.
