// Only the Copper Repair Kit is craftable; every other recipe from the mod stays disabled.
ServerEvents.recipes(event => {
  event.remove({ mod: 'iron_repair_kits' })
  event.custom({
    type: 'cgm:workbench',
    materials: [
      { item: 'minecraft:stick', count: 3 },
      { item: 'minecraft:copper_block', count: 2 },
      { item: 'minecraft:copper_ingot', count: 1 }
    ],
    result: { item: 'iron_repair_kits:copper_repair_kit' }
  }).id('seven_days_to_craft:cgm/utilities/repair_kits/copper_repair_kit')
})
