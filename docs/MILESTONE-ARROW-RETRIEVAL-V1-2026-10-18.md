# Arrow Retrieval v1 — 2026-10-18

- Os quatro nós `combat_1_slot15`, `slot16`, `slot18` e `slot20` permanecem nas posições originais. Os pontos já investidos são preservados.
- Os níveis concedem 9%, 18%, 27% e 35% de chance de recuperar uma flecha comum que cause dano a uma criatura. O sorteio ocorre uma vez por projétil.
- A flecha fica disponível ao dono quando ele se aproxima da criatura atingida (2,5 blocos) ou cai no local se ela morrer antes. Após 30 segundos, outros jogadores podem recolher a flecha caída. Registros em criaturas expiram após 2 minutos.
- Flechas fincadas em blocos continuam com a coleta nativa. Flechas especiais, Infinity, perfurantes e munição de armas de fogo não recebem recuperação extra.
- Arquivos anteriores: `backups/arrow-retrieval-v1-20261018/`. JAR instalado: `mods/seven-days-skill-bridge-1.0.1.jar`, SHA-256 `BB6DAF79F7914DCDE1FC91A31117D89D537E234968BDE2A0AB60DEDF3DEC7340`.
- Validação feita: compilação Gradle offline, leitura JSON, quatro referências à definição, igualdade de hashes entre JAR compilado, preparado e instalado. Ainda requer teste no jogo.

## Teste no jogo

1. Confirmar que a árvore mostra os quatro nós e as chances progressivas, inclusive num personagem que já os havia desbloqueado.
2. Atirar flechas comuns em criaturas e se aproximar; repetir com morte da criatura antes da aproximação.
3. Repetir com inventário cheio, dois jogadores, Infinity, flecha com efeito e perfuração.
4. Disparar armas CGM e TaCZ e confirmar que não há devolução de cartuchos.
