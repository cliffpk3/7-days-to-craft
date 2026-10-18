# Carne dos animais do Alex's Mobs

Auditoria das tabelas de entidade do `alexsmobs-1.22.9.jar`. Quantidades abaixo são
sem Looting; Looting e cozimento por fogo continuam seguindo as funções originais.
As substituições ficam em `kubejs/data/alexsmobs/loot_tables/entities/` e entram
em vigor após `/reload` no mundo, inclusive em jogo aberto.

| Animal | Item | Original | 7DTC | Situação |
| --- | --- | ---: | ---: | --- |
| Gazela | mutton | 0–1 | 2–3 | Ajustado: porte médio |
| Canguru | kangaroo_meat | 1–2 | 2–3 | Ajustado: porte médio |
| Tusklin | porkchop | 3–6 | 4–6 | Ajustado: porte grande |
| Urso-pardo | beef | 0 | 4–5 | Ajuste anterior preservado |
| Alce | moose_ribs | 1–3 | 4–5 | Ajuste anterior preservado |
| Bisão | beef | 6–8 | 6–8 | Original já proporcional |
| Peixe-gato médio | raw_catfish | 2–3 | 2–3 | Original já proporcional |
| Peixe-gato grande | raw_catfish | 4–6 | 4–6 | Original já proporcional |

As demais entidades grandes auditadas não fornecem carne no mod original:
elefante, rinoceronte, gorila, tigre, leopardo-das-neves, crocodilo, jacaré,
dragão-de-komodo, tubarões, orca e cachalote. Não receberam carne genérica nesta
etapa. Isso preserva a identidade dos itens do mod e evita transformar animais
raros ou espécies sem alimento próprio em fontes desproporcionais de `beef`.
Se o design decidir torná-los alimento, a espécie e o item devem ser definidos
explicitamente antes de adicionar a tabela.

Não há LootJS instalado. Migrar estas regras de JSON para scripts exigiria uma
dependência adicional sem trazer recarregamento melhor: `kubejs/data` já funciona
como datapack e aceita `/reload`. Os arquivos substitutos preservam os pools
originais de pele, chifre, ovo e demais materiais.

Validação em jogo: executar `/reload` e testar uma gazela, um canguru e um
Tusklin (sem Looting), conferindo as faixas acima e os materiais secundários.
