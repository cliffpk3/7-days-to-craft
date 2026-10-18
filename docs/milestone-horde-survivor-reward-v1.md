# Recompensa individual da horda — v1

Regra aprovada em 18/10/2026: cada jogador cuja horda termina normalmente e que nao morreu durante o evento recebe uma chave nivel 2 (`wrd:keystrongholdchest_gold`) e um cache nivel 2 (`seven_days_locks:mimic_cache_2`). O cache e colocado no mundo e exige a chave correspondente para abrir; ao abrir, concede um artefato de rank 2 pela regra ja existente. Morte e desconexao nao mudam as regras normais do mod The Hordes; o contador de mortes e o registro da ultima recompensa sao preservados ao respawn. A mesma pessoa nao recebe a recompensa duas vezes no mesmo dia de horda. Uma horda encerrada pelo comando de parada nao paga recompensa.

Nesta versao, estar na base nao e condicao: ainda nao existe um core/area de base confiavel para medir presenca. O criterio temporario e apenas nao ter morrido.

Backup do JAR anterior: `backups/horde-survivor-reward-v1-20261018/`. JAR novo instalado e guardado em `staging/horde-survivor-reward-v1-20261018/`; SHA-256 `576D05F2D91C73AE69BC755C5B73499F473474AF514EA26C960582844E2A1DFE`. A compilacao offline com JDK 17 passou. Ainda falta validacao em jogo.

## Testes em copia do mundo

- Dois jogadores sobrevivem: cada um recebe exatamente uma chave e um cache.
- Um jogador morre e outro sobrevive: so o sobrevivente recebe. Repetir com morte e respawn durante a mesma horda.
- Um jogador fica longe ou se desconecta e retorna: observar como o The Hordes encerra o evento pessoal e se a recompensa segue a regra de sobrevivencia.
- Inventario cheio: os dois itens devem aparecer no chao perto do jogador sem duplicacao.
- Encerrar a horda por comando, repetir termino no mesmo dia e testar proxima horda natural: nao pagar ao interromper; nao pagar duas vezes no mesmo dia; pagar normalmente no proximo ciclo.
