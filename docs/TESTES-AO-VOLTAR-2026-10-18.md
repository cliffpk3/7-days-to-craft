# Testes no jogo ao voltar ao PC — 18/10/2026

Usar primeiro um mundo de teste ou copia do save. Anotar dia, coordenadas, skill ativa e numero de jogadores; um video curto de cada falha ajuda a reproduzir. A lista sera ampliada conforme novas etapas forem instaladas.

## Prioridade 1 — correcoes instaladas, ainda sem teste de jogo

1. **Ore Sense v2:** ativar a skill e comparar o mesmo minerio a 19, 20 e 21 blocos; aproximar e afastar, inclusive em caverna grande. Repetir com a skill desativada e com shaders. Esperado: brilho somente com a skill e dentro do raio de 20 blocos. A v1 falhou deixando de iluminar ate minerios proximos.
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
- Arrow Retrieval: conferir os quatro níveis (9%, 18%, 27%, 35%), flecha recuperada ao se aproximar do monstro atingido ou após sua morte, dono da flecha, Infinity, flechas especiais, inventário cheio e ausência de reembolso de munição CGM/TaCZ. Flechas fincadas no chão mantêm a coleta nativa.
- Mais inimigos que puxam na horda: EnhancedAI possui Fisher, mas a funcionalidade e seu modulo pai `Mobs` estao desativados; a tag atual so aceita `minecraft:zombie`. A proxima etapa precisa habilitar ambos sem ativar equipamento/IA aleatorios para zumbis comuns fora da horda, equipar apenas uma parcela controlada dos zumbis de evento e testar alcance, linha de visao, cooldown e seguranca perto de paredes/desniveis.
