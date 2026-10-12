# Status do roadmap de gameplay — 12/10/2026

## Resultado desta rodada

| Item | Estado | Resultado |
|---|---|---|
| Prospector, Forager e Scavenger | **Concluído** | Testes iniciais aprovados pelo usuário; efeitos e avisos permanecem ativos. |
| 1 — Loot nativo do Infernal Mobs | **Concluído: mantido** | A raridade e a quantidade de prêmios são compatíveis com o risco; detalhes abaixo. |
| 2 — Divine Weaponry | **Implementado** | Loot exclusivo por ameaça, uma arma Divine no máximo, desgaste variável e receitas removidas. |
| 3 — Classificação de monstros | **Concluído estaticamente** | 224 registros revisados; fauna passiva/neutra permanece intencionalmente no nível 0. |
| 4 — EnhancedAI | **Implementado** | Módulo geral de mobs e Anti-Cheese desativados; equipamentos/funções de hordas continuam no módulo próprio. |
| 5 — Profundidade e risco | **Implementado e corrigido para teste** | Matriz central com 14 encontros, proteção de biomas especiais e substituição segura dos comuns. |
| 6 — Equipamento como loot | **Implementado para teste** | Armaduras por tier e armas de fogo somente em bosses/Infernal Mobs, sempre com baixa chance. |
| 7 — XP | **Implementado para teste** | Primeiro ponto passa de 18 para 24 XP; curva posterior mais íngreme e teto de 480 XP. |
| 8 — Fuel Efficiency / Treasure Sense | **Pendente por decisão** | Sem alteração nesta rodada. |
| 9 — componente raro de torretas | **Pendente por decisão** | Sem alteração nesta rodada. |
| 10 — Core da campanha | **Pendente por decisão** | Sem alteração nesta rodada. |

## 1. Infernal Mobs

O loot nativo foi mantido. O código do mod confirma:

- elite: rolagem de 1 em 50 entre criaturas elegíveis, aproximadamente 2%;
- ultra: nova rolagem de 1 em 40 depois de ser elite, aproximadamente 0,05% do total;
- infernal: nova rolagem de 1 em 40 depois de ser ultra, aproximadamente 0,00125% do total;
- um prêmio a cada cinco modificadores do monstro;
- 25 XP vanilla adicionais.

A lista elite contém principalmente equipamento de ferro/chain e cookies. A lista ultra pode fornecer ouro, golden apples, blaze powder e livro encantado. A lista infernal contém equipamento de diamante, três diamantes ou três ender pearls. Pelo encadeamento de raridade, os resultados mais fortes não aparecem em volume capaz de quebrar a progressão. Um Infernal Mob pode entregar seu prêmio nativo e também vencer a rolagem Divine, o que é aceito como recompensa de um encontro excepcional.

## 2 e 6. Equipamento e Divine Weaponry

- Todas as receitas nativas, smithing e CGM Workbench de Divine Weaponry foram removidas.
- Ameaça 1: 8% para o pool de pedra e 1% para o pool de ferro.
- Ameaças 2 e 3: 8% para ferro e 1% para diamante.
- Ameaça 4: 8% para diamante e 5% para ferro.
- Bosses: 5% de lendária.
- Infernal Mobs: 0,5% de lendária por modificador, limitado a 4%.
- Uma morte concede no máximo uma arma Divine.
- Armas Divine comuns chegam com 35–80% de durabilidade restante; lendárias com 70–95%.
- Armadura adicional: 2% / 3,5% / 5,5% / 8% conforme o tier, com 25–75% da durabilidade restante.
- Armas de fogo CGM: 1% em bosses; Infernal Mobs recebem 0,15% por modificador, limitado a 1,5%.
- Armas de fogo continuam craftáveis normalmente na CGM Workbench e não entram no loot de inimigos comuns.
- Armaduras vanilla continuam craftáveis.

## 3. Classificação de ameaça

Distribuição atual: **nível 0 = 117, nível 1 = 21, nível 2 = 54, nível 3 = 23, nível 4 = 9**.

Nível 0 agora significa passivo, neutro, parte de entidade ou utilidade. Foram promovidos 36 registros que estavam zerados, incluindo hostis de Alex's Caves/Alex's Mobs, Hoglin, Vex, Zoglin, Illusioner, Forgotten e Zombie Player. A listagem completa fica em `docs/CLASSIFICACAO-DE-AMEACAS.md`.

### Autoanálise

A classificação é suficiente para ativar as probabilidades de loot, Threat Sense e limites de profundidade sem confundir animais com monstros. Ela ainda precisa de testes de campo para o ajuste fino entre níveis 2 e 3. Os pontos que não podem ser determinados apenas por arquivos são: dano real depois de efeitos, alcance/velocidade da IA, frequência prática, dificuldade em espaços estreitos e sinergia com outros monstros. O tier 4 permanece reservado a bosses/encontros explícitos.

## 4. EnhancedAI

`Enable Mobs` e `Enable Anti-Cheese` estão desativados. Os demais módulos específicos já estavam desativados, incluindo Leap, Pearler, Miner, Fisher, Riding, Targeting, Movement e equipamentos de Shulker. Além disso, zumbis, husks, drowned, esqueletos e strays comuns têm armadura aleatória removida ao entrar no mundo; suas armas características continuam visíveis, e eles não recolhem nova armadura do chão. Monstros com função de horda, bosses e invocações são preservados. As funções de hordas continuam independentes em `HordeRoles`.

## 5. Profundidade

A fonte central é `kubejs/config/7dtc_depth_matrix.json`, que gera `config/incontrol/spawner.json` por `tools/rebuild-depth-spawns.ps1`.

Encontros ativos depois da ampliação:

- Rocky Roller: Y 24 até -40;
- Cave Centipede: Y -8 até -64;
- Watcher: Y -24 até -64, raro;
- Murmur: Y -40 até -64, muito raro;
- Forgotten: Y -12 até -64, raro;
- Cave Spider: Y 0 até -48;
- Silverfish: Y -20 até -64.
- Chort: Y 16 até -24;
- Imp: Y -16 até -48;
- Imp Guard: Y -24 até -56;
- Demon: Y -32 até -64;
- Demon Lord: Y -48 até -64, extremamente raro.

Todas as regras complementares exigem luz de bloco 0, posição de caverna e ao
menos 20 tentativas de posicionamento. Deep Dark, biomas sem monstros comuns e
os seis biomas do Alex's Caves foram excluídos da matriz genérica. A distribuição
detalhada fica em `docs/DISTRIBUICAO-DE-MOBS-POR-PROFUNDIDADE.md`.

O erro `Invalid condition 'maxlight' for spawner rule` foi corrigido: no In
Control 9.5.0, `maxlight` deve ficar dentro de `conditions.and`. O erro fazia as
regras complementares serem rejeitadas e explicava cavernas comuns vazias.

O ferro comum permanece com 20% menos tentativas que o vanilla. Chunks novos recebem ainda duas tentativas de um depósito profundo de até sete blocos entre Y -64 e -32. Isso aumenta um pouco a recompensa das cavernas profundas sem devolver a abundância geral anterior.

## 7. XP e relevância dos nós

Nova fórmula: `min(24 + level * 8 + level ^ 1.8, 480)`.

- nível 0: 24 XP, aproximadamente quatro zumbis comuns;
- nível 10: aproximadamente 167 XP;
- nível 30 em diante: teto de 480 XP por ponto.

Key Hunter foi elevado de 4% / 6% / 15% / 30% para **6% / 8% / 18% / 32%**. Conditioning e Fast Hands permanecem em +2% por nó porque possuem quatro ocorrências coerentes, chegando a +8% no conjunto; devem ser observados durante o teste de progressão antes de receber outro aumento.

## Validação realizada

- build Forge concluído com sucesso;
- JAR compilado e copiado para `mods/seven-days-locks-1.0.1.jar` com hash idêntico à saída de build;
- 224 IDs do registro central validados;
- oito JSON alterados analisados com sucesso;
- os sete encontros referenciam entidades existentes no registro;
- 36 IDs Divine e sete IDs de armas CGM validados nos JARs;
- JAR final contém `DivineWeaponDrops` e `EquipmentDrops`.

O Minecraft não foi aberto. As mudanças exigem reinício completo; `/reload` sozinho não recarrega o JAR nem o EnhancedAI.

## Teste de campo recomendado

1. Reiniciar o jogo e criar/visitar chunks ainda não gerados.
2. Em Y -24 a -40, permanecer em cavernas abertas por 10–15 minutos e registrar Rocky Roller, Centipede, Forgotten, Watcher, Cave Spider e Silverfish.
3. Matar amostras de inimigos níveis 1, 2 e 3 em modo survival para confirmar desgaste e limite de uma Divine por morte.
4. Confirmar que arma de fogo não cai de monstro comum.
5. Medir o tempo dos primeiros cinco pontos da árvore.
6. Comparar nível 2 e 3 quanto a dano, mobilidade e dificuldade em corredores.

## Backup

Estado anterior salvo em `C:\Users\carli\Documents\Codex\2026-09-20\e\backups\roadmap-20261012-013418`.

