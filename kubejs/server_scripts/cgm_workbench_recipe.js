// Central Workbench (cgm:workbench block): wood and stone only (7/11/2026, item 13).
// The first one a player obtains also grants the four 7DTC workbenches (advancement 7dtc:onboarding/central_workbench).
ServerEvents.recipes(event => {
  event.remove({ id: 'cgm:workbench' })

  event.shaped('cgm:workbench', [
    'PPP',
    'CTC',
    'C C'
  ], {
    P: '#minecraft:planks',
    C: '#forge:cobblestone',
    T: 'minecraft:crafting_table'
  }).id('seven_days_to_craft:cgm_workbench')
})
