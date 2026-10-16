# Blueprint and loot audit — 2026-10-16

## Verified relationships

- All 36 custom CGM Divine recipes use a one-item blueprint ingredient and output a corresponding registered Divine item. No wood, stone, or gold weapon is in those recipes.
- The 12 gun recipes also use one corresponding blueprint: nine CGM guns in `gun_blueprints.js` and three Alex's Mobs guns in `alexsmobs_integration.js`.
- The server CGM mixin intercepts `InventoryUtil.removeWorkstationIngredient` for `seven_days_locks:blueprint_*` and decrements one NBT use. Existing untagged plans consume normally in one craft.
- The generated Divine recipe inputs match Divine Weaponry v1.7's 36 native ingredient lists. Netherite recipes consume a matching diamond Divine weapon and a netherite ingot.
- The Create blueprint model is available because Create 6.0.8 is installed.
- The KubeJS `ItemEvents.modification` durability pattern is already used in the reference modpack. Here it changes max durability on both client and server at startup.
- Mob keys, direct Divine weapons, firearms, armor, and native Infernal loot are independent rolls. Elite Infernal key chance remains 10% for a level 3 key (18% with Key Hunter).
- Both blueprint systems and direct Divine drops require a player kill. Turrets, environmental damage, and other non-player final blows do not award them, which further lowers the practical supply during hordes.

## Balance findings

- A specific blueprint is only 1/12 of its material's chance. The tier 4 netherite chance is now 10% for any netherite Divine blueprint: one per 10 eligible kills on average. One specific weapon blueprint averages one per 120 tier 4 kills. These are statistical expectations, not guarantees.
- Iron and diamond plans are also much rarer than direct weapon finds. For a tier 3 ordinary mob, the iron weapon chance is about 6%, versus 0.8% for any iron plan; each plan averages 2.35 crafts. Plans therefore act as a long-term replacement source instead of the main way to acquire a weapon.
- Found standard weapons have 35–80% durability remaining. Multiplying max durability by 1.5 gives 52.5–120% of the former full durability on a found weapon. A crafted weapon starts at 150% of the former full durability.
- Native Infernal loot remains active. Elites may also award a key, equipment, a Divine weapon, and a blueprint on the same kill. This can make a rare kill feel rewarding, but the resulting loot bursts should be observed in co-op play.
- CGM's generic Workbench recipe creates a fresh netherite weapon even when it consumes a damaged diamond Divine weapon. This is a free repair path. Preserve input damage in a dedicated upgrade handler, or replace the diamond weapon ingredient with raw materials before finalizing the netherite progression.

## Suggested playtest checkpoints

1. Craft twice from a two-use gun plan and confirm its counter goes 2 → 1 → consumed; repeat for a three-use Divine plan.
2. Confirm that a damaged diamond Divine weapon does not create an unintended full-durability netherite upgrade before accepting that recipe economy.
3. Record at least 100 elite kills by threat tier, including native Infernal items, keys, Divine weapons, and plans. Small samples cannot validate the netherite blueprint rate.
4. Playtest the new 10% tier 4 netherite plan rate before increasing it further; each of the 12 plans still has only a 0.833% chance per eligible kill.
