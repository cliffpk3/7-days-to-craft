# 7DTC: auditoria e roteiro apos cidade/TaCZ

## Estado confirmado por arquivos

- Cidade: perfil `7dtc_city`, tres pools de carros substituidos por elementos vazios, edificios selecionados, posto fixo com milicia na peca inicial de cidades novas e delegacia opcional com soldados `usunit` inclusive no terraco.
- Entrada urbana: uma emboscada de 2 a 4 zumbis por jogador/cidade, com registro persistente e ao menos um Runner. Runners urbanos recebem +20% de velocidade base.
- Combate ambiental urbano: ate dois encontros sorteados por jogador/cidade em setores de 64 blocos; um soldado de esquadra surge 34-52 blocos distante e sem visao direta do jogador. O encontro exige ao menos dois zumbis visiveis para ele, gerando os ausentes perto do soldado. A IA nativa decide os alvos, inclusive jogadores que se aproximarem.
- Cidade: patrulhas de zumbis com teto local de 26 entidades; spawners de edificios nao entram nesse teto.
- Armas: 18 TaCZ e 11 acessorios na CGM Workbench; armas selecionadas exigem blueprint. Receitas padrao TaCZ ocultas por filtro.
- Mundo: 14 conjuntos de estruturas de Born in Chaos agora evitam iniciar em ate 16 chunks do inicio de `the_lost_city:city`.
- TaCZ: removida a injecao de uma Taurus 943 com munição TaCZ no bau bonus inicial, que escapava da lista selecionada.

## Pendencias e riscos a validar no jogo

1. **Inicializacao apos a instalacao TaCZ:** a sessao de 17/10 as 18:43 carregou o menu, mas caiu ao abrir a lista de mundos por falta do campo `processors` nos seis pools de spawners da cidade. Campo corrigido no gerador e nos arquivos; ainda requer nova abertura de mundo e teste de craft/consumo de blueprint, recarga e devolucao das tres municoes CGM, acessorios e comportamento dos soldados.
2. **Geracao de cidade nova:** o ultimo log disponivel registra pools subterraneos `the_lost_city:city_under*` vazios/nao encontrados e uma procura por `spawner_husk.nbt`. Os recursos aparecem no JAR, entao verificar a causa em uma nova geracao antes de alterar os pools.
3. **Alcance da exclusao:** a regra de estrutura considera centros de geracao, nao caixas completas, e nao modifica chunks ja gerados. Verificar cidade nova e suas bordas; outros mods sem estrutura set compativel podem requerer integracao propria.
4. **Deteccao da cidade:** `CityRaid.cityAt` usa a peca da estrutura. Checar ruas, terracos e subsolo; fora das pecas detectadas, patrulha e filtro de spawns podem nao atuar.
5. **Carga e loot:** spawners de edificios nao estao no teto das patrulhas. Milicianos tem uma rolagem propria de 3,5% para blueprint alem da rolagem generica por tier; chefes urbanos tambem tem 4% extra. Medir kills/hora e blueprints/hora antes de ajustar.
6. **Ore Sense:** a implementacao possui limite posicional de 20 blocos no motor de luz, mas tambem retorna emissao global para blocos de minerio quando a skill esta ativa. A observacao do usuario de brilho alem de 20 blocos e coerente com essa segunda via; requer correcao e teste visual.
7. **Armas antigas:** CGM continua instalado para a bancada, munição e torres. Fazer teste de receita, loot, comercio e JEI para confirmar que armas CGM antigas nao oferecem caminhos alternativos.
8. **Milicia por cidade:** confirmar em cidade nova que o posto fixo da peca inicial aparece sem colidir com as tres ruas restantes e que os soldados no terraco conseguem atingir a rua. A delegacia adicional continua sujeita ao sorteio.
9. **Emboscada de entrada:** entrar, sair, morrer e reconectar na mesma cidade com dois jogadores; conferir uma emboscada por jogador/cidade, 2 a 4 zumbis perseguindo e velocidade do Runner. Testar outra cidade para confirmar nova emboscada.
10. **Combate ambiental:** percorrer setores de 64 blocos e confirmar que o soldado aparece fora da visao direta, com ao menos dois zumbis nas proximidades, enfrenta zumbis usando sua IA nativa, faz ruido perceptivel e pode mirar no jogador quando ele se aproxima. Medir quantidade de encontros, TPS e milicianos ativos com dois ou mais jogadores.
11. **Carros em cidades existentes:** a substituicao dos pools jigsaw so vale ao gerar novas estruturas. Carros ja colocados no save permanecem.

## Roteiro de propostas para avaliacao com o usuario

Cada etapa abaixo e independente, com versao/milestone, teste e rollback. Nenhuma das propostas restantes foi aprovada para implementacao automatica.

| Ordem | Proposta | Porte | Criterio de validacao |
| --- | --- | --- | --- |
| 1 | Corrigir limite real de 20 blocos do Ore Sense | pequeno | Minerios a 19/20/21 blocos, com e sem shaders, em cavernas novas e existentes |
| 2 | Recompensa da horda para cada jogador elegivel conectado | pequeno/medio | Dois jogadores, incluindo quem esta distante ou morto, recebem exatamente a regra aprovada |
| 3 | Flechas recuperaveis em 10/20/30% | medio | Recuperacao apenas da flecha do dono; sem duplicar flechas infinitas/especiais |
| 4 | Mais mobs que puxam/reposicionam durante a horda | medio | Nao prende player em blocos, nao substitui toda a composicao da horda |
| 5 | Visao de animais no Advanced Compass em 20/30/40 blocos | medio | Icones so com a skill e apenas dentro de cada alcance |
| 6 | Threat Sense no Advanced Compass em 10/20/30 blocos | medio | Sem revelar mobs por paredes, abaixo da terra ou fora do raio conforme regra acordada |
| 7 | Durabilidade maior para armas e equipamentos via skill | medio | Desgaste e reparo coerentes com encantamentos e itens de mods |
| 8 | Revisar/remover vilarejos e recursos excessivos | medio/alto | Valor por vilarejo proporcional ao risco, sem afetar cidades abandonadas |
| 9 | Sorte de drops +30/60/90% relativos e interacao com Key Hunter | alto | Auditoria de todas as tabelas, chaves, elites, blueprints e limite de 100% |
| 10 | Filtrar monstros para zumbis/derivados/humanos armados | alto | Revisao de cavernas, superficie, horda, quests, loot e bosses antes de remover mods |
| 11 | Fragmentos de mapa dos mobs locais e compartilhamento entre jogadores | alto | Descoberta progressiva, sincronizacao e protecao contra exploracao por farm |
| 12 | Quebra lenta de blocos por todos os monstros; picareta acelera | muito alto | Regras de protecao de base, griefing, performance e estrategia de horda |
| 13 | Core individual, foco da horda, eliminacao/revivificacao e derrota coletiva | muito alto | Prototipo separado com persistencia, multiplayer, desconexao, recuperacao e upgrades |

Antes de cada etapa: confirmar design e numeros, criar milestone, testar em mundo descartavel e em save de copia, e so entao distribuir.

## Ideias de audio e infeccao da milicia para avaliar

- Som de morte condicionado: o Simple Enemy ja registra dois sons de morte para `usunit`, mas os usa em qualquer morte. Um evento Forge de morte pode identificar quando um zumbi matou um miliciano e tocar uma variante de grito com volume e alcance limitados. O audio especifico precisa ser escolhido ou criado; o som por si so nao atrai zumbis sem uma regra de audicao adicional.
- Conversao: implementada para milicianos `usunit` mortos diretamente por zumbis dentro das cidades, gerando `zombie_extreme:infected_military` com limite local de quatro convertidos e trinta zumbis. O efeito de infeccao do Zombie Extreme ja mata e cria um `zombie_extreme:infected` quando expira; essa via nativa nao e duplicada. Validar em copia do save, inclusive mortes internas e no terraco.
