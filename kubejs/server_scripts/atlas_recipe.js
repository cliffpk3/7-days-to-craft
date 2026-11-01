// A blank Atlas is the first milestone; the discovered local map is added
// separately with Map Atlases' native two-item crafting recipe.
ServerEvents.recipes(event => {
  event.remove({ id: 'map_atlases:craft_atlas' })
  event.remove({ id: 'map_atlases:add_atlas' })
  event.shapeless(Item.of('map_atlases:atlas', '{locked:1b,empty:0,7dtcAtlasBlank:1b}'), [
    'minecraft:book',
    'minecraft:slime_ball',
    'minecraft:paper',
    'minecraft:paper',
    'minecraft:paper',
    'minecraft:paper'
  ]).id('seven_days_to_craft:blank_atlas')
})
