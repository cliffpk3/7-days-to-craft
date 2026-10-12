// Divine Weaponry is loot-only. Native crafting, smithing and CGM Workbench
// recipes are removed so threat level controls access to the weapon tiers.
ServerEvents.recipes(event => {
  event.remove({ mod: 'divine_weaponry' })
})
