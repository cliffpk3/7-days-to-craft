// Removes all Zombie Extreme recipes, including smelting, crafting and special recipes.
ServerEvents.recipes(event => {
  event.remove({ mod: 'zombie_extreme' })
})
