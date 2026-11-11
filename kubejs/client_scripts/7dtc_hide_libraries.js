// 7DTC (7/11/2026): hide unobtainable CGM/TaCZ items in JEI; keep the Central Workbench and the Core statue.
JEIEvents.hideItems(event => {
  const keep = ['cgm:workbench', 'tacz:statue']
  Ingredient.of('@cgm').itemIds.forEach(id => { if (!keep.includes(String(id))) event.hide(id) })
  Ingredient.of('@tacz').itemIds.forEach(id => { if (!keep.includes(String(id))) event.hide(id) })
})

// Zombie Extreme items with no active use in 7DTC (no recipe, loot, drop, catalog or approval), generated
// 7/11/2026 by tools/7dtc_ze_unused_20261107.py: spawn eggs, unused materials, disabled ores, world-only furniture.
// Also the retired ZE medical items (SI replaces them). Furniture keeps generating in ZE structures; this only cleans the JEI catalog. Remove an id here when it gains a use.
const ZE_UNUSED = [
  'assasin_black_ops_spawn_egg', 'bandage', 'barbed_fence', 'bear_trap', 'birdsnest', 'black_ops_cargo',
  'black_ops_magnetic_card', 'boomer_spawn_egg', 'burning_land_oil_shale', 'camera', 'cash_register', 'chainsaw_spawn_egg',
  'clicker_spawn_egg', 'cloth_bandage', 'computer_monitor', 'concrete_fence', 'crawler_spawn_egg', 'cupboard',
  'dead_body', 'decomposing_backpack', 'deepslate_aluminum', 'deepslate_lead', 'deepslate_ruby', 'deepslate_steel',
  'deepslate_thorium', 'deepslate_tin', 'deepslate_titan', 'demolisher_spawn_egg', 'devestated_spawn_egg', 'divided_spawn_egg',
  'drinks_vending_machine', 'empty_cans', 'experimental_serum', 'explosive_infected_spawn_egg', 'faceless_spawn_egg', 'fetus_spawn_egg',
  'fittings', 'fridge', 'fulguria_fuel_rod', 'goon_spawn_egg', 'ignot_infected_steel', 'ignot_infected_titan',
  'ignot_thorium', 'ignot_titan', 'infected_flesh', 'infected_hazmat_spawn_egg', 'infected_juggernaut_spawn_egg', 'infected_military_spawn_egg',
  'infected_police_spawn_egg', 'infected_spawn_egg', 'inflated_spawn_egg', 'iron_table', 'kitchen_stove', 'lamp',
  'magnetic_card', 'medicine_box', 'metal_fence', 'microwave', 'military_cargo', 'military_spawn_egg',
  'night_hunter_spawn_egg', 'night_stand', 'oil_shale', 'oil_shale_sand', 'ore_aluminum', 'ore_lead',
  'ore_ruby', 'ore_steel', 'ore_tin', 'ore_titan', 'parasite_spawn_egg', 'personnel_mine',
  'pieceof_wire_fence', 'pipe', 'plastic_fence', 'radiation_block', 'radio_installation', 'ram_spawn_egg',
  'rat_king_spawn_egg', 'raw_plastic', 'raw_thorium', 'raw_tin', 'revived_spawn_egg', 'rubber',
  'runner_spawn_egg', 'sand_bag', 'saw_blade', 'shelf', 'spit_inflated', 'spotlight',
  'steel_blade', 'syringe_adrenaline', 'syringe_antidote', 'syringe_drug', 'syringe_night_vision', 'syringe_stimulator',
  'thorium_ore', 'thorium_rod', 'tool_box', 'trader_table', 'trash_bucket', 'tube_tv',
  'vending_machine', 'washing_machine', 'wire', 'wooden_barricades', 'wooden_chair', 'zero_patient_spawn_egg'
]
JEIEvents.hideItems(event => ZE_UNUSED.forEach(id => event.hide('zombie_extreme:' + id)))

// Scorched Repair Kit retired (7/11/2026): the Copper/Iron/Golden repair kits cover every damageable item, guns included.
JEIEvents.hideItems(event => event.hide('scguns:repair_kit'))
