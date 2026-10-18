# Milestone Engineering, Scavenging e Combat — 18/10/2026

## Instalado; inicialização validada em 18/10/2026

- Maintenance: quatro nós de 10%, máximo 40%. A chance é aplicada a cada dano de durabilidade causado por um jogador, antes de Unbreaking. Sem varredura por tick.
- Tool Proficiency: seis nós de 10% para a velocidade de quebra com a ferramenta correta, máximo 60%.
- Enduring Vitality: após 20 segundos sem dano, restaura 4 pontos de vida (2 corações) a cada 45 segundos.
- Arrow Retrieval: quatro nós de +7%, máximo 28%. Flechas comuns podem ser recuperadas após atingir criaturas; munição CGM usada pelas armas TaCZ selecionadas retorna ao inventário quando um projétil acerta uma criatura. Tiros que erram não geram reembolso.
- Embedded Arrows mantém a chance independente de 30% para flechas comuns cravadas, com nome e ícone de alvo distintos do nó básico.
- O ramo de Ore Sense II recebe ligação das pontas de Prospector e Forager; a ligação direta de Ore Sense I para II foi retirada.
- Scavenger usa um trevo próprio de 16×16 com cores e contorno próximos dos ícones da árvore. O item do Nameless Trinkets não é alterado.

O JAR instalado em `mods/seven-days-skill-bridge-1.0.1.jar` corresponde ao build local. O erro de mixin da Maintenance foi corrigido e a inicialização passou desse ponto. Os backups anteriores continuam em `backups/engineering-scavenging-v1-20261018/`. Os efeitos ainda precisam dos testes de gameplay abaixo.

## Validação em jogo após instalação

1. Conferir o desenho e as conexões da árvore: Prospector/Forager chegando a Ore Sense II, trevo de Scavenger e alvo de Embedded Arrows.
2. Comparar a durabilidade perdida em uma amostra de usos com Maintenance 0 e 4, inclusive armadura, ferramenta, arma, Unbreaking e item de mod.
3. Comparar a velocidade de mineração com Tool Proficiency 0 e 6 usando a ferramenta correta e uma incorreta.
4. Acertar criaturas com flechas e com armas TaCZ que usam Basic Bullet, Advanced Bullet e Shell. Conferir reembolso raro no inventário e ausência de reembolso em tiros que erram; um disparo de shotgun não deve render vários cartuchos por causa dos pellets.
5. Sofrer dano, esperar 20 segundos, medir a recuperação de 2 corações a cada 45 segundos de Enduring Vitality.

## Engenharia estrutural em avaliação

Structural Engineering hoje só concede a tag `7dtc_fortification_4`, sem efeito próprio. Advanced Manufacturing concede `7dtc_automation` e participa do avanço/quest correspondente. Embora a busca não tenha encontrado bloqueio de receita atrelado a essa tag, ela também controla o reembolso de pontos das duas cópias da milestone compartilhada. Substituir os dois por níveis de reforço exige migrar esse avanço e revisar a rota de Create Automation e o reembolso.
