# Milestone: sentidos, audição e Bruiser (18/10/2026)

## Mudanças

- Scavenging: o nó inicial Field Dressing vira Forager; os quatro nós posteriores viram Animal Sense (9/18/27/33 blocos), visível no Advanced Compass apenas para animais passivos que fornecem comida.
- Ranger: os quatro nós Arrow Retrieval passam a 7/14/21/28% para flechas comuns ou munição CGM disparada por armas TaCZ selecionadas ao acertar criaturas. A milestone superior Embedded Arrows mantém tentativa independente de 30% para recuperar uma flecha comum que acertou um monstro, ao morrer. Uma mesma flecha não é entregue duas vezes.
- Combate inferior: Threat Sense I detecta monstros a 10 blocos; Threat Sense II amplia para 30.
- Ore Sense I conserva 20 blocos e luz 8/15. O antigo nó Cartography vira Ore Sense II (custo 3), que remove o limite para minérios em chunks carregados. A atualização da iluminação já existente é gradual (12 seções por tick) para evitar um pico de processamento. O Atlas deixa de exigir Cartography; a quest antiga de Cartography foi retirada.
- Todos os tooltips de skills pagas exibem `Requires N skill points` em amarelo antes da descrição.
- Monstros hostis com navegação investigam disparos e gritos fora de combate. O raio para níveis de ameaça 1/2/3/4 é 100/75/50/35% do valor configurado. Ataques diretos da milícia iniciam retaliação quando não há alvo atual.
- Zombie Bruiser já faz parte da horda desde a segunda horda. Todos os Bruisers agora curam 1 HP a cada 3 segundos, somente depois de 10 segundos sem receber dano.
- `/7dtc_mob_audit` exporta atributos e pistas de comportamento de mobs hostis carregados. `python scripts/update_mob_registry.py` gera sugestões; `--apply` adiciona apenas IDs ausentes após salvar `.pre-audit`. A classificação automática exige revisão para habilidades especiais e elites.

## Estado e instalação

Os JARs compilados estão em `staging/sense-hearing-bruiser-v1-20261018/`. Os JARs anteriores, Xaero Minimap e os JSONs anteriores da árvore estão em `backups/sense-hearing-bruiser-v1-20261018/`. Depois que o Minecraft foi fechado, os dois JARs foram instalados e verificados por SHA-256; o Xaero Minimap foi movido para o backup com extensão `.disabled`. Os JSONs da árvore e quests já estão no perfil. As mudanças passam a valer no próximo início do jogo.

Um `/reload` isolado não ativa as mudanças nos JARs. Ainda falta validar o carregamento e o comportamento dentro do jogo.

## Teste dentro do jogo

1. Abrir a árvore e conferir Forager inicial, quatro Animal Sense, Ore Sense I/II, Threat Sense I/II, Embedded Arrows e o custo amarelo de nós de 1/2/3/4 pontos.
2. Com Ore Sense I, verificar um minério a 5 e 25 blocos. Com II, esperar alguns segundos pela atualização inicial e verificar um minério distante em chunk carregado. Medir FPS antes e depois.
3. Atirar com TaCZ perto de um Zombie Pharaoh sem aggro; confirmar investigação. Acertá-lo com Simple Enemy; confirmar retaliação. Repetir com monstro já em combate; o alvo não deve mudar só pelo som.
4. Testar Bruiser da caverna e da horda: após dano, não deve regenerar por 10 segundos; em seguida recupera 1 HP a cada 3 segundos. Confirmar que ele continua na seleção da horda 2+.
5. Testar Atlas sem skill, ausência do Xaero Minimap e quest antiga de Cartography; avaliar se mapa/atalhos restantes funcionam.
6. Rodar `/7dtc_mob_audit` como operador e revisar `staging/mob-audit-runtime.json`. Gerar sugestões antes de aplicar ao registro central.
