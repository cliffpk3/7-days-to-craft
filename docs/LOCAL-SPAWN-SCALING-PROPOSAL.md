# Proposta: escala local de monstros comuns por jogadores próximos

Estado: proposta de balanceamento; ainda não altera o spawn do jogo.

## Regra inicial de teste

| Jogadores próximos | Alvo de população e reposição local |
| --- | --- |
| 1 | 100% |
| 2 | 150% |
| 3 ou mais | 150% (mesmo limite de dois jogadores) |

Contar apenas jogadores ativos na mesma dimensão, a até 64 blocos horizontais e 24 blocos verticais do jogador âncora. Um jogador na superfície não deve aumentar a população de uma caverna muito abaixo. Jogadores separados formam áreas independentes e recebem o balanceamento de um jogador cada. Reavaliar o agrupamento a cada 5–10 segundos, não a cada tick.

Aplicar a regra à população hostil normal em caves, estruturas exploráveis e superfície noturna. Manter as distâncias mínimas existentes entre spawn e jogador. Excluir os spawns da horda, chefes, invocações, ovos de spawn e blocos de spawner dedicados na primeira versão.

## Integração necessária

O modpack combina spawns naturais, 14 regras extras do In Control em `config/incontrol/spawner.json`, encontros de `CaveEcosystems` e pressão de minério de `OreCavePressure`. A escala deve usar um orçamento local compartilhado por esses sistemas; multiplicar cada chance isoladamente produziria excesso e regras com `maxlocal` continuariam limitando algumas espécies.

O In Control permite `persecond`, `attempts`, `amount` e `maxlocal`, mas não expõe um contador de jogadores próximos como condição simples de `spawner.json`. `perplayer` em contadores de `spawn.json` usa a quantidade de jogadores presentes, que não representa necessariamente o grupo local. A implementação deve ficar numa pequena camada Java configurável e identificável por origem do spawn, preservando as regras de bioma, altura, luz e distância de cada fonte.

## Validação antes de ativar

1. Medir por alguns minutos a população hostil, tentativas aceitas e origem dos mobs em áreas equivalentes com 1, 2 e 3 jogadores; confirmar que o terceiro não eleva a meta acima de 150%.
2. Testar dois jogadores juntos na mesma cave, em caves separadas, e um na superfície enquanto outro está abaixo.
3. Verificar horda, cooldown do Rocky Roller, limites por espécie e desempenho do servidor.
4. Ajustar raio e orçamento conforme os dados de campo; manter um arquivo de configuração para rollback simples.
