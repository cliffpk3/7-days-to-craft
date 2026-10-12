# Registro central de mobs — 7 Days to Craft

O arquivo `7dtc_mob_registry.json` concentra os ajustes gerais dos mobs do modpack. Ele contém Minecraft, Alex's Mobs, Alex's Caves e as demais criaturas atualmente instaladas.

## Como editar

Procure o identificador do mob dentro de `mobs`. Exemplo: `minecraft:zombie`.

```json
"minecraft:zombie": {
  "attributes": {
    "maxHealth": 24.0,
    "attackDamage": 4.0,
    "armor": null,
    "armorToughness": null,
    "movementSpeed": null,
    "followRange": null,
    "knockbackResistance": null
  },
  "spawn": {
    "allow": true,
    "minY": -30,
    "maxY": 30,
    "dimensions": ["minecraft:overworld"],
    "biomes": [],
    "chance": 1.0
  }
}
```

- `null` mantém o atributo original do mod.
- `allow: false` bloqueia novos spawns avaliados pelo Forge.
- `minY` e `maxY` limitam a faixa vertical.
- `dimensions` e `biomes` aceitam IDs completos. Lista vazia não restringe.
- `chance` varia de `0.0` a `1.0` e somente reduz os spawns que já aconteceriam.
- `/reload` relê o arquivo durante um mundo aberto.

## O que permanece na configuração original do mod

Comportamentos especiais que não são atributos de uma criatura continuam nos arquivos próprios. Para Alex's Caves isso inclui geração das cavernas, explosão nuclear, chefes que quebram blocos e máquinas, em `config/alexscaves-general.toml` e `config/alexscaves_biome_generation/`.

Essa separação evita transformar mecânicas especiais em números genéricos incorretos. O registro central controla HP, dano, defesa, velocidade, alcance, resistência e filtros de spawn.

## Validação sem abrir o Minecraft

Execute no PowerShell, na raiz do modpack:

```powershell
powershell -ExecutionPolicy Bypass -File tools/validate-mob-registry.ps1
```

## Matriz de profundidade

`7dtc_depth_matrix.json` concentra as faixas ambientais e os encontros adicionais. Depois de editar `encounters`, gere a configuração ativa do InControl:

```powershell
powershell -ExecutionPolicy Bypass -File tools/rebuild-depth-spawns.ps1
```

A classificação completa gerada está em `docs/CLASSIFICACAO-DE-AMEACAS.md`. Nível 0 é usado de forma intencional para fauna passiva, criaturas neutras, partes de entidades e utilidades.
