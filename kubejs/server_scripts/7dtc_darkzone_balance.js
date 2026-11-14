// 13/11/2026 Dark Zone balance (tools/7dtc_darkzone_balance_20261113.py; docs/7DTC-ZONA-ESCURA-BALANCEAMENTO.md).
// Diamond steel only from the Utility Workbench L2 recipe (diamond + Dark Zone phosphor). The Scorched blend routes gave
// 8 ingots per diamond. The diamond steel armor is made at the Armor Workbench L3 now.
ServerEvents.recipes(event => {
  ;[
    'scguns:create/diamond_steel_blend_from_mixing',
    'scguns:create/diamond_steel_ingot_from_mixing',
    'scguns:macerator/diamond_steel_blend_from_macerating',
    'scguns:powered_macerator/powered_diamond_steel_blend_from_macerating'
  ].forEach(id => event.remove({ id: id }))
  ;['helmet', 'chestplate', 'leggings', 'boots'].forEach(piece => event.remove({ output: 'scguns:diamond_steel_' + piece }))
})

// 13/11/2026 steel leaks (user test): Create recycling of Scorched guns gave 2 diamond steel each (the gun frame needs no
// phosphor), depleted diamond steel turned back into steel, and the blend smelted into steel. All closed.
ServerEvents.recipes(event => {
  ;[
  "scguns:create/recycling/carapice_recycling",
  "scguns:create/recycling/cyclone_recycling",
  "scguns:create/recycling/dark_matter_recycling",
  "scguns:create/recycling/drill_conversion_recycling",
  "scguns:create/recycling/drill_recycling",
  "scguns:create/recycling/echoes_2_recycling",
  "scguns:create/recycling/inertial_recycling",
  "scguns:create/recycling/inquisitor_recycling",
  "scguns:create/recycling/krauser_recycling",
  "scguns:create/recycling/lockewood_recycling",
  "scguns:create/recycling/mas_55_recycling",
  "scguns:create/recycling/mas_peddler_recycling",
  "scguns:create/recycling/micina_recycling",
  "scguns:create/recycling/minksy_recycling",
  "scguns:create/recycling/nailer_recycling",
  "scguns:create/recycling/plasgun_recycling",
  "scguns:create/recycling/prush_gun_recycling",
  "scguns:create/recycling/rg_jigsaw_recycling",
  "scguns:create/recycling/sculk_resonator_recycling",
  "scguns:create/recycling/shard_culler_recycling",
  "scguns:create/recycling/shellurker_recycling",
  "scguns:create/recycling/soul_drummer_recycling",
  "scguns:create/recycling/uppercut_recycling",
  "scguns:create/recycling/valora_recycling",
  "scguns:create/recycling/whispers_recycling",
  "scguns:create/recycling/zilk_45_recycling",
  "scguns:create/deploying/depleted_diamond_steel",
  "scguns:create/diamond_steel_filling",
  "scguns:mech_press/depleted_diamond_steel",
  "scguns:powered_mech_press/powered_depleted_diamond_steel",
  "scguns:diamond_steel_ingot_from_blasting_diamond_steel_blend",
  "scguns:diamond_steel_ingot_from_smelting_diamond_steel_blend"
].forEach(id => event.remove({ id: id }))
})
