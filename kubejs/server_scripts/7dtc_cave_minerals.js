// 7DTC 11/11/2026 (cavernas, lote B-dados): fundicao dos minerios raros do subsolo que ganharam uso.
ServerEvents.recipes(event => {
  ;[['titanium', 'zombie_extreme:raw_titan', 'zombie_extreme:ignot_titan']].forEach(([name, raw, ingot]) => {
    event.smelting(ingot, raw).xp(1.0).cookingTime(200).id(`seven_days_to_craft:smelting/7dtc_${name}_ingot`)
    event.blasting(ingot, raw).xp(1.0).cookingTime(100).id(`seven_days_to_craft:blasting/7dtc_${name}_ingot`)
  })
})
