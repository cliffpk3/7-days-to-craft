# Roadmap de gameplay após City Raid v1

Estas propostas foram registradas para análise depois da instalação dos mods da cidade. Não fazem parte da instalação City Raid v1.

## Horda

- Testar mais inimigos que puxam ou reposicionam jogadores durante a horda, sem criar ataques inevitáveis.
- Entregar a recompensa individualmente a cada jogador online elegível; validar entrada e saída durante a horda e impedir recompensa duplicada.

## Habilidades

- Recuperação de flechas após o disparo.
- Detecção de animais: alcances progressivos de 20, 30 e 40 blocos.
- Threat Sense: detecção de monstros a 10, 20 e 30 blocos.
- Corrigir Ore Sense para respeitar o limite efetivo de 20 blocos, inclusive em cavernas grandes.
- Sorte: aumentar chances de drop em 30%, 60% e 90% de forma **relativa**. Exemplo: um drop de 1% passa a 1,9% no nível máximo, respeitando teto de 100%.
- Aumentar a durabilidade de armas e equipamentos por habilidade, definindo percentuais e compatibilidade com itens de mods.

## Monstros e mundo

- Investigar quebra lenta de blocos por monstros; os equipados com picareta devem quebrar mais rapidamente. Definir lista de blocos protegidos e limites para não destruir bases fora de combate.
- Avaliar substituição gradual de monstros místicos por zumbis variados e humanos armados, preservando diversidade de encontros e progressão por região.
- Auditar vilarejos e suas fontes de recursos antes de decidir entre remover o mod ou reduzir loot, armaduras e estruturas.

## Exploração e mapa

- Retirar a habilidade vinculada ao Xaero Map após definir a progressão substituta.
- Investigar descoberta de pontos de mapa por drops de inimigos locais e uma mesa para compartilhar informação cartográfica entre jogadores.
- Verificar os limites das APIs/configurações do Xaero e a persistência de mapa por jogador antes de escolher a implementação.

## Validação necessária

As alterações acima exigem testes em mundo separado, inclusive com dois ou mais jogadores, antes de afetarem o save principal. Registrar cada etapa com versão e ponto de rollback.
