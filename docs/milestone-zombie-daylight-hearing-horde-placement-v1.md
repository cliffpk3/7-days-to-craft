# Sol, audicao e spawn da horda (v1)

Preparado e instalado em 18/10/2026, depois que o Minecraft foi fechado. O JAR compilado esta em `staging/zombie-daylight-hearing-horde-placement-v1-20261018/seven-days-locks-1.0.1.jar` e foi copiado para `mods/seven-days-locks-1.0.1.jar`.

## Regras

- `config/hordes-common.toml`: zumbis comuns e zumbis de jogadores nao queimam mais ao sol. Esqueletos seguem a regra anterior. Os tres mobs Zombie Extreme citados pelo jogador sao subclasses de `Monster` e nao possuem a rotina vanilla de combustao solar.
- Audicao (ajuste v1.1): zumbis comuns mantem os raios da configuracao, inclusive `minecraft:zombie`, `zombie_extreme:infected`, `zombie_extreme:runner` e `zombie_extreme:crawler`. Variantes de ameaca nivel 2 ou maior e outras variantes do Zombie Extreme escutam a metade da distancia (um quarto da distancia ao quadrado). Zumbis em combate e zumbis da horda seguem sem investigar sons.
- Horda: antes de cada onda, sao sondados ate 96 locais em chunks carregados, a 80-112 blocos do jogador. O local deve aceitar o tipo de spawn da horda, manter pelo menos 64 blocos dos demais jogadores e nao estar em linha de visao direta. Posicoes atras de blocos opacos sao preferidas; uma posicao fora do campo de visao serve de alternativa. Se nenhuma for encontrada, aquela onda nao gera mobs junto ao jogador.

## Backup e verificacao

- JAR anterior e TOML anterior: `backups/zombie-daylight-hearing-horde-placement-v1-20261018/`.
- `gradlew --offline clean build` com JDK 17: passou.
- Hash SHA-256 do JAR instalado `1307DC6FE3C36F9B387A9F03347B4331CAB0AE66D957745EE082F1C45F6AA764`. Falta reiniciar e validar no jogo.
- Ajuste v1.1 instalado: SHA-256 `254537CE6264D9F57A376428C196EDFE5238DBC1DD458A3269DBE57E37AAD813`; copia anterior em `backups/zombie-hearing-strength-v1.1-20261018/`.
- Testar ao amanhecer em campo aberto: zombie vanilla e zombie player nao queimam. Confirmar que esqueleto ainda queima.
- Atirar com um zumbi de cada grupo dentro/fora do respectivo raio, sem alvo de combate.
- Iniciar horda em campo aberto e entre edificios, com um e dois jogadores; observar a primeira aparicao, conferir que nenhum mob surge na tela ou junto ao player, e medir se ondas continuam regulares. Se nao houver local carregado e oculto, a onda pode ser omitida.
