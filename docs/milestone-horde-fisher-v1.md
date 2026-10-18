# Horde Grapplers v1 — 18/10/2026

## Regra

- Cerca de 10% dos zumbis elegíveis de cada horda recebem o papel `7dtc_role_fisher`, nome Grappler e uma vara de pesca na mão secundária. Essa faixa substitui parte do papel Assault; Breaker, Tank, Disruptor e Summoner preservam suas faixas.
- A frequência pode ser ajustada entre 0% e 30% em `config/seven-days-horde-roles.toml` (`fisherChance`). Vale para novos spawns após reiniciar o jogo/servidor.
- O papel usa o objetivo e o gancho do EnhancedAI. Ele só é anexado aos mobs marcados pelo evento de spawn da horda, inclusive zombie villagers, husks e drowned quando aparecerem na tabela da horda.
- O módulo geral `Enable Mobs` e o módulo nativo `Enable Fisher Mobs` do EnhancedAI permanecem desativados. Zumbis comuns não recebem a habilidade. A chance nativa de roubar itens fica em zero.
- O lançamento nativo exige alvo visível, a até 24 blocos. Um mixin limita o impulso do Grappler a 0,55 bloco por tick na horizontal e 0,18 para cima; a colisão e a movimentação normais continuam valendo.
- A vara não cai como loot. O objetivo é recolocado ao carregar um Grappler salvo, sem criar cópias no mesmo objeto.

## Rollback

O JAR anterior e os arquivos de configuração anteriores estão em `backups/horde-fisher-v1-20261018/`. Recolocar o JAR anterior e voltar ao commit anterior remove o comportamento; os mobs já gerados com o papel podem continuar com vara até serem removidos.

JAR instalado: `mods/seven-days-locks-1.0.1.jar`, SHA-256 `B1A94B703DE6BB36696841045B80D0EDC15E8154933FF03DBE2DEB913C61F3BB`.

## Validação

- Feito: inspeção do comportamento do EnhancedAI e da ordem de eventos do The Hordes, compilação offline do mod e análise do TOML/JSON.
- Pendente no jogo: ativar horda em mundo descartável; observar Grapplers com vara entre zumbis da primeira e de hordas posteriores; conferir lançamento com alvo visível, ausência atrás de paredes, velocidade do puxão perto de desníveis, ausência de roubo de itens, drops da vara e nenhum Fisher em zumbis comuns. Repetir com dois jogadores e após salvar/reabrir o mundo.
