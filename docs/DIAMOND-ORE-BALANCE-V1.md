# Diamond ore balance V1

Minecraft 1.20.1's three diamond placed features remain at their original heights and use their original configured vein sizes and air-exposure rules. This version changes only their placement frequency:

| Feature | Vanilla attempts per chunk | 7DTC V1 attempts per chunk |
| --- | ---: | ---: |
| Small diamond | 7 | 5 |
| Buried diamond | 4 | 2 |
| Large diamond | 1 per 9 chunks | 1 per 12 chunks |

The combined nominal placement frequency falls from 11.11 to 7.08 attempts per chunk (36% less), before height selection and exposure rejection. At Y -54, its theoretical attempt density falls from about 0.122 to 0.077 per chunk per Y level. Iron's small and deep-risk features together have about 0.119 attempts per chunk per Y level there. Attempt density is not a guaranteed block count or a field-measured encounter rate.

The existing iron distance scaler remains 50% within 512 horizontal blocks of the world's original spawn, 65% to 1024 blocks, 80% to 1536 blocks, and 100% beyond. It scales vein size only; it does not change attempt counts. Existing chunks retain their ores. New generation uses this datapack after reload/restart.

Rollback: remove the three `ore_diamond*.json` placed-feature overrides in `kubejs/data/minecraft/worldgen/placed_feature`, then reload/restart before generating fresh chunks.
