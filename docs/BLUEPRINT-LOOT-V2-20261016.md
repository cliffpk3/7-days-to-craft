# Blueprint and Divine loot v2 — 2026-10-16

## Firearms

- One blueprint roll per player kill, then one random gun blueprint from that threat tier.
- Tier 1/2/3/4 roll: 0.25% / 0.5% / 1% / 1.5% (previously 0.5% / 1% / 2% / 3%).
- Uses rolled when dropped: 1 use 70%, 2 uses 20%, 3 uses 10%.
- Existing blueprints without the use tag are valid for one craft.

## Divine Weaponry

- CGM Workbench recipes exist only for iron, diamond, and netherite weapons: 12 weapon types per material, each with its own blueprint. Native Divine recipes remain disabled. Stone weapons remain loot only. Wood and gold have neither a blueprint nor a Workbench recipe.
- The blueprint drop is a single roll per player kill, not a separate roll per weapon. Tier 1: iron 0.15%. Tier 2: iron 0.5%. Tier 3: iron 0.8%, diamond 0.15%. Tier 4: iron 0.8%, diamond 0.35%, netherite 0.08%.
- These Divine blueprint chances are read from `config/7dtc_divine_blueprints.json` at server start. The loader rejects missing tiers, negative values, nonfinite values, and per-tier totals over 100%.
- Uses rolled when dropped: 1 use 55%, 3 uses 30%, 6 uses 15%.
- Recipes preserve the material counts in Divine Weaponry's original iron and diamond crafting recipes. Netherite consumes the corresponding diamond Divine weapon and one netherite ingot.
- The KubeJS startup script multiplies native max durability by 1.5 for the material weapon families and the named legendary weapons.
- Ordinary direct drops were reduced roughly 25%: main tier material 8% to 6%, secondary material 1% to 0.75%, tier 4 iron 5% to 4%. Legendary chance for tier 4/bosses is 4% (was 5%). Infernal legendary chance is 0.4% per modifier, capped at 3% (was 0.5% per modifier, capped at 4%).
- Infernal elites have a 1.5x multiplier to the nonlegendary Divine weapon chances. Divine weapons dropped by elites receive an enchantment roll whose strength scales with threat tier and Infernal modifier count.
- If the enchantment roll produces no enchantment on a damageable Divine weapon, an Unbreaking enchantment is applied so elite finds are visibly enchanted.
- Iron Golem's guaranteed random iron Divine weapon remains unchanged.

## Keys

- Infernal elites currently have a 10% roll for a level 3 key, or 18% for a player with Key Hunter. These rates were preserved.

## Runtime and rollback

- The CGM extension v1.4.9 intercepts blueprint ingredient consumption on the server and subtracts one NBT use per craft. Both the extension and Seven Days Locks JAR require a game restart.
- Previous installed JARs are retained under ignored `local/blueprints-20261016/`. Restore both JARs together when rolling back, then restore the previous recipes and configuration from Git.
