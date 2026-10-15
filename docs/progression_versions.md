# Progression versions (2026-10-15)

- `progression-v0-20261015`: repository baseline before progression changes. A copy of the original mod JAR and affected quest files is in `C:/Users/carli/Documents/Codex/2026-09-20/e/backups/progression-v0-20261015`.
- `progression-v1-blueprints`: each of the 9 CGM firearms and 3 Alex's Mobs firearms consumes its own blueprint in the Workbench. `config/7dtc_gun_blueprints.json` controls one-drop-per-kill odds and pools by threat tier. Blueprints use the Written Book icon. Existing firearm drops from rare mobs are unchanged.
- `progression-v2-quests`: onboarding Workbench interaction and split skill training objectives.
- `progression-v3-iron-distance`: spawn-distance based iron vein sizes, configurable per worldgen band.

The original world spawn is the proposed distance anchor for V3. Generated chunks keep their old ores. Later difficulty escalation after the first horde is a planning item only.

## V3: iron by world-spawn distance

`config/7dtc_iron_distance.json` controls the three radius limits and four multipliers. Default bands: 0-512 blocks = 50%, 513-1024 = 65%, 1025-1536 = 80%, farther = 100%. The calculation uses horizontal distance from the Overworld's shared spawn, not player bed position. The worldgen feature applies stochastic rounding to the original vein sizes (9, 4, and 7), so the percentage is an expected size rather than a hard exact block count. Ore placement attempts and height ranges remain unchanged. A server restart is required to reload the configuration; only new chunks receive new generation.

This covers the pack's configured iron features. Minecraft's separate, rare noise-driven giant ore veins are outside these configured features and should be checked during field testing before claiming a universal maximum for all iron deposits.

## Next design review

- Provide a second bounded source for gun blueprints and a useful conversion for duplicates, so unlucky players can still progress.
- Test time to first firearm and iron acquired before hordes 1 and 2 in a new world.
- Discuss world danger milestones after horde 1. A flat 50% increase to every mob is only an idea at this stage; no change has been applied.

- `progression-v3.1-quest-interaction`: onboarding CGM Workbench objective completes only on crafting or interacting with the block. Merely possessing a Workbench no longer completes it.
