# Calendario da horda v1 (15/10/2026)

Rollback: `horde-calendar-v0-20261015`. Alteracoes desta versao: `horde-calendar-v1-20261015`.

O relogio do HUD mostra o dia do calendario visual: Day 1 comeca as 06:00 no primeiro tick do mundo; Day 2 comeca quando esse relogio passa de 23:59 para 00:00. A contagem da horda usa a mesma virada de dia. No inicio do Day 7 aparece **HORDE TONIGHT**, em vez de esperar ate 06:00.

`config/hordes-common.toml` agora usa o tempo do mundo (`hordeEventByPlayerTime = false`) para que jogadores da mesma LAN compartilhem o calendario. Ao entrar pela primeira vez com esta versao, cada evento pessoal existente e alinhado ao proximo dia multiplo de sete do mundo (Day 7, 14, 21...). A migracao nao altera o relogio ou os saves manualmente; o mod atualiza seu agendamento ao carregar o jogador e registra essa migracao nos dados dele. Se uma horda ja estiver ativa, a migracao espera seu termino.

O dia usado na reducao temporaria de dano dos primeiros dias foi alinhado ao mesmo calendario. Os fontes recompilaveis ficam em `source/seven-days-locks` e `source/seven-days-skill-bridge`; Java 17 e necessario para os builds.

Verificacao estatica: os dois mods compilaram com Gradle offline. Pontos para testar no Minecraft apos reiniciar: virada 23:59 -> 00:00 no HUD, titulo de contagem uma vez na virada, aviso HORDE TONIGHT no comeco do Day 7, e primeira horda real no ciclo esperado. Em mundos ja existentes, a primeira data pode mudar para alinhar os jogadores ao calendario coletivo.
