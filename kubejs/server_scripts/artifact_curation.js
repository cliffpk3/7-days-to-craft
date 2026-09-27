// Artifact progression is exclusive to the tiered locked-chest system.
ServerEvents.recipes(event => {
  event.remove({ mod: 'artifacts' })
  event.remove({ mod: 'nameless_trinkets' })
  event.remove({ mod: 'celestial_artifacts' })
})