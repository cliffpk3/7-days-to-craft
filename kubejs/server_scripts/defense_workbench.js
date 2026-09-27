ServerEvents.recipes(event => {
  const material = (item, count) => ({ item: item, count: count })
  const add = (id, result, materials) => event.custom({
    type: 'cgm:workbench',
    materials: materials,
    result: { item: result }
  }).id(`seven_days_to_craft:cgm/defense/${id}`)

  add('basic_turret', 'seven_days_defense:basic_turret', [
    material('minecraft:dispenser', 1), material('minecraft:iron_ingot', 24),
    material('minecraft:redstone', 16), material('minecraft:copper_ingot', 12)
  ])
  add('advanced_turret', 'seven_days_defense:advanced_turret', [
    material('seven_days_defense:basic_turret', 1), material('minecraft:observer', 2),
    material('minecraft:diamond', 4), material('minecraft:redstone_block', 2)
  ])
  add('shotgun_turret', 'seven_days_defense:shotgun_turret', [
    material('seven_days_defense:basic_turret', 1), material('minecraft:iron_ingot', 20),
    material('minecraft:tripwire_hook', 4), material('minecraft:redstone', 12)
  ])
  add('explosive_turret', 'seven_days_defense:explosive_turret', [
    material('seven_days_defense:advanced_turret', 1), material('minecraft:obsidian', 8),
    material('cgm:grenade', 4), material('minecraft:diamond', 6)
  ])

  add('basic_turret_ammo_box', 'seven_days_defense:basic_turret_ammo_box', [material('cgm:basic_bullet', 64)])
  add('advanced_turret_ammo_box', 'seven_days_defense:advanced_turret_ammo_box', [material('cgm:advanced_bullet', 64)])
  add('turret_shell_box', 'seven_days_defense:turret_shell_box', [material('cgm:shell', 16)])
  add('grenade_canister', 'seven_days_defense:grenade_canister', [material('cgm:grenade', 4)])
})
