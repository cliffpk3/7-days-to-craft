// INACTIVE proposal. Only copy into kubejs/server_scripts when city raids are approved.
// Removes all Zombie Extreme recipes, including smelting, crafting and special recipes.
ServerEvents.recipes(event => {
  event.remove({ mod: 'zombie_extreme' })
})
