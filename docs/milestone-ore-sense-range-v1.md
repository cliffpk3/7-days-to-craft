# Ore Sense: limite de 20 blocos (v1)

Em 18/10/2026, removemos a emissao global de luz dos minerios no getter sem posicao. O `BlockLightEngine` continua usando `OreSenseOverlay.lightEmission`, que aplica luz nivel 6 apenas aos minerios situados ate 20 blocos da posicao do jogador quando a skill esta ativa.

## Instalacao e rollback

- JAR instalado: `mods/seven-days-skill-bridge-1.0.1.jar`.
- JAR anterior: `backups/ore-sense-range-v1-20261018/seven-days-skill-bridge-1.0.1.jar`.
- Para reverter, fechar o Minecraft e copiar o JAR anterior sobre o instalado.

## Verificacao

- `gradlew --offline clean build` com JDK 17: passou.
- Hash SHA-256 do JAR compilado igual ao instalado: `6FA8B007B2C56B7F59F9C32CE09DB18E80340F4F95CD8475FAC42ECEFD415E41`.
- Teste visual do usuario: falhou; minerios proximos deixaram de brilhar. Esta versao foi substituida pela `ore-sense-range-v2-20261018`.

Esta correcao limita a luz produzida pela skill. Qualquer brilho visual independente aplicado pelo shader ou por outro mod deve ser avaliado separadamente durante o teste em jogo.
