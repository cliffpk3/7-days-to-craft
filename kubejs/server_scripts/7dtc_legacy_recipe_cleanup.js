// 7DTC (7/11/2026): CGM and TaCZ stay installed only as libraries.
// cgm:workbench is the Central Workbench (its crafting recipe: cgm_workbench_recipe.js);
// the Core statue is re-added by personal_core_recipe.js, which runs after this file.
ServerEvents.recipes(event => {
  event.remove({ type: 'cgm:workbench' })      // legacy CGM screen: guns, ammo, migrated items
  event.remove({ mod: 'cgm' })                 // native CGM recipes (bullets, attachments)
  event.remove({ mod: 'tacz' })                // TaCZ guns, ammo, attachments, smith tables
  // Mod-wide removals formerly done by the retired legacy scripts:
  event.remove({ mod: 'fracturepoint' })       // cgm_workbench_catalog.js
  event.remove({ mod: 'divine_weaponry' })     // divine_weaponry_workbench.js
  event.remove({ mod: 'iron_repair_kits' })    // repair_kits_workbench.js
})
