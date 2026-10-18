# Roadmap de gameplay — outubro de 2026

## Revisão preparada em 18/10/2026

O nó inicial de Scavenging passa a Forager. Os quatro nós posteriores de Field Dressing passam a Animal Sense (9/18/27/33 blocos no Advanced Compass). Ranger mantém quatro nós Arrow Retrieval (9/18/27/35%) e sua milestone superior passa a recuperar flechas cravadas no monstro na morte (30%). A trilha inferior de combate tem Threat Sense I (10 blocos) e II (30 blocos). Esta revisão foi compilada, mas ainda precisa de validação no jogo.

O nó antigo Cartography/Minimap passa a Ore Sense II. Ore Sense I ilumina minérios num raio de 20 blocos; o nível II remove o limite para minérios carregados. Xaero Minimap foi retirado, com backup do JAR. O Atlas continua disponível sem a antiga trava de Cartography. Os tooltips das habilidades pagas mostram o custo real em amarelo.

A audição de disparos e gritos foi estendida a monstros hostis com navegação. O alcance cai conforme a ameaça: tiers 1/2/3/4 usam 100/75/50/35% do raio. Um auditador gera sugestões para mobs novos sem substituir ajustes manuais. O Zombie Bruiser da horda e das cavernas regenera 1 ponto de vida a cada 3 segundos após 10 segundos sem sofrer dano.

Detalhes de instalação e teste: `docs/MILESTONE-SENSE-HEARING-BRUISER-2026-10-18.md`.

## Fila curta após a instalação de 18/10/2026

1. **Validar a instalação recente em jogo:** inicialização, Ore Sense I/II, Animal Sense, Threat Sense, Embedded Arrows, audição dos monstros e regeneração do Bruiser. Registrar defeitos observados antes de novos ajustes nessas mecânicas.
2. **Maintenance funcional preparada para instalação.** Os quatro nós agora concedem 10% cada (máximo 40%). O JAR compilado aplica a chance antes de Unbreaking. Após a instalação, testar ferramentas, armas, armaduras, Unbreaking, Mending, reparos e itens de mods.
3. **Depois: Sorte e Key Hunter.** Auditar todas as rolagens de loot antes de escolher se a sorte relativa de +30/60/90% convive com Key Hunter, substitui parte dele ou exclui chaves e blueprints.

**Nota sobre a skill existente:** Scavenger já concede +0,5 no atributo Luck por nó (até +2,5), além de +3% por nó na chance de um item extra elegível ao abrir loot gerado (até 15%). Isso não equivale ao aumento relativo proposto para todos os drops de monstros; a auditoria de Sorte e Key Hunter continua pendente.
4. **Em seguida: monstros e mundo.** Avaliar separadamente o filtro de criaturas para o tema zumbi, a quebra lenta de blocos e o Core. Cada etapa terá versão, backup e teste próprio.

### Quebra de blocos por zumbis — proposta para avaliação

- Todos os zumbis devem conseguir avançar quebrando blocos, porém lentamente. O tempo de quebra deve crescer com a resistência do bloco; materiais resistentes não podem ceder tão rápido quanto madeira ou terra.
- Zumbis equipados com picareta, inclusive os tipos que já existem no pack, devem quebrar em velocidade normal apropriada à ferramenta e ao material. A picareta não deve tornar instantânea a quebra de blocos resistentes.
- Antes da implementação, definir alcance, escolha do bloco, cooperação entre zumbis, persistência do progresso e regras para blocos protegidos/reforçados. Medir TPS e quantidade de tentativas simultâneas em uma horda e em cidades.
- Validar em cópia do mundo com madeira, pedra, minério, obsidiana e blocos reforçados; incluir zumbis comuns e com picareta. A aprovação dos valores e exceções fica para a etapa de design.

**Em standby — ecossistema das cavernas:** substituir monstros místicos que não são zumbis, inclusive esqueletos e demônios, por variantes zumbis dos mods já instalados. Levantar todos os spawns por bioma/profundidade, classificar força e função dos substitutos e revisar loot, quests, estruturas, elites e desempenho. Preservar variedade e progressão de perigo. Apresentar a matriz de substituições para avaliação antes de alterar as configurações ou remover mods; o escopo inicial é apenas cavernas.

**Em standby — mapas:** mapas regionais dropados por monstros e compartilhamento cartográfico. O item observado pelo jogador ainda não foi identificado; Map Atlases atualmente oferece mapas gratuitos e expansão sem peças. Retomar quando houver o item/ID ou uma regra concreta de geração regional, antes de alterar drops, receitas ou o Atlas.

## Atualização executada em 12/10/2026

As etapas autorizadas 1–7 foram implementadas. Prospector, Forager e Scavenger foram validados e marcados como concluídos. O loot nativo do Infernal Mobs foi mantido após auditoria de raridade; Divine Weaponry tornou-se loot exclusivo; a classificação central foi revisada; EnhancedAI deixou de alterar monstros comuns; a matriz de profundidade, equipamento por tier e a nova curva de XP foram instaladas. Fuel Efficiency/Treasure Sense, o componente raro de torretas e o Core permanecem pendentes por decisão. O relatório técnico está em `docs/STATUS-ROADMAP-2026-10-12.md`.

## Direção central

O perigo deve ser reconhecido pelo tipo de criatura, seu comportamento e o local em que aparece. Um zumbi comum deve continuar legível como ameaça comum; a dificuldade maior deve vir de criaturas visualmente diferentes, profundidade, composição dos encontros e eventos.

O risco também deve determinar a recompensa: áreas profundas e perigosas oferecem materiais melhores, equipamento, chaves e progressão, sem transformar inimigos comuns em esponjas de dano imprevisíveis.

## Estado encontrado na auditoria

- O registro central `kubejs/config/7dtc_mob_registry.json` já possui campos de atributos, faixa vertical, chance de spawn e nível de ameaça.
- Existem 224 criaturas registradas: 155 ainda estão no nível 0, incluindo criaturas perigosas de Alex's Caves que precisam ser classificadas antes do balanceamento vertical.
- Infernal Mobs promove somente uma parcela pequena e visualmente identificável das criaturas. Essa variação é desejada e será preservada; apenas seus drops serão integrados aos pools controlados do 7DTC.
- Wesley's Roguelike Dungeons já está com modificadores aleatórios desativados.
- A curva atual da árvore mantém a intenção antiga de aproximadamente três zumbis para o primeiro ponto: `min(18 + level * 6 + level ^ 1.7, 360)`.
- Field Dressing e Arrow Retrieval possuem implementação real.
- Prospector, Forager e Scavenger possuem efeitos reais no Skill Bridge e passaram nos testes iniciais de gameplay.
- As torretas já custam quantidades altas de materiais comuns. Os modelos avançados usam diamantes, redstone, obsidiana e granadas, mas ainda não dependem de um recurso raro específico da progressão subterrânea.
- Dynamic Lights Reforged está instalado. Ele melhora luz carregada por entidades, mas não aumenta de forma relevante o raio de uma tocha colocada.

## Fase 0 — mudanças lineares

### 0.1 Chave nível 1 de zumbi comum — concluído

- `minecraft:zombie` passa de 1,5% para 3% de chance base de uma Iron Level 1 Key.
- Outros inimigos comuns permanecem na chance atual.
- Key Hunter continua elevando a chance de nível 1 para 4% por enquanto. Esse ganho precisa ser revisto na fase de rebalanceamento, pois 3% → 4% é pouco relevante para uma milestone.

### 0.2 Feedback das habilidades — parcialmente concluído

- Field Dressing avisa no action bar quantos recursos adicionais foram gerados.
- Arrow Retrieval mantém quatro nós e recupera apenas flechas comuns de criaturas atingidas (9%, 18%, 27%, 35%).
- Prospector, Forager e Scavenger receberão avisos quando seus efeitos reais forem implementados.

### Critérios de teste

- Um zumbi comum nunca entrega chave acima do nível 1 por essa regra.
- A taxa observada em teste automatizado longo converge para aproximadamente 3%.
- O aviso aparece apenas quando o bônus realmente acontece.
- O aviso não ocupa o chat persistente.

## Fase 1 — fechar as habilidades básicas existentes — concluída e validada

### 1.1 Prospector

- Uma rolagem fixa por bloco elegível já foi implementada.
- Usar o bloco e a ferramenta corretos para evitar duplicação por Silk Touch, Fortune ou explosões.
- Exibir o minério e a quantidade recebida no action bar.

### 1.2 Forager

- A lista inicial contém arbustos silvestres de Sweet Berries.
- Arbustos colocados pelo jogador ficam gravados em dados persistentes do mundo e não são elegíveis.
- O bônus é informado no action bar. Outras fontes naturais só serão adicionadas quando puderem ser separadas com segurança de plantações.

### 1.3 Scavenger

- O bônus é rolado uma única vez quando o primeiro jogador realmente abre um recipiente de loot gerado.
- As duas metades de um baú duplo ficam marcadas para impedir novas tentativas.
- Baús trancados, baús nativos do WRD, artefatos, chaves, armas e itens com NBT são excluídos.

### Motivo da prioridade

Não é seguro tornar XP mais caro enquanto alguns nós ainda não fazem nada. Primeiro todos os nós precisam ser reais, testáveis e relevantes.

## Fase 2 — identidade e níveis de ameaça dos monstros

### Objetivo

Substituir variação extrema dentro da mesma espécie por variedade de criaturas reconhecíveis.

### Trabalho

1. Classificar todas as criaturas hostis no registro central:
   - nível 0: passivo, ambiental ou não classificado;
   - nível 1: ameaça comum;
   - nível 2: ameaça especializada;
   - nível 3: elite reconhecível;
   - nível 4: chefe ou ameaça de evento.
2. Corrigir os 155 registros ainda classificados como nível 0.
3. Preservar os modificadores do Infernal Mobs, pois a promoção é rara, visível e comunica que aquela criatura é especial.
4. Migrar as recompensas do Infernal Mobs para tabelas controladas por nível e quantidade de modificadores.
5. Impedir que EnhancedAI equipe silenciosamente monstros comuns. Miner Mobs, Fisher Mobs e Leap foram desativados; as funções das hordas continuam sendo equipadas pelo módulo próprio `HordeRoles`.
6. Definir limites de atributos por nível. Uma criatura individual não receberá multiplicadores aleatórios extremos.

### Proposta de identidade

- Nível 1: zumbis, esqueletos, aranhas e variantes comuns.
- Nível 2: runners, bruisers, criaturas subterrâneas especializadas e predadores fortes.
- Nível 3: mutantes, mimics e elites visualmente únicas.
- Nível 4: bosses, horde boss e criaturas de encontro especial.

### Fontes adicionais de criaturas

- `Realm RPG: Imps & Demons`: candidato direto, com foco em criaturas e sem necessidade de importar quests próprias.
- `Zombie Extreme`: candidato prioritário. Serão mantidas as entidades; estruturas, minérios, receitas, loot próprio e itens fora da progressão serão neutralizados. Seus drops passarão aos pools do 7DTC.
- `Nyf's Spiders`: identificado como o antigo comportamento avançado das aranhas. Deve ser testado sem as rotinas incompatíveis de IA e com limite específico de população de aranhas.
- `Born in Chaos`: possui muitas criaturas interessantes, mas também estruturas, blocos, receitas e sistemas próprios. Exige uma integração seletiva maior antes de entrar.
- `Improved Mobs` e `Mobs Properties Randomness`: não serão usados como fonte de dificuldade geral; ambos podem fortalecer/equipar criaturas sem identidade visual suficiente.

### Critérios de teste

- Dois zumbis comuns da mesma origem têm atributos próximos.
- O jogador reconhece uma ameaça nível 3 pela criatura, sem precisar observar sua barra de vida.
- Criaturas nível 4 não entram no spawn comum por acidente.

## Fase 3 — profundidade, perigo e recursos

### Faixas verticais iniciais

- Acima de Y 32: predominância de nível 1.
- Y 0 a 32: nível 1 com presença de nível 2.
- Y -32 a 0: predominância de nível 2 e pequena presença de nível 3.
- Abaixo de Y -32: nível 2 e nível 3, com nível 4 somente em estruturas, eventos ou condições explícitas.

### Nível ambiental

O ambiente também terá uma classificação de 1 a 4. Ela será calculada por profundidade, estrutura/dungeon, evento ativo e composição local de ameaças. O nível do ambiente controla o teto dos monstros e o tamanho máximo das veias, sem alterar silenciosamente a força de uma criatura comum.

- Ambiente 1: superfície e cavernas rasas; veias pequenas e ameaças comuns.
- Ambiente 2: cavernas médias e estruturas simples; veias moderadas e especialistas.
- Ambiente 3: cavernas profundas ou dungeons perigosas; veias maiores, elites e loot melhor.
- Ambiente 4: arenas, bosses, hordas avançadas e estruturas finais; recompensas excepcionais.

### Implementação

- Estender o registro central com pesos por faixa vertical. Os campos atuais `minY`, `maxY` e `chance` permitem restringir, mas não formam sozinhos uma composição completa de encontros.
- Controlar adições/substituições por um módulo Forge ou regras geradas para InControl.
- Evitar que criaturas exclusivas dos biomas de Alex's Caves sejam removidas do próprio contexto.
- Criar uma matriz de recursos por profundidade.

### Recursos

- Ferro e outros recursos continuam com disponibilidade regular. Veias comuns permanecem pequenas; ambientes 2 a 4 podem gerar depósitos progressivamente maiores.
- Materiais raros de torretas e equipamento avançado ficam vinculados às faixas mais perigosas.
- Aumento de recompensa deve vir de geração e loot controlados, sem multiplicar Fortune silenciosamente.

### Critérios de teste

- Uma sessão de mineração inicial ainda encontra ferro suficiente para ferramentas essenciais.
- Abaixo de Y -32, o ganho esperado por minuto é maior, mas o risco e o consumo de recursos também são maiores.
- Mundos já gerados mantêm seus minérios; mudanças de geração valem para chunks novos.

## Fase 4 — armas e conjuntos como loot

### Equipamento e Divine Weaponry

**Implementado em 12/10/2026:** drops de armas Divine Weaponry por nível de ameaça, com uma única seleção por morte e lendárias exclusivas de bosses/Infernal Mobs. Durabilidade variável, armaduras e o pool separado de armas de fogo continuam nesta fase.

- Manter ferramentas de trabalho craftáveis.
- Manter armas primitivas necessárias ao início do jogo.
- Manter as armaduras vanilla craftáveis, incluindo diamante.
- Manter armas de fogo craftáveis na CGM Workbench e fora do loot de criaturas comuns.
- Zumbi/esqueleto comum: uma rolagem de 8% no pool completo de armas de pedra Divine Weaponry e uma rolagem de 1% no pool completo de ferro; 0% para diamante.
- Criatura mais forte/profunda: uma rolagem de 8% no pool completo de ferro e uma rolagem de 1% no pool completo de diamante.
- A porcentagem pertence ao pool: sucesso escolhe uma arma, em vez de aplicar a chance individualmente a cada arma.
- Armas lendárias Divine Weaponry: bosses e Infernal Mobs, com chance proporcional à quantidade/força dos modificadores.
- Armas de fogo só podem aparecer em bosses ou Infernal Mobs fortes, com chance baixa e pool separado.
- Distribuir armaduras, espadas, arcos e equipamento avançado por nível de ameaça, baús trancados, dungeons e eventos.
- Equipamentos dropados chegam com durabilidade variável. Encantamentos e equipamento completo são progressivamente mais raros.
- Recycler e kits de reparo ganham importância na manutenção dos itens encontrados.

### Dependências

- Classificação completa de monstros.
- Neutralização das recompensas não controladas do Infernal Mobs sem remover sua mecânica de promoção.
- Inventário dos tiers reais de Divine Weaponry e de todos os itens próprios dos mods importados.
- Revisão das quests que ainda exigem fabricar equipamento.

## Fase 5 — XP e relevância dos nós

### Ordem correta

1. Concluir todos os efeitos das habilidades.
2. Medir tempo para obter pontos em combate, mineração, pesca e crafting.
3. Aumentar o custo de nível.
4. Reavaliar cada nó cujo ganho seja pequeno demais para o novo tempo de aquisição.

### Proposta de teste

- Primeiro ponto: aproximadamente 4 a 5 zumbis comuns.
- Meio da árvore: um ponto deve exigir uma atividade deliberada, sem grind excessivo.
- Fim da árvore: pontos continuam possíveis durante gameplay normal, mas deixam de ocorrer em sequência rápida.

### Habilidades que já merecem revisão

- Key Hunter: 4% com base de 3% é pouco ganho.
- Conditioning e Fast Hands: valores por nó podem ficar discretos demais com XP mais caro.
- Habilidades de utilidade binária permanecem relevantes mesmo com custo maior.

## Fase 6 — novas habilidades

### 6.1 Fuel Efficiency — proposta recomendada

- Galho: Engineering.
- Tipo: habilidade básica.
- Quatro nós de +5% de duração do combustível, máximo de +20%.
- Posição: rota de Mechanical Foundations/Create, substituindo quatro das oito ocorrências de Workshop Efficiency.
- Aplicação: fornalha comum, alto-forno e defumador. Integrações de máquinas devem ser avaliadas separadamente.

### 6.2 Iluminação aprimorada por tochas

O Minecraft limita luz a 15. Mudar a tocha de 14 para 15 quase não altera seu raio. Para uma diferença perceptível existem três opções:

1. colocar emissores de luz invisíveis ao redor da tocha e removê-los quando ela quebrar;
2. desbloquear uma tocha própria com lógica de raio ampliado;
3. melhorar apenas a luz dinâmica da tocha na mão.

Recomendação: item próprio desbloqueado por milestone, porque evita alterar milhares de tochas já colocadas e permite controlar desempenho e remoção dos emissores.

### 6.3 Localização de baús e perigos

A detecção será passiva e limitada a 24 blocos. Em intervalos controlados, a habilidade procura:

- um baú de nível 2 ou maior ainda relevante;
- uma criatura de ameaça 3 ou 4;
- qualquer Infernal Mob.

Ao entrar na área, mostra `Um monstro ou recompensa de valor estão próximos` e toca um som curto de mistério. O mesmo alvo não repete o aviso continuamente; uma trava por alvo e um cooldown evitam spam e custo excessivo.

## Fase 7 — torretas e materiais raros

### Estado atual

- Basic Turret: dispenser, 24 ferros, 16 redstones e 12 cobres.
- Advanced Turret: Basic Turret, observers, diamantes e blocos de redstone.
- Shotgun Turret: Basic Turret, ferro, tripwire hooks e redstone.
- Explosive Turret: Advanced Turret, obsidiana, granadas e diamantes.

### Proposta

- Criar um componente raro próprio comum a todas as torretas: `Turret Control Core`.
- O componente terá textura, nome, descrição e integração JEI próprios; nenhum placeholder ou textura ausente será aceito.
- Control Core básico: recompensa de ameaças nível 2, dungeons e baús nível 2.
- Versões avançadas: materiais das profundezas e recompensas nível 3/4.
- A milestone continua necessária; possuir o material raro não ignora a skill.
- Garantir pelo menos uma fonte renovável, ainda que difícil, para servidores longos.

## Fase 8 — Core da campanha

### MVP recomendado

1. A última etapa de `Welcome, Survivor` exige construir ou interagir com o Core e explica que protegê-lo é o objetivo primário da campanha.
2. Existe somente um Core compartilhado por mundo. Cada jogador precisa se associar a ele para concluir a introdução.
3. O Core registra proprietário, equipe, posição e vida persistente.
4. Durante hordas, monstros recebem o Core como objetivo prioritário. Monstros comuns também podem atacá-lo quando o enxergam ou chegam muito perto, sem atravessar longas distâncias do mundo procurando-o.
5. A HUD mostra vida e distância do Core durante a horda.
6. A destruição grava um estado persistente de derrota, encerra a horda, mostra `GAME OVER` no centro da tela e coloca todos os jogadores em espectador.
7. Um comando administrativo permite recuperar o mundo durante testes.
8. O Core não pode ser movido nesta primeira versão.
9. O texto inicial informa explicitamente sobre as hordas a cada sete dias e que a destruição do Core encerra o jogo.

### Expansão do Core

Adiar até o MVP estar estável. Espalhar blocos automaticamente pode destruir construções, bloquear caminhos, aumentar custo de chunks e deixar a defesa menos legível. Caso seja testado, a expansão deve ocorrer somente dentro de uma área reservada e com limite rígido.

Também ficam reservados para uma fase posterior itens obtidos de bosses e entidades especiais que aumentem vida, defesa ou outras propriedades do Core.

## Estudo separado — ampliar a profundidade do mundo

É tecnicamente possível usar `min_y = -128`, pois o tipo da dimensão e as alturas precisam respeitar múltiplos de 16. A mudança exige também adaptar noise settings, carvers, aquíferos, minérios, estruturas e regras de spawn; alterar apenas o limite criaria camadas sem conteúdo coerente.

O teste deve ocorrer em uma ramificação de worldgen e somente em mundos novos. Alex's Caves e mods de estruturas precisam ser validados individualmente, e a profundidade extra aumenta tempo de geração, tamanho do mundo e custo de exploração. Nenhuma alteração de profundidade será aplicada nesta etapa.

## Fase 9 — rastreador de bosses e objetivos

### Proposta

- Criar o item `Threat Locator`.
- Manter um registro central de bosses e encontros: Alex's Caves, Alex's Mobs, bosses de horda e encontros de dungeon.
- Auditoria do Alex's Mobs concluída em `docs/AUDITORIA-ALEXS-MOBS-CAVERNAS.md`: Rocky Roller restaurado como encontro subterrâneo controlado e criaturas especiais comparadas com o modpack antigo.
- O item não revela bosses nunca descobertos globalmente.
- Ao encontrar uma pista, estrutura ou iniciar uma quest, o local é registrado no item e mostrado no mapa/bússola.
- O marcador desaparece quando o boss correspondente morre ou a quest é concluída.
- Integrar com Cartography e, para ameaças, com Threat Sense.

### Decisões necessárias

- O item aponta para o boss mais próximo ou permite selecionar um objetivo conhecido?
- O marcador deve aparecer no Xaero, Advanced Compass, ou nos dois?
- O item é permanente, usa durabilidade ou consome um recurso por busca?

## Ordem de execução recomendada

1. Concluir Prospector, Forager e Scavenger, com feedback.
2. Classificar todos os monstros, preservar Infernal Mobs e bloquear fortalecimento/equipamento silencioso de criaturas comuns.
3. Implementar faixas de profundidade e matriz de recursos.
4. Migrar armas/armaduras para loot controlado.
5. Rebalancear XP e a relevância de cada nó.
6. Adicionar Fuel Efficiency e decidir a solução de iluminação.
7. Criar materiais raros e revisar receitas de torretas.
8. Construir o Core MVP e integrar a IA das hordas.
9. Criar Treasure Sense e Threat Locator.
10. Avaliar expansão do Core somente após testes de campanha.

