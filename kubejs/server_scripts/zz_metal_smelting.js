// 7DTC 7/11/2026: city_raid_recipes.js removes every Zombie Extreme recipe, including smelting.
// Restore only the three zombie-drop metals used by Ordnance ammo and trap repair.
ServerEvents.recipes(event => {
  const metals = [
    ['lead', 'zombie_extreme:raw_lead', 'zombie_extreme:ignotlead'],
    // 12/11/2026: steel unified as Steellium (Utility level 2, 3 raw steel); no ZE steel ingot.
    ['aluminum', 'zombie_extreme:raw_aluminum', 'zombie_extreme:ignot_aluminum']
  ]
  metals.forEach(([name, raw, ingot]) => {
    event.smelting(ingot, raw).xp(0.7).cookingTime(200).id(`seven_days_to_craft:smelting/${name}_ingot`)
    event.blasting(ingot, raw).xp(0.7).cookingTime(100).id(`seven_days_to_craft:blasting/${name}_ingot`)
  })
})
