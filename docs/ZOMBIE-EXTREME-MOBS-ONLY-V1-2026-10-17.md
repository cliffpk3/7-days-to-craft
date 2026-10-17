# Zombie Extreme — somente mobs v1

O JAR distribuído do Zombie Extreme 0.2.6.5 foi ajustado com o patch reproduzível em `tools/zombie-extreme-mobs-only/PatchZombieExtreme.java`.

- A rotina que insere `zombie_extreme:scorched_earth` no gerador de biomas foi neutralizada.
- A rotina de radiação aplicada por estar nesse bioma foi neutralizada.
- Os dez `worldgen/structure_set` nativos foram removidos do JAR; suas estruturas não recebem posições de geração natural.
- Permanecem os 42 modificadores de bioma substituídos por `forge:none`, impedindo minérios e spawns globais próprios do mod.
- As receitas próprias continuam removidas por KubeJS. O sistema City Raid do 7DTC controla spawn e drops dos mobs nas cidades.
- A definição do bioma permanece registrada para que chunks antigos com esse identificador continuem abrindo. O bioma deixa de ser criado em chunks novos; chunks já gerados não mudam retroativamente.

O JAR original está em `backups/zombie-extreme-mobs-only-v1-20261017/` no computador local. A tag Git anterior a este patch permite rollback da versão distribuída.
