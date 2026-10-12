# Distribuicao de mobs por profundidade

Atualizado em 12/10/2026. A fonte executavel desta matriz e
`kubejs/config/7dtc_depth_matrix.json`; o arquivo
`config/incontrol/spawner.json` e gerado a partir dela.

## Regras gerais

- Os encontros complementares so procuram posicoes de caverna com luz de bloco 0.
- Cada tentativa cria somente uma criatura.
- Cada tipo possui limite local pequeno, evitando aglomeracoes artificiais.
- Deep Dark, biomas com a tag `forge:no_default_monsters` e todos os biomas do
  Alex's Caves ficam fora da matriz generica.
- Os biomas especiais do Alex's Caves usam suas proprias populacoes e mantem sua
  identidade.
- Zumbis e esqueletos comuns continuam naturais ate Y -32. Abaixo disso, so sao
  reduzidos quando ja existem pelo menos duas criaturas substitutas proximas.
- Infernal Mobs nunca e removido pela substituicao de profundidade.

## Faixas do Overworld

| Faixa | Altura | Populacao complementar | Papel |
|---|---:|---|---|
| Superficie e entradas | Y 33 ou mais | Nenhuma | Mantem o comportamento nativo da superficie. |
| Caverna rasa | Y 0 a 32 | Rocky Roller; Zombie, Skeleton e Chort ate Y 16 | Primeiro contato com variedade subterranea. |
| Caverna media | Y -24 a -1 | Zombie, Skeleton, Chort, Rocky Roller, Cave Spider, Cave Centipede, Forgotten e Imp; Watcher e Imp Guard entram em Y -24 | Mistura de ameacas comuns e nivel 2. |
| Caverna profunda | Y -40 a -25 | Rocky Roller, Cave Spider, Cave Centipede, Forgotten, Imp, Watcher, Imp Guard, Silverfish e Demon; Murmur entra em Y -40 | Substituicao gradual dos comuns por criaturas mais perigosas. |
| Abissal | Y -64 a -41 | Cave Centipede, Forgotten, Imp, Watcher, Imp Guard, Silverfish, Demon e Murmur; Demon Lord entra em Y -48 | Ameacas de nivel 2 e 3, com um encontro de nivel 4 extremamente raro. |

Os limites se sobrepoem de proposito. A mudanca de dificuldade acontece pela
variedade de criaturas e nao por multiplicadores grandes aplicados ao mesmo mob.

## Born in Chaos

Os spawns naturais do Born in Chaos passam por uma segunda camada de controle em
`kubejs/config/7dtc_mob_registry.json`:

| Faixa principal | Criaturas naturais do Born in Chaos |
|---|---|
| Y -24 a 96 | Baby Skeleton, Bloody Gadfly, Corpse Fly, Decaying Zombie, Decrepit Skeleton, Restless Spirit, Spirit Guide Assistant, Swarmer, Thornshell Crab, Zombie Clown, Zombie Fisherman e Zombie Lumberjack |
| Y -40 a 32 | Barrel Zombie, Bonescaller, Dark Vortex, Door Knight, Dread Hound, Phantom Creeper, Seared Spirit, Siamese Skeletons, Skeleton Demoman, Spirit Guide e Spirit of Chaos |
| Y -64 a -8 | Dire Hound Leader, Fallen Chaos Knight, Nightmare Stalker, Sir Pumpkinhead, Skeleton Thrasher, Supreme Bonescaller e Zombie Bruiser |
| Y -64 a -40 | Lifestealer e Missionary |

Cada grupo também recebe uma redução progressiva na tentativa natural: 75% para
nível 1, 60% para nível 2, 40% para nível 3 e 18% para nível 4. Formas invocadas,
partes de entidades e chefes vinculados a estruturas são classificados para loot
e Threat Sense, mas não recebem uma nova injeção de spawn.

## Situacao observada em Y -14

Em uma caverna comum, ampla e escura em Y -14, podem aparecer pela matriz:

1. Zombie;
2. Skeleton;
3. Chort;
4. Rocky Roller;
5. Cave Spider;
6. Cave Centipede;
7. Forgotten.

O conjunto representa cerca de 0,118 operacoes por segundo antes da escolha do
jogador, procura de uma posicao valida e limites locais. Com tres jogadores em
LAN, as tentativas sao distribuidas entre as regioes dos jogadores. Os spawns
naturais continuam contribuindo em paralelo.

## Biomas especiais do Alex's Caves

| Bioma | Populacao nativa relevante |
|---|---|
| Abyssal Chasm | Deep Ones, Deep One Knight, Deep One Mage, Hullbreaker e fauna aquatica. |
| Candy Cavity | Caniac, Gumbeeper, Gum Worm, Caramel Cube, Candicorn e Gummy Bear. |
| Forlorn Hollows | Underzealot, Corrodent, Vesper e Gloomoth. |
| Magnetic Caves | Magnetron, Teletor, Boundroid e Ferrouslime, alem de hostis vanilla. |
| Primordial Caves | Vallumraptor, Tremorsaurus, Atlatitan e demais fauna pre-historica. |
| Toxic Caves | Brainiac, Nucleeper, Gammaroach e Raycat, alem de hostis vanilla. |

Esses biomas nao recebem Zombie, Skeleton, Chort ou outros preenchimentos da
matriz. Eles ja possuem densidade e composicao proprias.

## Diagnostico e calibracao

- `/incontrol reload`: recarrega os arquivos sem reiniciar o Minecraft.
- `/incontrol info`: mostra luz, horario e contexto da posicao atual.
- `/incontrol list`: lista as entidades presentes na dimensao.
- `/incontrol showstats`: grava estatisticas de correspondencia das regras no log.
- `/incontrol clearstats`: limpa as estatisticas antes de uma nova amostra.

Para calibrar densidade, medir por pelo menos cinco minutos em uma caverna comum,
mantendo areas validas entre 24 e 64 blocos dos jogadores. Ajustar primeiro
`persecond`; os limites locais devem permanecer como protecao contra excesso.

## Restricao conhecida da sessao LAN

A distancia de simulacao atual do host e 5 chunks. Isso reduz o volume disponivel
para o spawn natural quando varios jogadores estao separados. A matriz do
InControl compensa parte desse efeito, mas uma distancia 7 e o alvo recomendado
para a proxima sessao caso o desempenho do host permita.
