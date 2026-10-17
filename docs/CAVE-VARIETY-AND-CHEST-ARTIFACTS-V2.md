# Cave variety and chest artifacts V2 (16/10/2026)

## Cave predators and encounters

- The Alex's Mobs centipede head has a native follow range of 32 blocks. Its player/villager target goal uses this attribute. 7DTC sets it to 8 blocks on entity join and in the central mob registry, covering newly spawned and loaded centipedes. Attacking one can still provoke retaliation.
- Cave ecosystem families grow from five to seven. The two new families are `bone_cult` (bonescaller, skeleton demoman, decrepit skeleton, and a rare skeleton thrasher pairing) and `haunted_depths` (spirit guide/restless spirit, seared spirit/spirit of chaos, and a rare nightmare stalker).
- Existing hound and restless-dead families receive three more encounter combinations: hound/stalker, decaying zombie/barrel zombie, and barrel zombie/zombie bruiser. The per-player encounter chance, local caps, 32-block spawn-safe radius and sector cooldown remain unchanged.
- All new encounters use mobs already registered in `7dtc_mob_registry.json`. Their existing threat tiers, Divine Weaponry/key/blueprint drop handling, and Advanced Compass exclusions remain in effect. Born in Chaos native spawns and the InControl population still coexist with this additional layer.

## Keyed chest artifacts

| Chest level | Before | V2: chance of one artifact | Rank when awarded |
| --- | ---: | ---: | --- |
| 1 | 15% | 15% | 1 |
| 2 | 25% | 25% | 2 |
| 3 | 40% | 100% | 20% rank 2, 80% rank 3 |
| 4 | 100% | 100% | 25% rank 3, 75% rank 4 |

The rank roll is made once when a key first opens a chest. Subsequent openings never reroll or require another key. Special WRD completion chests, mimic caches, and the onboarding cache keep their guaranteed dedicated rewards. A misspelled rank 1 item ID (`novely_drinking_hat`) is corrected to `novelty_drinking_hat`.

The previous JAR and both earlier configurations are backed up in `backups/cave-variety-artifacts-v2-20261016` as `*.before.*`. To roll back the behavior, restore those three files and restart. The new JAR and cave configuration are loaded on startup. Existing chests already opened do not receive a new artifact.
