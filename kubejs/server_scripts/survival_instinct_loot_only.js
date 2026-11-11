// Survival Instinct supplies roadside structures and salvage, not native crafting.
// Keep any future 7DTC recipes under a separate namespace so they can be curated.
ServerEvents.recipes(event => {
  event.remove({ mod: 'survival_instinct' })
})
