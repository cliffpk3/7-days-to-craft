ServerEvents.recipes(event => {
  event.remove({ id: 'cgm:workbench' })

  event.shaped('cgm:workbench', [
    'SSS',
    'III',
    'I I'
  ], {
    S: '#minecraft:stone_tool_materials',
    I: '#forge:ingots/iron'
  }).id('seven_days_to_craft:cgm_workbench')
})
