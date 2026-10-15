# Cave ecosystems v1 (15/10/2026)

Baseline de rollback: tag `cave-ecosystems-v0-20261015`. Esta versao fica na tag `cave-ecosystems-v1-20261015`.

## Como funciona

`config/7dtc_cave_ecosystems.json` define cinco familias subterraneas: stone crawlers, demonkin, dread hounds, restless dead e infested passages. O mod sorteia uma familia estavel para cada setor horizontal de 96 blocos e faixa de profundidade, usando seed e bioma. Perto das bordas, mistura os setores vizinhos gradualmente. Os encontros usam grupos pequenos da mesma familia. As regras naturais e os 14 encontros do InControl continuam ativos.

O fonte compilavel do mod esta em `source/seven-days-locks`. Requer Java 17 para o Gradle 7.4; as quatro dependencias locais sao lidas de `mods/`. Executar `source/seven-days-locks/gradlew.bat build --offline` com `JAVA_HOME` apontando para Java 17 gera o JAR em `source/seven-days-locks/build/libs/`.

Surgem apenas em cavernas escuras, carregadas, a pelo menos 32 blocos do jogador. Nao aparecem em Deep Dark, biomas proprios do Alex's Caves ou `forge:no_default_monsters`. A rotina verifica a cada 10 segundos, tenta um encontro com 25% de chance, limita a 12 hostis e seis mobs de ecossistema num raio local, e espera no minimo 60 segundos antes de criar outro encontro no mesmo setor. Esses limites sao configuraveis no JSON. A densidade final ainda precisa de teste no jogo, principalmente com dois jogadores na LAN.

## Loot e ameaca

Cada mob criado recebe a tag `7dtc_ecosystem_spawn` e entra no mesmo evento de drops dos outros hostis. O nivel vem de `kubejs/config/7dtc_mob_registry.json`, nao da familia escolhida. A rotina de ecossistema nao acrescenta drop proprio.

- Divine Weaponry: nivel 1, 8% pedra e 1% ferro; niveis 2 e 3, 8% ferro e 1% diamante; nivel 4 usa o pool de elite. Uma rolagem por morte com jogador. Drops nativos Divine sao removidos antes da rolagem controlada.
- Chaves: as chances existentes variam com nivel de ameaca, Infernal Mobs e Key Hunter. Demonios, hounds e demais entidades novas usam essa mesma classificacao.
- Blueprints: chances existentes de 2%, 4%, 6% e 10% por nivel, somente quando o jogador mata o mob. Armas de fogo comuns continuam fora desses drops.
- Equipamentos e drops nativos nao Divine seguem suas regras existentes. O aumento no numero de mobs pode aumentar o loot total por caverna; por isso a v1 limita frequencia e populacao.

## Checklist obrigatorio ao adicionar um mob

1. Registrar ID, atributos, altura/bioma, `spawn.allow` e nivel 1-4 em `kubejs/config/7dtc_mob_registry.json`.
2. Atribuir o mob a um encontro da matriz de profundidade ou a uma familia do ecossistema, com altura e grupo coerentes. Nao duplicar bosses comuns nem biomas exclusivos de outro mod.
3. Conferir se seu tier produz a chave, arma Divine, equipamento e blueprint desejados; conferir drops nativos especiais para evitar duplicacao ou bypass.
4. Confirmar que o Advanced Compass so mostra players e tumbas. `tools/enforce-mob-policy.ps1` corrige os arquivos de icones; o filtro do mod cobre entidades criadas depois da inicializacao.
5. Executar `tools/validate-mob-registry.ps1` e `tools/enforce-mob-policy.ps1`, compilar o mod e testar spawn, distancia, loot e desempenho num mundo de teste. O script de politica roda automaticamente nos BATs de publicar e atualizar.

Os valores de armas Divine e chaves ainda estao no codigo do mod; blueprints estao em `config/7dtc_gun_blueprints.json`. O script verifica classificacao, pools de blueprint e icones, mas nao substitui um teste de drops no Minecraft.

## Rollback

Restaurar a tag baseline para os arquivos do pacote e o JAR, ou definir `"enabled": false` no JSON para desligar apenas os encontros adicionais. Alteracoes no JAR exigem reinicio do jogo; o JSON e carregado no inicio do servidor.
