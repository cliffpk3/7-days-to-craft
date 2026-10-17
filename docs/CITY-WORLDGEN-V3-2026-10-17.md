# City Worldgen v3 — cohesive cities and vanilla villages

## Change

- Disabled Doomsday Structures 1.0.0. Doomsday Decoration and Translations remain available because existing city templates or worlds may use their decorative blocks.
- Disabled Better Villages 3.2.0 and Towns and Towers 1.12. Removed the KubeJS structure templates copied from those mods so future Minecraft villages use vanilla layouts. Existing generated villages remain in saves.
- Kept Berezka The Lost City enabled. Its `7dtc_city` profile now generates the city itself without separate skyscrapers, factories, camps, lighthouses, towers, posts, rails, roads, or trains. The standalone `camp` structure is disabled by a datapack override.
- Restricted the 7DTC city raid structure tag to `the_lost_city:city`.

## Evidence and verification

- In `Mystical Vault of Swords`, two `the_lost_city:city` starts were registered near (928, 704) and (1104, 688), but the log reports collisions with a Lost City train and a Doomsday fire station, followed by removal of both city starts. Their stored first pieces were at Y 593 and Y 570, outside the normal world height.
- In `Creepy River`, 10,601 inspected chunks contained no `the_lost_city:city` starts; the latest client process had loaded the previous profile before the v2 change was written. That world did contain many scattered posts, trains, Doomsday structures, and other independent Lost City structures.
- Static checks: the three disabled JARs have no other mod declaring a required dependency on them. Their worldgen data references and village structure overrides were removed from KubeJS. The generation profile and structure tag parse as JSON.
- Runtime confirmation still requires a fresh Minecraft start and generation of new chunks. A newly created test world is the clearest check. Use `/locate structure the_lost_city:city`, then inspect the site; a locate result by itself does not prove the full city rendered.

Rollback: Git tag `city-worldgen-v3-pre-disable`.
