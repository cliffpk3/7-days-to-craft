// Steel tools are scavenging finds in roadside buildings, not crafting outputs.
ServerEvents.recipes(event => {
  ;[
    'zombie_extreme:steel_axe',
    'zombie_extreme:steel_hoe',
    'zombie_extreme:steel_pickaxe',
    'zombie_extreme:steel_shovel',
    'zombie_extreme:steel_sledgehammer'
  ].forEach(item => event.remove({ output: item }))
})
