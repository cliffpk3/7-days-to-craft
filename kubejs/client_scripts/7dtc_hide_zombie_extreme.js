// 7DTC (11/11/2026): Zombie Extreme items are retired; only the materials used in recipes stay in JEI.
JEIEvents.hideItems(event => {
  const keep = ['raw_lead', 'raw_steel', 'raw_aluminum', 'ignotlead', 'ignotsteel', 'ignot_aluminum', 'titan_plate'].map(id => 'zombie_extreme:' + id)
  Ingredient.of('@zombie_extreme').itemIds.forEach(id => { if (!keep.includes(String(id))) event.hide(id) })
})
