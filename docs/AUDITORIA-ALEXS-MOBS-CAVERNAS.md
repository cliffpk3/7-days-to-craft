# Auditoria — Alex's Mobs nas cavernas

Data da comparação: 2026-10-11

Pacotes comparados:

- `modpack`: Alex's Mobs 1.22.8 e Alex's Caves 1.1.4.
- `7-days-to-craft`: Alex's Mobs 1.22.9 e Alex's Caves 2.0.2.

## Rocky Roller

O Rocky Roller está instalado no 7 Days to Craft (`alexsmobs:rocky_roller`), mas sua aparição não reproduzia a experiência do modpack antigo.

No modpack antigo:

- peso nativo: 80;
- `SpawnRolls`: 1;
- havia uma segunda regra explícita no InControl;
- aparecia entre Y -64 e Y 30;
- dividia um limite local de quatro criaturas com outros monstros subterrâneos.

No 7 Days to Craft, antes da correção:

- peso nativo: 60;
- `SpawnRolls`: 0;
- não havia regra no InControl.

Isso explica por que o jogador não encontrava o Rocky Roller.

Configuração adotada:

- spawn controlado pelo InControl;
- somente no Overworld;
- entre Y -64 e Y 24;
- distância de 32 a 64 blocos do jogador;
- um por tentativa;
- máximo local de dois;
- frequência intencionalmente menor do que a configuração antiga.

No registro central do 7DTC ele é uma ameaça de nível 2, na categoria `dangerous cave creature`.

## Outros monstros subterrâneos do Alex's Mobs

O modpack antigo fazia estas criaturas aparecerem com mais frequência que a configuração inicial do 7DTC:

| Criatura | Modpack antigo | 7DTC ajustado | Observação |
|---|---:|---:|---|
| Cave Centipede | rolls 1 | rolls 2 | ameaça 2; continua rara |
| Dropbear | rolls 1 | rolls 2 | ameaça 2; continua rara |
| Skreecher | rolls 1 | rolls 2 | ameaça 2; criatura de caverna |
| Murmur | rolls 1 | rolls 2 | ameaça 2; criatura de caverna profunda |

No Alex's Mobs, quanto maior o número de `SpawnRolls`, mais tentativas são descartadas e mais rara fica a criatura. O valor 2 mantém esses encontros especiais visíveis sem reproduzir a densidade alta do modpack antigo.

Outras diferenças encontradas:

| Criatura | Modpack antigo | 7DTC atual |
|---|---:|---:|
| Cockroach | rolls 0 | rolls 3 |
| Crimson Mosquito | rolls 0 | rolls 4 |
| Guster | rolls 0 | rolls 4 |
| Skelewag | rolls 0 | rolls 3 |

Essas diferenças foram mantidas. As quatro criaturas eram muito mais comuns no modpack antigo, mas não fazem parte do núcleo atual de encontros subterrâneos e restaurar `0` aumentaria bastante a população global.

## Alex's Caves e outros encontros antigos

O InControl antigo também injetava o `alexscaves:watcher` no intervalo Y -64 a Y 0, junto de demônios do Realm RPG e Mutant Monsters. O bloco misto não foi copiado, pois permitia até três criaturas por tentativa.

O Watcher foi restaurado separadamente como evento raro:

- somente no Overworld e entre Y -64 e Y 0;
- surge de 48 a 80 blocos do jogador;
- apenas um por tentativa e no máximo um na região;
- frequência de tentativa oito vezes menor que a do Rocky Roller;
- ameaça nível 3, categoria `rare deep cave threat`.

Essa é a criatura que possui o jogador e transfere temporariamente sua câmera para a perspectiva do monstro. A configuração nativa de possessão permanece ativa, com cooldown de 300 ticks entre tentativas.

O segundo grupo subterrâneo antigo misturava Rocky Roller, monstros do WRD, Wither Skeleton e World of Rot entre Y -64 e Y 30. Somente o Rocky Roller foi restaurado nesta etapa.

## Configurações especiais antigas que não devem ser copiadas

### EnhancedAI

O datapack antigo colocava Rocky Roller, Dropbear, Skreecher, Murmur e diversas outras criaturas do Alex's Mobs na lista `can_be_miner`. Também permitia troca de alvo e outras rotinas globais de IA.

Essa integração não será copiada. No 7DTC, `Miner Mobs`, `Fisher Mobs` e `Leap` estão desativados para impedir equipamentos ou movimentos inesperados em monstros comuns. Funções especiais de horda continuam sendo aplicadas pelo sistema próprio de hordas.

### Drops adicionais

O modpack antigo aplicava drops aleatórios desconectados da identidade das criaturas. Exemplos: Grappling Hook no Rocky Roller, recursos do Twilight Forest no Dropbear, granada no Cockroach e grandes pilhas de tochas ou flechas em outros monstros.

Essas tabelas não serão copiadas. Os drops devem seguir os níveis de ameaça, as chaves do 7DTC e os futuros pools controlados de equipamento.

### Bad Mobs

O Bad Mobs não bloqueava o spawn normal das criaturas analisadas. Seus quatro meios de criação estavam liberados: natural, spawner, spawn egg e conversão. Portanto, ele não era responsável pela presença especial do Rocky Roller.

## Estado de validação

- Arquivos JSON do InControl, registro central e Advanced Compass: sintaxe validada.
- Rocky Roller: regra instalada, ainda requer encontro em jogo ou teste administrativo.
- Watcher: encontro profundo e raro instalado; possessão preservada e ainda requer validação em jogo.
- Não foi necessário abrir ou reiniciar o Minecraft nesta etapa.

## Candidatos para integração futura

Os seis biomas do Alex's Caves estão habilitados. Eles só começam a ser gerados longe do spawn:

| Bioma | Distância mínima do spawn |
|---|---:|
| Abyssal Chasm | 400 blocos |
| Magnetic Caves | 400 blocos |
| Primordial Caves | 450 blocos |
| Candy Cavity | 500 blocos |
| Forlorn Hollows | 650 blocos |
| Toxic Caves | 650 blocos |

Essa distância explica por que diversas criaturas instaladas ainda não foram observadas. A adaptação recomendada é usar poucos monstros como antecipação dos ambientes futuros e preservar o restante dentro de seus biomas.

### Recomendados como encontros eventuais

| Criatura | Origem | Proposta 7DTC | Cuidados |
|---|---|---|---|
| Farseer | Alex's Mobs | Elite nível 3, extremamente raro em cavernas profundas, máximo de um | Testar em LAN antes; substituir ou condicionar o drop `farseer_arm` |
| Vesper | Alex's Caves | Predador nível 2, aparição rara abaixo de Y 0, grupos de 1–2 | Drops simples (`vesper_wing` e `guano`) não quebram progressão |
| Vallumraptor | Alex's Caves | Caçador nível 2, encontro raro em dupla nas cavernas profundas | Não usar os grupos nativos de 6–7 fora das Primordial Caves |
| Mimicube | Alex's Mobs | Encontro nível 2 exclusivo do End | Manter fora do Overworld; funciona como variedade de fase tardia |

O Farseer é o candidato mais forte para um segundo evento excepcional: possui 70 de vida, armadura, combate corpo a corpo e um raio que causa dano proporcional à vida do alvo. Seu spawn nativo fica restrito à proximidade da borda do mundo, então ele é praticamente invisível durante uma campanha normal.

### Devem permanecer ligados ao próprio bioma

| Grupo | Motivo |
|---|---|
| Underzealot e Gloomoth | O ritual entre eles é a origem natural do Watcher e conta uma história própria das Forlorn Hollows |
| Brainiac, Gammaroach e Nucleeper | Radiação, Fissile Core e outros recursos pertencem à progressão das Toxic Caves |
| Magnetron, Teletor, Boundroid e Ferrouslime | Dependem da identidade magnética e de materiais do respectivo bioma |
| Deep Ones, Mine Guardian e Hullbreaker | Ecossistema aquático, trocas e explosivos devem continuar exclusivos da Abyssal Chasm |
| Criaturas da Candy Cavity | Itens e estética próprios; espalhá-las reduziria a identidade e a descoberta do bioma |

### Já funcionam sem uma nova regra global

- Cave Centipede: encontro subterrâneo abaixo de Y 0.
- Murmur: encontro profundo abaixo de Y -30.
- Skreecher: encontro ligado ao Deep Dark; pode invocar Warden e não deve receber spawn forçado.
- Dropbear e Bone Serpent: ameaças próprias do Nether.
- Rocky Roller e Watcher: já receberam regras raras controladas do 7DTC.

### Não forçar como encontros aleatórios

- Tremorzilla e Luxtructosaurus: poder destrutivo e escala de boss.
- Forsaken: boss das Forlorn Hollows e fonte de `pure_darkness`.
- Mine Guardian: explosão e `depth_charge` podem furar a progressão de explosivos.
- Nucleeper: explosão e `fissile_core` antecipam conteúdo nuclear.
- Skreecher: risco de transformar uma aparição aleatória em encontro com Warden.

## Pendência do registro central

Vesper, Underzealot, Corrodent, Vallumraptor e alguns monstros da Candy Cavity ainda constam como nível 0 no registro central, apesar de serem hostis ou perigosos em seu contexto. Eles devem ser classificados antes da etapa de loot por nível, Threat Sense e experiência escalonada. Essa classificação ainda não altera seus spawns.
