// The visual block is supplied by TaCZ. The Core mod binds the
// first personally crafted copy to the crafter's UUID on the server.
ServerEvents.recipes(event => {
  event.remove({ id: 'tacz:statue' })
  event.shaped('tacz:statue', [
    ' S ',
    'TBT',
    ' B '
  ], {
    S: 'minecraft:cobblestone_slab',
    T: 'minecraft:cobblestone_stairs',
    B: 'minecraft:cobblestone'
  }).id('tacz:statue')
})
