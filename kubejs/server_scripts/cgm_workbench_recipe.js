// Central Workbench (cgm:workbench): 16/11/2026 (user) no longer craftable; the Central Forge replaces it.
// The Forge recipe (the old Central Workbench recipe plus a furnace) is data in seven-days-workbenches:
// data/seven_days_workbenches/recipes/7dtc_central_forge.json. Placed Central Workbenches keep working.
ServerEvents.recipes(event => {
  event.remove({ id: 'cgm:workbench' })
})
