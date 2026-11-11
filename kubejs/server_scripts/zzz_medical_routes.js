// Medical batch: retired ZE treatments have SI replacements at Utility.
ServerEvents.recipes(event => {
  event.remove({ output: 'zombie_extreme:cloth_bandage' })
  event.remove({ output: 'zombie_extreme:bandage' })
  event.remove({ output: 'zombie_extreme:syringe_antidote' })
  event.remove({ output: 'zombie_extreme:syringe_adrenaline' })
  event.remove({ output: 'zombie_extreme:syringe_stimulator' })
  event.remove({ output: 'zombie_extreme:syringe_drug' })
  event.remove({ output: 'zombie_extreme:syringe_night_vision' })
})
