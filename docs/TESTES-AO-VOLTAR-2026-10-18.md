# Testes no jogo ao voltar ao PC — 18/10/2026

Usar primeiro um mundo de teste ou copia do save. Anotar dia, coordenadas, skill ativa e numero de jogadores; um video curto de cada falha ajuda a reproduzir. A lista sera ampliada conforme novas etapas forem instaladas.

## Prioridade 1 — correcoes instaladas, ainda sem teste de jogo

1. **Ore Sense I/II:** com apenas o nível I, comparar o mesmo minério a 19, 20 e 21 blocos. Com o nível II, confirmar brilho de minérios mais distantes que estejam em chunks carregados. Repetir sem a skill e com shaders; conferir se brilho e desempenho permanecem estáveis.
2. **Aparicao da horda:** iniciar uma horda de teste em campo aberto e outra na cidade, com um jogador e depois dois. Esperado: nenhum mob materializa perto ou na tela dos jogadores; as ondas continuam surgindo. Verificar se uma onda e omitida quando nao ha local carregado e oculto.
3. **Zumbis ao sol:** esperar amanhecer ao lado de zumbi vanilla, zumbi de jogador e variantes Zombie Extreme. Esperado: zumbis nao queimam; esqueletos ainda seguem sua regra de combustao.
4. **Audicao:** produzir disparos TaCZ com zumbis comuns e fortes sem alvo ativo a distancias abaixo e acima do alcance configurado. Repetir enquanto um zumbi ja persegue um jogador. Esperado: os comuns investigam dentro do raio, os fortes precisam estar 50% mais perto e ninguem abandona um alvo de combate para investigar som.
5. **Recompensa da horda v1:** com dois jogadores, terminar uma horda sem mortes; cada um recebe uma chave nivel 2 e um cache nivel 2. Repetir com morte e respawn de um jogador, inventario cheio, parada por comando e reconexao. Nao deve haver pagamento ao jogador que morreu nem pagamento duplicado no mesmo dia.

## Prioridade 2 — integracoes urbanas que precisam de observacao

6. **Cidade nova:** confirmar posto ocupado por milicia, soldados no terraco, ausencia de carros novos, ruas e subsolo gerados corretamente. Cidades/chunks antigos preservam estruturas ja colocadas.
7. **Entrada na cidade:** com dois jogadores, entrar, sair, morrer e reconectar. Esperado: uma emboscada de 2 a 4 zumbis para cada jogador por cidade; outra cidade permite novo evento.
8. **Combate ambiental:** observar um esquadrao surgindo fora de vista com zumbis proximos; deve combater zumbis e tambem atacar jogadores que se aproximem. Registrar se ha milicianos ou spawners em excesso e eventuais quedas de TPS.
9. **Workbench e armas:** verificar receitas/blueprints TaCZ, consumo de municao CGM, HUD de municao e ausencia de craft das armas CGM antigas.

## Etapas seguintes

- Presenca na base para receber a recompensa: adiada ate existir um core ou uma area de base confiavel.
- Arrow Retrieval: conferir os quatro níveis (7%, 14%, 21%, 28%) para flechas comuns cravadas e munição CGM consumida por armas TaCZ selecionadas. Testar dono da flecha, Infinity, flechas especiais, inventário cheio, tiro que erra e pellets de shotgun sem múltiplos reembolsos. Embedded Arrows mantém chance separada de 30% após matar a criatura; flechas fincadas no chão mantêm a coleta nativa.
- Fire Swallower: agachar e clicar com o botão direito no ar com tocha na mão principal. Confirmar consumo de uma tocha, 10 segundos de visão noturna por carga, limite de 60 segundos e colocação normal de tocha ao mirar em bloco. Verificar os três passos da quest e o ícone Fire Charge.
- Mobility e Engineering: Soft Landing deve negar integralmente o dano de queda em 6% por nó; Maintenance deve preservar durabilidade em 10% por nó (até 40%); Tool Proficiency deve aumentar a velocidade com ferramenta correta em 10% por nó; Enduring Vitality deve curar 4 HP a cada 45 segundos após 20 segundos sem dano. Conferir a nova disposição dos nós e os ícones.
- Grapplers da horda v1: em mundo de teste, observar a vara em aproximadamente 10% dos zumbis elegíveis da horda, incluindo husks e drowned quando surgirem. Testar alvo visível a até 24 blocos, parede entre mob e player, força do puxão perto de paredes/desníveis, ausência de roubo de itens e vara como drop, zumbis comuns sem Fisher, dois jogadores e gravação/reabertura do mundo. O módulo geral do EnhancedAI segue desativado.
