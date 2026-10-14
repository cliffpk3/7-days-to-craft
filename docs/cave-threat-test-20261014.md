# Cave threat test — 2026-10-14

## Diagnóstico

- A população comum é abastecida pelo spawn natural, enquanto 14 encontros controlados usam `config/incontrol/spawner.json` gerado de `kubejs/config/7dtc_depth_matrix.json`.
- A probabilidade `persecond` é a chance de **iniciar uma operação** por segundo, não a chance de um monstro aparecer. Cada operação ainda precisa encontrar uma posição válida. Antes do teste, Watcher tinha 0,005, Demon 0,004 e Demon Lord 0,001 por segundo. Portanto, o primeiro podia sequer iniciar uma tentativa por cerca de 200 segundos em média.
- Os encontros controlados exigem caverna, luz de bloco 0 e posições a pelo menos 32 blocos do jogador. Muitos pontos de uma caverna grande falham nas condições de posição, ocupação ou bioma. A presença de lava, tochas e diferentes níveis verticais também reduz candidatos.
- Rocky Roller só tem spawn nativo em um conjunto restrito de biomas de caverna, segundo `config/alexsmobs/rocky_roller_spawns.json`. Fora deles, sua regra suplementar tem mais importância.
- Os demônios Realm RPG só entram no Overworld pelas regras controladas: `config/incontrol/spawn.json` nega tentativas que não venham do In Control.
- O código do pacote só reduz zumbis e esqueletos naturais abaixo de Y -33 quando já existem pelo menos dois substitutos perigosos em até 48 blocos. Isso evita esvaziar caves, mas também preserva a predominância visual dos inimigos comuns quando os especiais não conseguem aparecer.

## Configuração de teste instalada

| Monstro | `persecond` anterior → teste |
|---|---:|
| Zombie / Skeleton | 0,018 → 0,035 / 0,014 → 0,030 |
| Chort / Rocky Roller / Cave Spider | 0,018 → 0,050 / 0,030 → 0,055 / 0,014 → 0,028 |
| Centipede / Forgotten / Imp | 0,018 → 0,040 / 0,006 → 0,020 / 0,012 → 0,040 |
| Watcher / Imp Guard / Silverfish | 0,005 → 0,015 / 0,007 → 0,025 / 0,006 → 0,016 |
| Demon / Murmur / Demon Lord | 0,004 → 0,013 / 0,0025 → 0,008 / 0,001 → 0,0025 |

- As operações usam 28 tentativas de posição, exceto Demon Lord, que mantém 24. As 14 regras agora procuram pontos até 16 blocos acima ou abaixo do jogador.
- A distância mínima de 32 blocos, os tetos locais e as exclusões de Deep Dark, biomas próprios de Alex's Caves e biomas sem monstros padrão foram preservados.
- `OreCavePressure` examina uma área de 17 × 9 × 17 blocos em volta do jogador a cada dez segundos, sem carregar chunks novos. Com pelo menos três minérios da tag `forge:ores` expostos ao ar, pode acrescentar um encontro coerente com a altura; a chance cresce com a quantidade e é limitada a 45% por verificação. Coloca no máximo um monstro por verificação e não atua se já houver dez monstros próximos.
- Esse acréscimo só atua no Overworld, em cavernas até Y 32, fora dos biomas excluídos e da dificuldade Pacífica. O ponto de spawn precisa estar a 32–48 blocos, em um chunk carregado, com piso sólido, espaço para a entidade e **luz de bloco e de céu igual a zero**. Iluminar os pontos possíveis bloqueia essa pressão extra.

## Validação e limites

- `tools/rebuild-depth-spawns.ps1` gerou e validou as 14 regras. O JSON gerado foi relido e conferido.
- `seven-days-locks` compilou com `gradlew.bat build --offline`; o JAR foi instalado em `mods/seven-days-locks-1.0.1.jar`.
- **Pendente observar em jogo**: as taxas finais dependem do relevo, bioma, população existente e das regras internas de cada mod. Não há garantia literal de um monstro em cada corredor; o teste aumenta as oportunidades válidas e a presença de espécies especiais.
- O reforço por minério conta somente minérios expostos num raio curto. Veias inteiramente enterradas não aumentam a pressão até serem reveladas.
- Para comparar, explorar três cavernas comuns (Y 0 a -24, -25 a -40 e abaixo de -40), esperar pelo menos três minutos em cada uma sem colocar tochas e observar o número e a variedade de criaturas; depois iluminar uma área rica em minério e verificar se novas aparições param ali. `/incontrol showstats` e `/incontrol list` podem ajudar na inspeção.

## Backup e reversão

Backup local, fora do repositório do modpack: `C:\Users\carli\Documents\Codex\2026-09-20\e\backups\cave-threat-test-20261014-142740\`. Ele guarda a matriz, o spawner, o JAR anterior, `SevenDaysLocks.java` anterior e hashes SHA-256. Para reverter, fechar o jogo, copiar os três arquivos de configuração/JAR de volta aos caminhos originais e executar `tools/rebuild-depth-spawns.ps1`. Para também remover o código-fonte do reforço por minério, remover `OreCavePressure.java` da fonte do mod antes da próxima compilação. Nenhum save foi editado.

Referência técnica: https://mcjty.eu/docs/mods/control-mods/control-mods-20-concepts
