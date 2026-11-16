// 7DTC (11/11/2026): Zombie Extreme items are retired; only the materials used in recipes stay in JEI.
JEIEvents.hideItems(event => {
  // 15/11/2026: only the ones still found in loot stay visible (they salvage into Metal Scrap).
  // 16/11/2026: the loot cleanup removed the last Zombie Extreme metals; none stays visible.
  const keep = [].map(id => 'zombie_extreme:' + id)
  Ingredient.of('@zombie_extreme').itemIds.forEach(id => { if (!keep.includes(String(id))) event.hide(id) })
})
