ServerEvents.recipes(event => {
  // Fracture Point's own Ballistics Bench recipes are intentionally disabled.
  // Only items explicitly catalogued below can be assembled in the CGM Workbench.
  event.remove({ mod: 'fracturepoint' })
  event.remove({ output: 'quark:backpack' })
  event.remove({ output: 'grapplemod:grapplinghook' })
  event.remove({ output: 'corail_recycler:recycler' })

  const material = (item, count) => ({ item: item, count: count })
  const tagged = (tag, count) => ({ tag: tag, count: count })
  const add = (category, id, result, materials) => event.custom({
    type: 'cgm:workbench',
    materials: materials,
    result: { item: result }
  }).id(`seven_days_to_craft:cgm/${category}/${id}`)

  // Insurgency Commander
  add('equipment', 'insurgency_commander_helmet', 'fracturepoint:insurgency_commander_helmet', [
    tagged('forge:ingots/iron', 30), material('minecraft:diamond', 2), material('minecraft:diamond_helmet', 1),
    material('minecraft:rabbit_hide', 6), material('minecraft:redstone', 4), material('minecraft:black_dye', 4)
  ])
  add('equipment', 'insurgency_commander_chestplate', 'fracturepoint:insurgency_commander_chestplate', [
    tagged('forge:ingots/iron', 46), material('minecraft:diamond', 4), material('minecraft:diamond_chestplate', 1),
    material('minecraft:leather', 12), material('minecraft:slime_ball', 4), material('minecraft:black_dye', 6)
  ])
  add('equipment', 'insurgency_commander_leggings', 'fracturepoint:insurgency_commander_leggings', [
    tagged('forge:ingots/iron', 36), material('minecraft:diamond', 3), material('minecraft:diamond_leggings', 1),
    material('minecraft:string', 12), material('minecraft:copper_ingot', 8), material('minecraft:black_dye', 4)
  ])
  add('equipment', 'insurgency_commander_boots', 'fracturepoint:insurgency_commander_boots', [
    tagged('forge:ingots/iron', 24), material('minecraft:diamond', 2), material('minecraft:diamond_boots', 1),
    material('minecraft:leather', 10), material('minecraft:slime_ball', 3), material('minecraft:feather', 6)
  ])
  add('equipment', 'fsb_squad_leader_helmet', 'fracturepoint:fsb_squad_leader_helmet', [
    tagged('forge:ingots/iron', 32), material('minecraft:diamond', 2), material('minecraft:diamond_helmet', 1),
    material('minecraft:leather', 6), material('minecraft:redstone', 6), material('minecraft:gray_dye', 4)
  ])
  add('equipment', 'fsb_squad_leader_chestplate', 'fracturepoint:fsb_squad_leader_chestplate', [
    tagged('forge:ingots/iron', 48), material('minecraft:diamond', 4), material('minecraft:diamond_chestplate', 1),
    material('minecraft:leather', 14), material('minecraft:obsidian', 3), material('minecraft:gray_dye', 6)
  ])
  add('equipment', 'fsb_leggings', 'fracturepoint:fsb_leggings', [
    tagged('forge:ingots/iron', 38), material('minecraft:diamond', 3), material('minecraft:diamond_leggings', 1),
    material('minecraft:string', 12), material('minecraft:copper_ingot', 8), material('minecraft:gray_dye', 4)
  ])
  add('equipment', 'fsb_boots', 'fracturepoint:fsb_boots', [
    tagged('forge:ingots/iron', 24), material('minecraft:diamond', 2), material('minecraft:diamond_boots', 1),
    material('minecraft:leather', 10), material('minecraft:slime_ball', 3), material('minecraft:brown_dye', 4)
  ])

  add('equipment', 'beta7_nvg_helmet', 'fracturepoint:beta7_nvg_helmet', [
    tagged('forge:ingots/iron', 30), material('minecraft:diamond', 2), material('minecraft:diamond_helmet', 1),
    material('minecraft:tinted_glass', 4), material('minecraft:amethyst_shard', 6), material('minecraft:comparator', 2)
  ])
  add('equipment', 'beta7_chestplate', 'fracturepoint:beta7_chestplate', [
    tagged('forge:ingots/iron', 50), material('minecraft:diamond', 5), material('minecraft:diamond_chestplate', 1),
    material('minecraft:obsidian', 5), material('minecraft:leather', 12), material('minecraft:echo_shard', 1)
  ])
  add('equipment', 'beta7_leggings', 'fracturepoint:beta7_leggings', [
    tagged('forge:ingots/iron', 40), material('minecraft:diamond', 3), material('minecraft:diamond_leggings', 1),
    material('minecraft:copper_ingot', 10), material('minecraft:string', 14), material('minecraft:amethyst_shard', 4)
  ])
  add('equipment', 'beta7_boots', 'fracturepoint:beta7_boots', [
    tagged('forge:ingots/iron', 26), material('minecraft:diamond', 2), material('minecraft:diamond_boots', 1),
    material('minecraft:leather', 10), material('minecraft:slime_ball', 4), material('minecraft:feather', 8)
  ])

  // A battery without BatteryEnergy NBT starts fully charged (50,000 FE) in Fracture Point.
  add('utilities', 'nvg_battery', 'fracturepoint:nvg_battery', [
    material('minecraft:copper_ingot', 8), material('minecraft:redstone', 12), material('minecraft:coal', 4),
    material('minecraft:amethyst_shard', 2)
  ])
  add('utilities', 'quark_backpack', 'quark:backpack', [
    material('minecraft:leather', 16), material('quark:ravager_hide', 2), material('minecraft:chest', 2),
    material('minecraft:iron_ingot', 12), material('minecraft:string', 8)
  ])
  add('utilities', 'grappling_hook', 'grapplemod:grapplinghook', [
    material('minecraft:iron_ingot', 18), material('minecraft:lead', 3), material('minecraft:crossbow', 1),
    material('minecraft:slime_ball', 4), material('minecraft:redstone', 6), material('minecraft:ender_pearl', 1)
  ])

  add('utilities', 'recycler', 'corail_recycler:recycler', [
    material('minecraft:iron_ingot', 16), material('minecraft:copper_ingot', 12),
    material('minecraft:redstone', 12), material('minecraft:diamond', 2),
    material('minecraft:crafting_table', 1)
  ])

  // Create: essential early and intermediate engineering components.
  // Their regular recipes are removed so progression remains centralized in this catalog.
  const createWorkbenchItems = [
    'create:andesite_alloy', 'create:shaft', 'create:cogwheel', 'create:large_cogwheel',
    'create:gearbox', 'create:clutch', 'create:gearshift', 'create:depot',
    'create:encased_fan', 'create:mechanical_press', 'create:mechanical_mixer',
    'create:water_wheel', 'create:large_water_wheel'
  ]
  createWorkbenchItems.forEach(item => event.remove({ output: item }))

  add('engineering', 'andesite_alloy', 'create:andesite_alloy', [
    material('minecraft:andesite', 6), material('minecraft:iron_nugget', 12), material('minecraft:clay_ball', 2)
  ])
  add('engineering', 'shaft', 'create:shaft', [
    material('create:andesite_alloy', 2), material('minecraft:iron_nugget', 4)
  ])
  add('engineering', 'cogwheel', 'create:cogwheel', [
    material('create:shaft', 1), tagged('minecraft:planks', 6), material('minecraft:iron_nugget', 4)
  ])
  add('engineering', 'large_cogwheel', 'create:large_cogwheel', [
    material('create:cogwheel', 2), tagged('minecraft:planks', 12), material('create:andesite_alloy', 2)
  ])
  add('engineering', 'gearbox', 'create:gearbox', [
    material('create:cogwheel', 4), material('create:andesite_casing', 1), material('create:shaft', 2)
  ])
  add('engineering', 'clutch', 'create:clutch', [
    material('create:shaft', 2), material('create:andesite_casing', 1), material('minecraft:redstone', 6), material('minecraft:iron_ingot', 4)
  ])
  add('engineering', 'gearshift', 'create:gearshift', [
    material('create:cogwheel', 2), material('create:andesite_casing', 1), material('minecraft:redstone', 8), material('minecraft:iron_ingot', 4)
  ])
  add('engineering', 'depot', 'create:depot', [
    material('create:andesite_alloy', 4), material('minecraft:smooth_stone', 6), material('minecraft:iron_ingot', 4)
  ])
  add('engineering', 'encased_fan', 'create:encased_fan', [
    material('create:andesite_casing', 1), material('create:shaft', 2), material('minecraft:iron_ingot', 8), material('minecraft:iron_bars', 4)
  ])
  add('engineering', 'mechanical_press', 'create:mechanical_press', [
    material('create:andesite_casing', 2), material('create:shaft', 2), material('minecraft:iron_block', 2), material('create:cogwheel', 2)
  ])
  add('engineering', 'mechanical_mixer', 'create:mechanical_mixer', [
    material('create:andesite_casing', 2), material('create:cogwheel', 3), material('minecraft:iron_ingot', 12), material('create:whisk', 1)
  ])
  add('engineering', 'water_wheel', 'create:water_wheel', [
    material('create:shaft', 2), tagged('minecraft:planks', 20), material('create:andesite_alloy', 4)
  ])
  add('engineering', 'large_water_wheel', 'create:large_water_wheel', [
    material('create:water_wheel', 2), tagged('minecraft:planks', 32), material('create:andesite_alloy', 8)
  ])})

