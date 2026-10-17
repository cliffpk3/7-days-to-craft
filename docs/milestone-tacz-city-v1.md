# 7DTC milestone: cidade e armas TaCZ v1

## Escopo

- Carros do The Lost City desativados em novos chunks pelo perfil `7dtc_city`.
- Delegacia da cidade ocupada somente por `simpleenemymod:usunit`; dois spawners no terraço ativam a até 88 blocos. Eventos aleatórios de Simple Enemy são bloqueados, para não misturar facções no prédio.
- 18 armas TaCZ e 11 acessórios selecionados para a CGM Workbench. Armas exigem blueprint específica; a mesa TaCZ não oferece suas receitas padrão.
- As armas selecionadas recarregam com `cgm:basic_bullet`, `cgm:advanced_bullet` ou `cgm:shell`, conforme a classe definida em `TaczCgmAmmoMixin`.
- Loot da milícia e dos baús de cidade segue a distribuição rara de armas, projetos e acessórios.

## Rollback

O estado anterior à migração das armas está na tag `milestone/pre-tacz-migration-v1-20261017`.
Os três JARs substituídos também estão em `backups/tacz-migration-v1-20261017/`.
O gerador de estruturas é `tools/generate_city_spawners.py`; o catálogo da Workbench é `tools/generate_tacz_workbench.py`.

## Validação

- JSON de receitas, loot e configuração analisados sem erros de sintaxe.
- JARs de `seven-days-locks` e `seven-days-skill-bridge` compilados; extensão da Workbench compilada e classe atualizada no JAR.
- NBT da delegacia: dois spawners no terraço sobre piso sólido e somente `usunit` no pool da milícia.
- Ainda requer teste dentro do jogo: carregamento, craft com consumo de blueprint, recarga/descarregamento dos três tipos de munição, abas da Workbench, linha de visão e taxa real de spawn no terraço.
- Estruturas já geradas em saves existentes não são reconstruídas automaticamente.
