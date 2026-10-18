# Auditoria de lateralidade — 18/10/2026

## Escopo e evidência

Lote recente de habilidades, cliente/servidor integrado, dependências locais e publicação do modpack. Conferência estática dos arquivos e observação de `logs/latest.log` durante a abertura de 18/10. Testes probabilísticos em gameplay continuam necessários.

## Verificado

- A publicação em `origin/main` passou após retirar staging e backups do commit. O publicador agora bloqueia esses diretórios e padrões de ID de acesso AWS antes de criar novos commits. Os arquivos locais foram preservados.
- O erro de mixin da Maintenance não reapareceu. O JAR instalado de Skill Bridge e o de Locks têm o mesmo SHA-256 dos builds locais. O servidor integrado iniciou e abriu um mundo; não houve falha fatal, erro de mixin ou erro de fonte na nova tentativa.
- Puffish: 149 nós apontam para definições existentes, todas as conexões apontam para nós existentes e todos os nós são alcançáveis a partir da raiz. O Fire Swallower usa o ícone `minecraft:fire_charge` e mantém o ID interno antigo para preservar o vínculo de saves.
- As três tarefas de treino do Fire Swallower apontam para avanços com o critério `completed` presentes no JAR instalado. Os 29 provedores da fonte de ícones têm suas texturas no pacote. O item `alexsmobs:roadrunner_boots` usado em Reserve Jump existe no JAR instalado.
- Código/texto: Soft Landing usa 6% por nó para negar o dano de queda; Maintenance usa 10% por nó (máximo 40%); Tool Proficiency usa 10% por nó com ferramenta correta; Enduring Vitality cura 4 HP depois de 20 s sem dano e a cada 45 s; Arrow Retrieval usa 7/14/21/28% e Embedded Arrows usa rolagem separada de 30% para flechas comuns.
- Dependências obrigatórias passaram pela carga do Forge. Os módulos TaCZ, CGM, Puffish, FTB Quests e Simple Enemy apareceram na inicialização sem erro de dependência ausente. O build usa Forge 47.1.3 e o jogo Forge 47.4.10; a faixa declarada aceita ambos e a execução carregou, mas alinhar o build ao runtime será uma manutenção de desenvolvimento futura.

## Pendências e riscos observados

- Fire Swallower ainda precisa de teste real: consumir uma tocha ao agachar e clicar no ar, acumular 10 s por uso até 60 s, não consumir ao mirar em bloco e concluir as três tarefas. A implementação preserva o ID da antiga Dark Adaptation; uma quest já concluída em save antigo não reinicia automaticamente.
- Maintenance, Tool Proficiency, Soft Landing, Enduring Vitality e Arrow Retrieval precisam de amostras em jogo. A compilação e a abertura do mundo comprovam carregamento, não os percentuais observados.
- O log registrou 59 mensagens de `setBlock in a far chunk`, todas ligadas à geração de `minecraft:large_dripstone`. Não houve travamento associado nesta abertura. Observar tempo de geração e TPS em cavernas novas antes de ajustar worldgen.
- Há avisos de modelos/sons ausentes em Born in Chaos, WRD e outros mods; não foram fatais. Investigar somente se aparecer item ou criatura visualmente quebrado.
- O log do cliente mostrou falha 401 ao consultar a autenticação/Realms, mas o servidor integrado abriu. Se houver falha de conexão com outros jogadores, verificar autenticação e o log da tentativa de conexão.

## Próxima etapa do roadmap

1. Executar o lote curto de testes de habilidades em mundo de teste ou cópia do save, conforme `docs/TESTES-AO-VOLTAR-2026-10-18.md`.
2. Em seguida, fechar o design de reforço de blocos da Engineering antes de codificar: ferramenta, materiais consumidos, blocos elegíveis, limite por jogador/base, persistência e como o bônus de 50/100% afeta quebra por jogadores e zumbis. A troca de Advanced Manufacturing exige preservar a rota Create Automation, a quest e o reembolso de pontos da milestone duplicada.
3. A substituição dos monstros místicos das cavernas por variantes zumbis e o sistema de mapas continuam em standby. A regra de quebra lenta por zumbis vem após o design de reforço; o Core individual permanece posterior.
