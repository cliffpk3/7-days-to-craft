ServerEvents.recipes(event => {
  [
    'quark:ancient_chest',
    'quark:acacia_chest',
    'quark:prismarine_chest',
    'quark:nether_brick_chest'
  ].forEach(item => event.remove({ output: item }))
})
