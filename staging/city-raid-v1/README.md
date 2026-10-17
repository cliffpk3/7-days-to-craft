# City raid V1 — inactive preparation

Status: **proposal only**. The live `mods` folder was restored to its pre-study contents. The candidate JARs are held in `backups/city-raid-v1-20261016/installed-jars-hold/` and are not loaded by the game. No file in `config`, `kubejs`, or an active datapack was changed by this proposal. The chosen city generator is **Berezka's The Lost City 1.5.0**, with Doomsday Structures and The Lost City: Refurbished.

## Generator decision

The inspected Big Lost City 1.0.1 JAR has 42 independent jigsaw structures, each with its own `structure_set`, random-spread spacing and separation, and `size: 1`. It does not contain a shared city planner. Its categories are:

- Buildings: three skyscrapers, three ruined skyscrapers, three houses, one store, one warehouse, one power plant.
- Landmarks and sites: ferris wheel, big and small shipping containers, three tents.
- Props: ten cars and fourteen decorative structures.

Those pieces can appear near one another by chance, but the JAR does not guarantee connected streets or a coherent city district. Berezka's The Lost City has connected city structure pools and is the selected base for planned raid areas.

The `obsolete-mcjty/lostcities-profile-7dtc-city-raid.json` file was copied from a McJty *The Lost Cities* profile before the generator choice was clarified. **It is obsolete and must not be applied to Berezka's mod.** The old McJty JAR remains disabled (`.jar_`). Berezka profiles must instead be authored against its generated `config/TheLostCity.toml` and `config/city_profiles/generation_profiles/` and `structure_profiles/` formats after a controlled first launch. Doomsday Structures contributes a fire station, gas station and shops to Berezka city pools; Refurbished supplies furniture templates. Generation must be tested in **new chunks**; existing chunks will not be rebuilt. The exact inactive candidate JARs and SHA-256 hashes are recorded in `candidate-jars.json`.

## Zombie Extreme preparation

`prepare.py` generated `candidate-data` from Zombie Extreme 0.2.6.5 for Forge 1.20.1. It stages:

- `forge:none` overrides for all 42 native biome modifiers: 26 global monster-spawn modifiers and 16 ore/feature modifiers. This prevents global distribution and foreign ore progression when activated.
- Ten native structure definitions redirected to an empty biome tag. Their buildings will not generate in normal biomes when the staged datapack is activated.
- `recipes.js` to remove recipes from the `zombie_extreme` namespace. The audit records all 74 recipe IDs found in the JAR.

These are **not sufficient** for the desired experience. Zombie Extreme has no entity loot tables in its JAR; its entity drops require runtime inspection of mod procedures or field tests before any promise about native items. A city-only spawn controller must identify Berezka city structures and their boundaries, block other hostile natural spawns inside the raid area, and spawn Zombie Extreme groups there with a local cap, cooldown, distance from players, and tier weighting. The controller must also prevent Zombie Extreme mobs from appearing outside cities through spawners, commands excluded, and any non-biome-modifier paths. Existing hordes must remain unaffected.

## Proposed difficulty and rewards to validate

- Common patrols: infected, crawler, runner, clicker, revived and infected police. Dense enough to be seen from a distance, without spawning beside a player.
- Specialist groups: boomer, explosive infected, military, hazmat, faceless, divided, inflated and related variants. Mix with common patrols, with a lower local cap.
- Rare anchors: juggernaut, demolisher, chainsaw, night hunter, rat king, zero patient and black-ops assassin. One major threat per local encounter; validate their actual damage and AI before final weighting.
- Convert Berezka city chests to the existing locked-chest tiers, with the requested 30% chance of being open. Put the richest rewards in protected interiors or upper floors and prevent direct unrestricted access to tier 3–4 loot. Keys, Divine Weaponry, blueprints and guaranteed tier 3–4 artifact behavior must reuse the current 7DTC policy. Do not stack native Zombie Extreme gear on top of these rewards.

The registry and drop adapters have deliberately **not** been added to active files. Exact city detection, local caps, native drop removal, native spawner removal, loot table mapping, and runtime compatibility remain prerequisites to activation.

## Activation gate

1. Generate and review Berezka's native config and profiles in an isolated test instance; choose rarity and confirm cities belong in the overworld.
2. Implement and test city boundary detection against Berezka The Lost City 1.5.0, including buildings, streets and players leaving a city.
3. Implement exclusive city spawn routing and test non-city, city, horde and multiplayer cases.
4. Inspect every Zombie Extreme mob drop path and map reward tiers to 7DTC; verify crafting and JEI exclusions.
5. Map Berezka city loot tables and generated containers to locked chests; verify every tier opens once and awards the intended loot, with 30% open.
6. Test a new throwaway world, logs and generation before copying any staged files into active paths.
